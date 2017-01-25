package com.afollestad.bridge;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"}) public final class Request implements Serializable {

    @IntDef({Method.UNSPECIFIED, Method.GET, Method.POST, Method.PUT, Method.DELETE})
    @Retention(RetentionPolicy.SOURCE) @interface MethodInt {
    }

    private final RequestBuilder builder;
    private boolean isCancelled;
    boolean cancelCallbackFired;
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

    @WorkerThread Request makeRequest() throws BridgeException {
        try {
            URL url = new URL(builder.mUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int responseCode = -1;
            String responseMessage = "";
            HashMap<String, List<String>> responseHeaders = new HashMap<>();

            try {
                conn.setReadTimeout(builder.mReadTimeout);
                conn.setConnectTimeout(builder.mConnectTimeout);
                final String method = Method.name(builder.mMethod);
                Log2.d(Request.this, "Resolved HTTP method %d to %s", builder.mMethod, method);
                conn.setRequestMethod(method);
                conn.setInstanceFollowRedirects(true);

                if (builder.mHeaders != null && builder.mHeaders.size() > 0) {
                    for (final String key : builder.mHeaders.keySet()) {
                        final String value = valueToString(builder.mHeaders.get(key));
                        conn.setRequestProperty(key, value);
                        Log2.d(this, "HEADER %s = %s", key, value);
                    }
                }
                if (builder.mPipe != null) {
                    Log2.d(this, "HEADER Content-Length = %d", builder.mPipe.contentLength());
                    conn.setRequestProperty("Content-Length", builder.mPipe.contentLength() + "");
                } else if (builder.mBody != null) {
                    Log2.d(this, "HEADER Content-Length = %d", builder.mBody.length);
                    conn.setRequestProperty("Content-Length", builder.mBody.length + "");
                    conn.setFixedLengthStreamingMode(builder.mBody.length); // Disable internal buffering to make upload progress work properly
                }
                conn.setDoInput(true);

                checkCancelled();
                if (builder.mPipe != null || builder.mBody != null) {
                    if (builder.mUploadProgress != null)
                        builder.mUploadProgress.request = this;
                    conn.setDoOutput(true);
                    conn.connect();

                    Log2.d(this, "Connection established");
                    if (builder.mInfoCallback != null)
                        builder.mInfoCallback.onConnected(this);

                    OutputStream os = null;
                    try {
                        os = conn.getOutputStream();
                        if (builder.mPipe != null) {
                            Log2.d(this, "Uploading content from Pipe %s", builder.mPipe.getClass().getSimpleName());
                            builder.mPipe.writeTo(os, builder.mUploadProgress);
                            Log2.d(Request.this, "Wrote pipe content to %s %s request.",
                                    Method.name(method()), url());
                        } else {
                            Log2.d(this, "Uploading content from raw body.");
                            writeTo(builder.mBody, os, builder.mUploadProgress);
                            if (builder.mUploadProgress != null)
                                builder.mUploadProgress.publishProgress(builder.mBody.length, builder.mBody.length);
                            Log2.d(Request.this, "Wrote %d bytes to %s %s request.",
                                    builder.mBody.length, Method.name(method()), url());
                        }
                        os.flush();
                        if (builder.mInfoCallback != null)
                            builder.mInfoCallback.onRequestSent(this);
                    } finally {
                        if (builder.mPipe != null)
                            builder.mPipe.close();
                        BridgeUtil.closeQuietly(os);
                    }
                } else {
                    conn.connect();
                    Log2.d(this, "Connection established");
                    if (builder.mInfoCallback != null)
                        builder.mInfoCallback.onConnected(this);
                }

                checkCancelled();
                byte[] data = null;
                InputStream is = null;
                ByteArrayOutputStream bos = null;

                responseCode = conn.getResponseCode();
                responseMessage = conn.getResponseMessage();
                responseHeaders = new HashMap<>(conn.getHeaderFields());
                Log.d(Request.this, "%s %s status: %s %s", Method.name(method()), url(), responseCode, responseMessage);

                try {
                    Log2.d(this, "Preparing to receive data...");
                    is = conn.getInputStream();
                    byte[] buf = new byte[Bridge.config().bufferSize];
                    int read;
                    int totalRead = 0;
                    int totalAvailable;
                    if (conn.getHeaderField("Content-Length") != null) {
                        String clStr = conn.getHeaderField("Content-Length");
                        if (clStr == null) clStr = conn.getHeaderField("content-length");
                        totalAvailable = Integer.parseInt(clStr);
                    } else {
                        totalAvailable = is.available();
                    }

                    if (builder.mLineCallback != null) {
                        BufferedReader reader = null;
                        try {
                            reader = new BufferedReader(new InputStreamReader(is));
                            String line;
                            int count = 0;
                            while ((line = reader.readLine()) != null) {
                                builder.mLineCallback.onLine(line);
                                count++;
                            }
                            Log2.d(this, "Read %d lines from the server.", count);
                        } finally {
                            BridgeUtil.closeQuietly(reader);
                        }
                    } else {
                        bos = new ByteArrayOutputStream();
                        Log2.d(this, "Server reported %d bytes available for download.", totalAvailable);
                        if (totalAvailable != 0)
                            builder.mContext.fireProgress(Request.this, 0, totalAvailable);
                        while ((read = is.read(buf)) != -1) {
                            checkCancelled();
                            bos.write(buf, 0, read);
                            totalRead += read;
                            if (totalAvailable != 0)
                                builder.mContext.fireProgress(Request.this, totalRead, totalAvailable);
                            Log2.d(this, "Received %d/%d bytes...", totalRead, totalAvailable);
                        }
                        if (totalAvailable == 0)
                            builder.mContext.fireProgress(Request.this, 100, 100);
                        data = bos.toByteArray();
                        Log.d(Request.this, "Read %d bytes from the %s %s response.", data != null ?
                                data.length : 0, Method.name(method()), url());
                    }
                } finally {
                    BridgeUtil.closeQuietly(is);
                    BridgeUtil.closeQuietly(bos);
                }

                checkCancelled();
                response = new Response(data, url(), responseCode, responseMessage, responseHeaders, builder.mDidRedirect);
                if (builder.mThrowIfNotSuccess)
                    BridgeUtil.throwIfNotSuccess(response);
                conn.disconnect();

                if (responseCode >= 300 && responseCode <= 303) {
                    final List<String> locHeader = responseHeaders.get("Location");
                    if (locHeader.size() > 0) {
                        if (Bridge.config().autoFollowRedirects) {
                            // Follow redirect
                            Log.d(Request.this, "Following redirect: " + locHeader.get(0));
                            builder.prepareRedirect(locHeader.get(0));
                            return makeRequest(); // chain redirected request
                        } else {
                            Log2.d(Request.this, "Redirect NOT followed: " + locHeader.get(0));
                        }
                    }
                }

                Log.d(Request.this, "%s %s request completed successfully.", Method.name(method()), url());
            } catch (Exception fnf) {
                Log.e(Request.this, "Processing exception... %s, %s", fnf.getClass().getName(), fnf.getMessage());
                if (fnf instanceof BridgeException) {
                    if (((BridgeException) fnf).reason() != BridgeException.REASON_RESPONSE_UNSUCCESSFUL)
                        throw fnf; // redirect to outside catch
                } else if (!(fnf instanceof FileNotFoundException))
                    throw new BridgeException(this, fnf);
                InputStream es = null;
                try {
                    es = conn.getErrorStream();
                    response = new Response(BridgeUtil.readEntireStream(es), url(),
                            responseCode, responseMessage, responseHeaders, builder.mDidRedirect);
                } catch (Throwable e3) {
                    Log.e(Request.this, "Unable to get error stream... %s", e3.getMessage());
                    response = new Response(null, url(), responseCode,
                            responseMessage, responseHeaders, builder.mDidRedirect);
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
        if (builder.mValidators != null) {
            for (ResponseValidator val : builder.mValidators) {
                Log2.d(this, "Checking with validator %s...", val.id());
                try {
                    if (!val.validate(response))
                        throw new BridgeException(response, val);
                } catch (Exception e) {
                    if (e instanceof BridgeException)
                        throw (BridgeException) e;
                    throw new BridgeException(response, val, e);
                }
            }
        }
        if (builder.mThrowIfNotSuccess)
            BridgeUtil.throwIfNotSuccess(response);
        return this;
    }

    private void writeTo(byte[] bytes, OutputStream os, ProgressCallback progressCallback) throws IOException {
        byte[] buffer = new byte[Bridge.config().bufferSize];
        InputStream is = new ByteArrayInputStream(bytes);
        int read;
        int totalRead = 0;
        final int available = is.available();
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
            totalRead += read;
            if (progressCallback != null)
                progressCallback.publishProgress(totalRead, available);
        }
        // ByteArrayInputStream doesn't have to be closed
    }

    private void checkCancelled() throws BridgeException {
        if (isCancelled) {
            BridgeException ex = new BridgeException(this);
            Log.d(this, ex.getMessage());
            throw ex;
        }
    }

    public boolean isCancellable() {
        return builder().mCancellable;
    }

    public void cancel() {
        cancel(false);
    }

    public void cancel(boolean force) {
        if (!force && !isCancellable())
            throw new IllegalStateException("This request is not cancellable.");
        isCancelled = true;
    }

    public Response response() {
        return response;
    }

    public String url() {
        return builder.mUrl;
    }

    @MethodInt
    public int method() {
        return builder.mMethod;
    }

    @Override
    public String toString() {
        if (response != null)
            return String.format("[%s]", response.toString());
        return String.format("%s %s", Method.name(method()), url());
    }
}