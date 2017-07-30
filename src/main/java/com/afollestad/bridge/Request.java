package com.afollestad.bridge;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Request implements Serializable {

  private final RequestBuilder builder;
  boolean cancelCallbackFired;
  private boolean isCancelled;
  private Response response;

  protected Request(RequestBuilder builder) {
    this.builder = builder;
  }

  RequestBuilder builder() {
    return builder;
  }

  private String valueToString(@Nullable Object value) {
    if (value == null) return null;
    else if (value instanceof String) return (String) value;
    else if (value instanceof Character) return Character.toString((Character) value);
    else if (value instanceof Short) return Short.toString((Short) value);
    else if (value instanceof Integer) return Integer.toString((Integer) value);
    else if (value instanceof Long) return Long.toString((Long) value);
    else if (value instanceof Boolean) return Boolean.toString((Boolean) value);
    else if (value instanceof Double) return Double.toString((Double) value);
    else if (value instanceof Float) return Float.toString((Float) value);
    else if (value instanceof Byte) return Byte.toString((Byte) value);
    else if (value instanceof byte[]) return new String((byte[]) value);
    else return value.toString();
  }

  Request makeRequest() throws BridgeException {
    try {
      return performRequest();
    } catch (BridgeException e) {
      final Response resp = e.response();
      if (builder.totalRetryCount > 0 && builder.currentRetryCount < builder.totalRetryCount) {
        // Retry count exists and we haven't reached the max yet
        if (builder.retryCallback == null
            || builder.retryCallback.onWillRetry(resp, e, builder())) {
          // We are allowed to retry again
          if (builder.retrySpacingMs > 0) {
            // Wait for a user-set interval before retrying
            try {
              Thread.sleep(builder.retrySpacingMs);
            } catch (InterruptedException ignored) {
            }
          }
          builder.currentRetryCount++;
          return makeRequest();
        } else if (resp != null) {
          throw new BridgeException(
              resp, "Max retry count reached!", BridgeException.REASON_REQUEST_MAX_RETRIES);
        } else {
          throw new BridgeException(
              e.request(), "Max retry count reached!", BridgeException.REASON_REQUEST_MAX_RETRIES);
        }
      }
      throw e;
    }
  }

  Request performRequest() throws BridgeException {
    try {
      URL url = new URL(builder.url);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      int responseCode = -1;
      String responseMessage = "";
      HashMap<String, List<String>> responseHeaders = new HashMap<>();

      try {
        conn.setReadTimeout(builder.readTimeout);
        conn.setConnectTimeout(builder.connectTimeout);
        final String method = Method.name(builder.method);
        conn.setRequestMethod(method);
        conn.setInstanceFollowRedirects(false);

        if (builder.headers != null && builder.headers.size() > 0) {
          for (final String key : builder.headers.keySet()) {
            final String value = valueToString(builder.headers.get(key));
            conn.setRequestProperty(key, value);
          }
        }
        if (builder.pipe != null) {
          conn.setRequestProperty("Content-Length", builder.pipe.contentLength() + "");
        } else if (builder.body != null) {
          conn.setRequestProperty("Content-Length", builder.body.length + "");
          // Disable internal buffering to make upload progress work properly
          conn.setFixedLengthStreamingMode(builder.body.length);
        }
        conn.setDoInput(true);

        checkCancelled();
        if (builder.pipe != null || builder.body != null) {
          if (builder.uploadProgress != null) {
            builder.uploadProgress.request = this;
          }
          conn.setDoOutput(true);
          conn.connect();

          OutputStream os = null;
          try {
            os = conn.getOutputStream();
            if (builder.pipe != null) {
              builder.pipe.writeTo(os, builder.uploadProgress);
            } else {
              writeTo(builder.body, os, builder.uploadProgress);
              if (builder.uploadProgress != null) {
                builder.uploadProgress.publishProgress(builder.body.length, builder.body.length);
              }
            }
            os.flush();
          } finally {
            if (builder.pipe != null) {
              builder.pipe.close();
            }
            BridgeUtil.closeQuietly(os);
          }
        } else {
          conn.connect();
        }

        checkCancelled();
        byte[] data = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;

        responseCode = conn.getResponseCode();
        responseMessage = conn.getResponseMessage();
        responseHeaders = new HashMap<>(conn.getHeaderFields());
        LogCompat.d(
            Request.this,
            "%s %s status: %s %s",
            Method.name(method()),
            url(),
            responseCode,
            responseMessage);

        try {
          is = conn.getInputStream();
          byte[] buf = new byte[Bridge.config().bufferSize];
          int read;
          int totalRead = 0;
          int totalAvailable;
          if (conn.getHeaderField("Content-Length") != null) {
            String clStr = conn.getHeaderField("Content-Length");
            if (clStr == null) {
              clStr = conn.getHeaderField("content-length");
            }
            totalAvailable = Integer.parseInt(clStr);
          } else {
            totalAvailable = is.available();
          }

          bos = new ByteArrayOutputStream();
          if (totalAvailable != 0) {
            builder.context.fireProgress(Request.this, 0, totalAvailable);
          }
          while ((read = is.read(buf)) != -1) {
            checkCancelled();
            bos.write(buf, 0, read);
            totalRead += read;
            if (totalAvailable != 0) {
              builder.context.fireProgress(Request.this, totalRead, totalAvailable);
            }
          }
          if (totalAvailable == 0) {
            builder.context.fireProgress(Request.this, 100, 100);
          }
          data = bos.toByteArray();
          LogCompat.d(
              Request.this,
              "Read %d bytes from the %s %s response.",
              data != null ? data.length : 0,
              Method.name(method()),
              url());
        } finally {
          BridgeUtil.closeQuietly(is);
          BridgeUtil.closeQuietly(bos);
        }

        checkCancelled();
        response =
            new Response(
                data,
                url(),
                responseCode,
                responseMessage,
                responseHeaders,
                builder.didRedirect,
                builder.redirectCount);
        if (builder.throwIfNotSuccess) {
          BridgeUtil.throwIfNotSuccess(response);
        }
        conn.disconnect();

        if (responseCode >= 300 && responseCode <= 303) {
          final List<String> locHeader = responseHeaders.get("Location");
          if (locHeader.size() > 0) {
            if (Bridge.config().autoFollowRedirects) {
              // Follow redirect
              builder.prepareRedirect(locHeader.get(0));
              return makeRequest(); // chain redirected request
            }
          }
        }

        LogCompat.d(
            Request.this, "%s %s request completed successfully.", Method.name(method()), url());
      } catch (Exception fnf) {
        LogCompat.e(
            Request.this,
            "Processing exception... %s, %s",
            fnf.getClass().getName(),
            fnf.getMessage());
        if (fnf instanceof BridgeException) {
          if (((BridgeException) fnf).reason() != BridgeException.REASON_RESPONSE_UNSUCCESSFUL)
            throw fnf; // redirect to outside catch
        } else if (!(fnf instanceof FileNotFoundException)) throw new BridgeException(this, fnf);
        InputStream es = null;
        try {
          es = conn.getErrorStream();
          response =
              new Response(
                  BridgeUtil.readEntireStream(es),
                  url(),
                  responseCode,
                  responseMessage,
                  responseHeaders,
                  builder.didRedirect,
                  builder.redirectCount);
        } catch (Throwable e3) {
          LogCompat.e(Request.this, "Unable to get error stream... %s", e3.getMessage());
          response =
              new Response(
                  null,
                  url(),
                  responseCode,
                  responseMessage,
                  responseHeaders,
                  builder.didRedirect,
                  builder.redirectCount);
        } finally {
          BridgeUtil.closeQuietly(es);
          conn.disconnect();
        }
      }
    } catch (Exception e) {
      if (e instanceof BridgeException) {
        ((BridgeException) e).request = this;
        throw (BridgeException) e;
      }
      throw new BridgeException(this, e);
    }
    if (builder.validators != null) {
      for (ResponseValidator val : builder.validators) {
        try {
          if (!val.validate(response)) {
            throw new BridgeException(response, val);
          }
        } catch (Exception e) {
          if (e instanceof BridgeException) {
            throw (BridgeException) e;
          }
          throw new BridgeException(response, val, e);
        }
      }
    }
    if (builder.throwIfNotSuccess) {
      BridgeUtil.throwIfNotSuccess(response);
    }
    return this;
  }

  private void writeTo(byte[] bytes, OutputStream os, ProgressCallback progressCallback)
      throws IOException {
    byte[] buffer = new byte[Bridge.config().bufferSize];
    InputStream is = new ByteArrayInputStream(bytes);
    int read;
    int totalRead = 0;
    final int available = is.available();
    while ((read = is.read(buffer)) != -1) {
      os.write(buffer, 0, read);
      totalRead += read;
      if (progressCallback != null) {
        progressCallback.publishProgress(totalRead, available);
      }
    }
    // ByteArrayInputStream doesn't have to be closed
  }

  private void checkCancelled() throws BridgeException {
    if (isCancelled) {
      BridgeException ex = new BridgeException(this);
      LogCompat.d(this, ex.getMessage());
      throw ex;
    }
  }

  public boolean isCancellable() {
    return builder().cancellable;
  }

  public void cancel() {
    cancel(false);
  }

  public void cancel(boolean force) {
    if (!force && !isCancellable()) {
      throw new IllegalStateException("This request is not cancellable.");
    }
    isCancelled = true;
  }

  public Response response() {
    return response;
  }

  public String url() {
    return builder.url;
  }

  public int method() {
    return builder.method;
  }

  @Override
  public String toString() {
    if (response != null) {
      return String.format("[%s]", response.toString());
    }
    return String.format("%s %s", Method.name(method()), url());
  }
}
