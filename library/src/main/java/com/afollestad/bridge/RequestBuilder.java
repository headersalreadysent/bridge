package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.text.Spanned;

import com.afollestad.bridge.conversion.base.RequestConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class RequestBuilder implements AsResultsExceptions, Serializable {

    final Bridge context;
    String url;
    @Request.MethodInt final int method;
    HashMap<String, Object> headers;
    byte[] body;
    Pipe pipe;
    int connectTimeout;
    int readTimeout;
    private int bufferSize;
    private Request request;
    boolean cancellable = true;
    Object tag;
    boolean throwIfNotSuccess = false;
    ResponseValidator[] validators;
    ProgressCallback uploadProgress;
    InfoCallback infoCallback;
    LineCallback lineCallback;
    boolean didRedirect = false;

    void prepareRedirect(String url) {
        this.url = url;
        this.didRedirect = true;
    }

    RequestBuilder(String url, @Request.MethodInt int method, Bridge context) {
        this.context = context;
        final Config cf = Bridge.config();
        if (!url.startsWith("http") && cf.host != null)
            url = cf.host + url;

        Log.d(this, "%s %s", Method.name(method), url);
        this.url = url;
        this.method = method;

        headers = cf.defaultHeaders;
        connectTimeout = cf.connectTimeout;
        readTimeout = cf.readTimeout;
        bufferSize = cf.bufferSize;
        validators = cf.validators;
    }

    public RequestBuilder header(@NonNull String name, @NonNull Object value) {
        headers.put(name, value);
        return this;
    }

    public RequestBuilder headers(@NonNull Map<String, ? extends Object> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public RequestBuilder contentType(@NonNull String contentType) {
        return header("Content-Type", contentType);
    }

    public RequestBuilder connectTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Connect timeout must be greater than 0.");
        connectTimeout = timeout;
        return this;
    }

    public RequestBuilder readTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Read timeout must be greater than 0.");
        readTimeout = timeout;
        return this;
    }

    public RequestBuilder bufferSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Buffer size must be greater than 0.");
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
        Log.d(this, "Body: %s", textBody);
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

    public RequestBuilder body(@NonNull Pipe pipe) {
        this.pipe = pipe;
        contentType(pipe.contentType());
        return this;
    }

    public RequestBuilder body(@NonNull File file) {
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
            final String contentType = RequestConverter.getContentType(object.getClass(), headers.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            body = converter.convertObject(object, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for object of type %s.",
                    diff, diff / 1000, object.getClass().getName());
        }
        return this;
    }

    public RequestBuilder body(@Nullable Object[] objects) {
        if (objects == null || objects.length == 0) {
            body = null;
        } else {
            final long start = System.currentTimeMillis();
            final String contentType = RequestConverter.getContentType(objects[0].getClass(), headers.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            body = converter.convertArray(objects, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for array of %s objects.",
                    diff, diff / 1000, objects[0].getClass().getName());
        }
        return this;
    }

    public RequestBuilder body(@Nullable List<Object> objects) {
        if (objects == null || objects.size() == 0) {
            body = null;
        } else {
            final long start = System.currentTimeMillis();
            final String contentType = RequestConverter.getContentType(objects.get(0).getClass(), headers.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            body = converter.convertList(objects, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for list of %s objects.",
                    diff, diff / 1000, objects.get(0).getClass().getName());
        }
        return this;
    }

    public RequestBuilder uploadProgress(@NonNull ProgressCallback callback) {
        uploadProgress = callback;
        return this;
    }

    public RequestBuilder infoCallback(@NonNull InfoCallback callback) {
        infoCallback = callback;
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

    @WorkerThread public Request request() throws BridgeException {
        return new Request(this).makeRequest();
    }

    public RequestBuilder throwIfNotSuccess() {
        throwIfNotSuccess = true;
        return this;
    }

    @UiThread public Request request(Callback callback) {
        request = new Request(this);
        if (context.pushCallback(request, callback)) {
            new Thread(new Runnable() {
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
            }).start();
        }
        return request;
    }

    // Shortcut methods

    @Nullable
    @WorkerThread
    public Response response() throws BridgeException {
        return request().response();
    }

    @Nullable
    @WorkerThread
    public byte[] asBytes() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asBytes();
    }

    @Override public void asBytes(final @NonNull ResponseConvertCallback<byte[]> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asBytes(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    @Override
    public String asString() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asString();
    }

    @Override public void asString(final @NonNull ResponseConvertCallback<String> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asString(), null);
            }
        });
    }

    @Override public Response asLineStream(@NonNull LineCallback cb) throws BridgeException {
        lineCallback = cb;
        return response();
    }

    @Override public void asLineStream(@NonNull LineCallback cb, @NonNull Callback callback) {
        lineCallback = cb;
        request(callback);
    }

    @Nullable
    @WorkerThread
    @Override
    public Spanned asHtml() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asHtml();
    }

    @Override public void asHtml(final @NonNull ResponseConvertCallback<Spanned> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asHtml(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    @Override
    public Bitmap asBitmap() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asBitmap();
    }

    @Override public void asBitmap(final @NonNull ResponseConvertCallback<Bitmap> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asBitmap(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    public JSONObject asJsonObject() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asJsonObject();
    }

    @Override
    public void asJsonObject(final @NonNull ResponseConvertCallback<JSONObject> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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
    @WorkerThread
    public JSONArray asJsonArray() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asJsonArray();
    }

    @Override public void asJsonArray(final @NonNull ResponseConvertCallback<JSONArray> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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
    public <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClassList(cls);
    }

    @Override
    public <T> void asClassList(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<List<T>> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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

    @WorkerThread public void asFile(@NonNull File destination) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null)
            throw new BridgeException(request(), "No response was returned to save into a file.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        response.asFile(destination);
    }

    @Override
    public void asFile(final @NonNull File destination, final @NonNull ResponseConvertCallback<File> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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
    public <T> T asClass(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClass(cls);
    }

    @Override
    public <T> void asClass(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<T> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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
    public <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClassArray(cls);
    }

    @Override
    public <T> void asClassArray(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<T[]> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
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

    @Nullable
    @WorkerThread
    @Override
    public Object asSuggested() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asSuggested();
    }

    @Override public void asSuggested(final @NonNull ResponseConvertCallback<Object> callback) {
        request(new Callback() {
            @Override
            public void response(@NonNull Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asSuggested(), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }
}