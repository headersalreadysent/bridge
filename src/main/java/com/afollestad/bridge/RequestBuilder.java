package com.afollestad.bridge;

import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import com.afollestad.bridge.conversion.IConverter;
import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class RequestBuilder implements AsResultsExceptions, Serializable {

  final Bridge context;
  final int method;
  String url;
  HashMap<String, Object> headers;
  byte[] body;
  Pipe pipe;
  int connectTimeout;
  int readTimeout;
  int currentRetryCount;
  int totalRetryCount;
  long retrySpacingMs;
  RetryCallback retryCallback;
  boolean cancellable = true;
  Object tag;
  boolean throwIfNotSuccess = false;
  ResponseValidator[] validators;
  ProgressCallback uploadProgress;
  boolean didRedirect = false;
  int redirectCount = 0;
  private int bufferSize;
  private Request request;

  RequestBuilder(String url, int method, Bridge context) {
    this.context = context;
    final Config cf = Bridge.config();
    if (!url.startsWith("http") && cf.host != null) url = cf.host + url;

    LogCompat.d(this, "%s %s", Method.name(method), url);
    this.url = url;
    this.method = method;

    headers = cf.defaultHeaders;
    connectTimeout = cf.connectTimeout;
    readTimeout = cf.readTimeout;
    bufferSize = cf.bufferSize;
    validators = cf.validators;
  }

  void prepareRedirect(String url) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      int startIndex = this.url.indexOf('/');
      startIndex = this.url.indexOf('/', startIndex + 1);
      String host = this.url.substring(0, this.url.indexOf("/", startIndex + 1));
      url = host + (url.startsWith("/") ? "" : "/") + url;
    }
    this.url = url;
    this.didRedirect = true;
    this.redirectCount++;
    if (this.redirectCount > Bridge.config().maxRedirects) {
      throw new IllegalStateException(
          "Max redirect count is "
              + Bridge.config().maxRedirects
              + ", "
              + url
              + " tried to redirect more.");
    }
  }

  public RequestBuilder header(@NotNull String name, @NotNull Object value) {
    headers.put(name, value);
    return this;
  }

  public RequestBuilder headers(@NotNull Map<String, ?> headers) {
    this.headers.putAll(headers);
    return this;
  }

  public RequestBuilder contentType(@NotNull String contentType) {
    return header("Content-Type", contentType);
  }

  public RequestBuilder authentication(@NotNull Authentication authentication) {
    try {
      authentication.apply(this);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to apply authentication " + authentication.getClass().getName(), e);
    }
    return this;
  }

  public RequestBuilder retries(int count, long retrySpacing) {
    return retries(count, retrySpacing, null);
  }

  public RequestBuilder retries(int count, long retrySpacing, @Nullable RetryCallback callback) {
    this.totalRetryCount = count;
    this.retrySpacingMs = retrySpacing;
    this.retryCallback = callback;
    return this;
  }

  public RequestBuilder connectTimeout(int timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Connect timeout must be greater than 0.");
    }
    connectTimeout = timeout;
    return this;
  }

  public RequestBuilder readTimeout(int timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Read timeout must be greater than 0.");
    }
    readTimeout = timeout;
    return this;
  }

  public RequestBuilder bufferSize(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Buffer size must be greater than 0.");
    }
    bufferSize = size;
    return this;
  }

  public RequestBuilder validators(ResponseValidator... validators) {
    this.validators = validators;
    return this;
  }

  public RequestBuilder body(@Nullable byte[] rawBody) {
    if (rawBody == null) {
      body = null;
      return this;
    }
    body = rawBody;
    return this;
  }

  public RequestBuilder body(@Nullable String textBody) {
    LogCompat.d(this, "Body: %s", textBody);
    if (textBody == null) {
      body = null;
      return this;
    }
    contentType("text/plain");
    try {
      body = textBody.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never happen
      throw new RuntimeException(e);
    }
    return this;
  }

  public RequestBuilder body(@Nullable Ason json) {
    return body(json != null ? json.toStockJson() : null);
  }

  public RequestBuilder body(@Nullable AsonArray json) {
    return body(json != null ? json.toStockJson() : null);
  }

  public RequestBuilder body(@Nullable JSONObject json) {
    if (json == null) {
      body = null;
      return this;
    }
    body(json.toString());
    contentType("application/json");
    return this;
  }

  public RequestBuilder body(@Nullable JSONArray json) {
    if (json == null) {
      body = null;
      return this;
    }
    body(json.toString());
    contentType("application/json");
    return this;
  }

  public RequestBuilder body(@Nullable Form form) {
    if (form == null) {
      body = null;
      return this;
    }
    body(form.toString());
    contentType("application/x-www-form-urlencoded");
    return this;
  }

  public RequestBuilder body(@Nullable MultipartForm form) {
    if (form == null) {
      body = null;
      return this;
    }
    body(form.data());
    contentType(String.format("multipart/form-data; boundary=%s", form.BOUNDARY));
    return this;
  }

  public RequestBuilder body(@NotNull Pipe pipe) {
    this.pipe = pipe;
    contentType(pipe.contentType());
    return this;
  }

  public RequestBuilder body(@NotNull File file) {
    return body(Pipe.forFile(file));
  }

  public RequestBuilder body(@Nullable Object object) {
    if (object instanceof List) {
      //noinspection unchecked
      return body((List) object);
    }
    if (object == null) {
      body = null;
    } else {
      final long start = System.currentTimeMillis();
      final String contentType =
          BridgeUtil.getContentType(object.getClass(), headers.get("Content-Type"));
      contentType(contentType);
      IConverter converter = Bridge.config().converter(contentType);
      try {
        body = converter.serialize(object);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to serialize object to body!", e);
      }
      final long diff = System.currentTimeMillis() - start;
      LogCompat.d(
          this,
          "Request conversion took " + "%dms (%d seconds) for object of type %s.",
          diff,
          diff / 1000,
          object.getClass().getName());
    }
    return this;
  }

  public RequestBuilder body(@Nullable Object[] objects) {
    if (objects == null || objects.length == 0) {
      body = null;
    } else {
      final long start = System.currentTimeMillis();
      final String contentType =
          BridgeUtil.getContentType(objects[0].getClass(), headers.get("Content-Type"));
      contentType(contentType);
      IConverter converter = Bridge.config().converter(contentType);
      try {
        body = converter.serializeArray(objects);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to serialize array to body!", e);
      }
      final long diff = System.currentTimeMillis() - start;
      LogCompat.d(
          this,
          "Request conversion took " + "%dms (%d seconds) for array of %s objects.",
          diff,
          diff / 1000,
          objects[0].getClass().getName());
    }
    return this;
  }

  public RequestBuilder body(@Nullable List<Object> objects) {
    if (objects == null || objects.size() == 0) {
      body = null;
    } else {
      final long start = System.currentTimeMillis();
      final String contentType =
          BridgeUtil.getContentType(objects.get(0).getClass(), headers.get("Content-Type"));
      contentType(contentType);
      IConverter converter = Bridge.config().converter(contentType);
      try {
        body = converter.serializeList(objects);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to serialize list to body!", e);
      }
      final long diff = System.currentTimeMillis() - start;
      LogCompat.d(
          this,
          "Request conversion took " + "%dms (%d seconds) for list of %s objects.",
          diff,
          diff / 1000,
          objects.get(0).getClass().getName());
    }
    return this;
  }

  public RequestBuilder uploadProgress(@NotNull ProgressCallback callback) {
    uploadProgress = callback;
    return this;
  }

  public RequestBuilder cancellable(boolean cancelable) {
    cancellable = cancelable;
    return this;
  }

  public RequestBuilder tag(@Nullable Object tag) {
    this.tag = tag;
    return this;
  }

  public Request request() throws BridgeException {
    return new Request(this).makeRequest();
  }

  public RequestBuilder throwIfNotSuccess() {
    throwIfNotSuccess = true;
    return this;
  }

  public Request request(Callback callback) {
    request = new Request(this);
    if (context.pushCallback(request, callback)) {
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    request.makeRequest();
                    if (request.cancelCallbackFired) return;
                    final Response response = request.response();
                    context.fireCallbacks(request, response, null);
                  } catch (final BridgeException e) {
                    if (request.cancelCallbackFired) return;
                    context.fireCallbacks(request, request.response(), e);
                  }
                }
              })
          .start();
    }
    return request;
  }

  // Shortcut methods

  @Nullable
  public Response response() throws BridgeException {
    return request().response();
  }

  @Nullable
  public byte[] asBytes() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) return null;
    return response.asBytes();
  }

  @Override
  public void asBytes(final @NotNull ResponseConvertCallback<byte[]> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              callback.onResponse(response, response.asBytes(), null);
            }
          }
        });
  }

  @Nullable
  @Override
  public String asString() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asString();
  }

  @Override
  public void asString(final @NotNull ResponseConvertCallback<String> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              callback.onResponse(response, response.asString(), null);
            }
          }
        });
  }

  @Nullable
  public Ason asAsonObject() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asAsonObject();
  }

  public void asAsonObject(@NotNull final ResponseConvertCallback<Ason> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asAsonObject(), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  public AsonArray<?> asAsonArray() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asAsonArray();
  }

  public void asAsonArray(@NotNull final ResponseConvertCallback<AsonArray<?>> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asAsonArray(), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  public JSONObject asJsonObject() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asJsonObject();
  }

  @Override
  public void asJsonObject(final @NotNull ResponseConvertCallback<JSONObject> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asJsonObject(), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  public JSONArray asJsonArray() throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asJsonArray();
  }

  @Override
  public void asJsonArray(final @NotNull ResponseConvertCallback<JSONArray> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asJsonArray(), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  @Override
  public <T> List<T> asClassList(@NotNull Class<T> cls) throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) return null;
    return response.asClassList(cls);
  }

  @Override
  public <T> void asClassList(
      final @NotNull Class<T> cls, final @NotNull ResponseConvertCallback<List<T>> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asClassList(cls), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  public void asFile(@NotNull File destination) throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      throw new BridgeException(
          request(),
          "No response was " + "returned to save into a file.",
          BridgeException.REASON_RESPONSE_UNPARSEABLE);
    }
    response.asFile(destination);
  }

  @Override
  public void asFile(
      final @NotNull File destination, final @NotNull ResponseConvertCallback<File> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                response.asFile(destination);
                callback.onResponse(response, destination, null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  @Override
  public <T> T asClass(@NotNull Class<T> cls) throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asClass(cls);
  }

  @Override
  public <T> void asClass(
      final @NotNull Class<T> cls, final @NotNull ResponseConvertCallback<T> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asClass(cls), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }

  @Nullable
  @Override
  public <T> T[] asClassArray(@NotNull Class<T> cls) throws BridgeException {
    throwIfNotSuccess();
    Response response = response();
    if (response == null) {
      return null;
    }
    return response.asClassArray(cls);
  }

  @Override
  public <T> void asClassArray(
      final @NotNull Class<T> cls, final @NotNull ResponseConvertCallback<T[]> callback) {
    request(
        new Callback() {
          @Override
          public void response(@NotNull Request request, Response response, BridgeException e) {
            if (e != null) {
              callback.onResponse(response, null, e);
            } else {
              try {
                callback.onResponse(response, response.asClassArray(cls), null);
              } catch (BridgeException e1) {
                callback.onResponse(response, null, e1);
              }
            }
          }
        });
  }
}
