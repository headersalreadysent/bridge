package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class RequestBuilder implements AsResultsExceptions, Serializable {

    protected final Bridge mContext;
    protected final String mUrl;
    protected final
    @Request.MethodInt
    int mMethod;
    protected HashMap<String, Object> mHeaders;
    protected byte[] mBody;
    protected Pipe mPipe;
    protected int mConnectTimeout;
    protected int mReadTimeout;
    protected int mBufferSize;
    private Request mRequest;
    protected boolean mCancellable = true;
    protected Object mTag;
    protected boolean mThrowIfNotSuccess = false;
    protected ResponseValidator[] mValidators;
    protected ProgressCallback mUploadProgress;
    protected InfoCallback mInfoCallback;

    protected RequestBuilder(String url, @Request.MethodInt int method, Bridge context) {
        mContext = context;
        if (!url.startsWith("http") && Bridge.client().config().mHost != null)
            url = Bridge.client().config().mHost + url;
        Log.d(this, "%s %s", Method.name(method), url);
        mUrl = url;
        mMethod = method;

        Config cf = Bridge.client().config();
        mHeaders = cf.mDefaultHeaders;
        mConnectTimeout = cf.mConnectTimeout;
        mReadTimeout = cf.mReadTimeout;
        mBufferSize = cf.mBufferSize;
        mValidators = cf.mValidators;
    }

    public RequestBuilder header(@NonNull String name, @NonNull Object value) {
        mHeaders.put(name, value);
        return this;
    }

    public RequestBuilder headers(@NonNull Map<String, ? extends Object> headers) {
        mHeaders.putAll(headers);
        return this;
    }

    public RequestBuilder connectTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Connect timeout must be greater than 0.");
        mConnectTimeout = timeout;
        return this;
    }

    public RequestBuilder readTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Read timeout must be greater than 0.");
        mReadTimeout = timeout;
        return this;
    }

    public RequestBuilder bufferSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Buffer size must be greater than 0.");
        mBufferSize = size;
        return this;
    }

    public RequestBuilder validators(ResponseValidator... validators) {
        mValidators = validators;
        return this;
    }

    public RequestBuilder body(@Nullable byte[] rawBody) {
        if (rawBody == null) {
            mBody = null;
            return this;
        }
        mBody = rawBody;
        return this;
    }

    public RequestBuilder body(@Nullable String textBody) {
        Log.d(this, "Body: %s", textBody);
        if (textBody == null) {
            mBody = null;
            return this;
        }
        header("Content-Type", "text/plain");
        try {
            mBody = textBody.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        return this;
    }

    public RequestBuilder body(@Nullable JSONObject json) {
        if (json == null) {
            mBody = null;
            return this;
        }
        body(json.toString());
        header("Content-Type", "application/json");
        return this;
    }

    public RequestBuilder body(@Nullable JSONArray json) {
        if (json == null) {
            mBody = null;
            return this;
        }
        body(json.toString());
        header("Content-Type", "application/json");
        return this;
    }

    public RequestBuilder body(@Nullable Form form) {
        if (form == null) {
            mBody = null;
            return this;
        }
        body(form.toString());
        header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    public RequestBuilder body(@Nullable MultipartForm form) {
        if (form == null) {
            mBody = null;
            return this;
        }
        body(form.data());
        header("Content-Type", String.format("multipart/form-data; boundary=%s", form.BOUNDARY));
        return this;
    }

    public RequestBuilder body(@NonNull Pipe pipe) {
        mPipe = pipe;
        header("Content-Type", pipe.contentType());
        return this;
    }

    public RequestBuilder body(@NonNull File file) {
        return body(Pipe.forFile(file));
    }

    public RequestBuilder uploadProgress(@NonNull ProgressCallback callback) {
        mUploadProgress = callback;
        return this;
    }

    public RequestBuilder infoCallback(@NonNull InfoCallback callback) {
        mInfoCallback = callback;
        return this;
    }

    public RequestBuilder cancellable(boolean cancelable) {
        mCancellable = cancelable;
        return this;
    }

    public RequestBuilder tag(@Nullable Object tag) {
        mTag = tag;
        return this;
    }

    public Request request() throws BridgeException {
        return new Request(this).makeRequest();
    }

    public RequestBuilder throwIfNotSuccess() {
        mThrowIfNotSuccess = true;
        return this;
    }

    public Request request(Callback callback) {
        mRequest = new Request(this);
        if (mContext.pushCallback(mRequest, callback)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRequest.makeRequest();
                        if (mRequest.mCancelCallbackFired) return;
                        final Response response = mRequest.response();
                        mContext.fireCallbacks(mRequest, response, null);
                    } catch (final BridgeException e) {
                        if (mRequest.mCancelCallbackFired) return;
                        mContext.fireCallbacks(mRequest, null, e);
                    }
                }
            }).start();
        }
        return mRequest;
    }

    // Shortcut methods

    public Response response() throws BridgeException {
        return request().response();
    }

    public byte[] asBytes() throws BridgeException {
        return response().asBytes();
    }

    public String asString() throws BridgeException {
        return response().asString();
    }

    @Override
    public Spanned asHtml() throws BridgeException {
        return response().asHtml();
    }

    @Override
    public Bitmap asBitmap() throws BridgeException {
        return response().asBitmap();
    }

    public JSONObject asJsonObject() throws BridgeException {
        return response().asJsonObject();
    }

    public JSONArray asJsonArray() throws BridgeException {
        return response().asJsonArray();
    }

    public void asFile(File destination) throws BridgeException {
        response().asFile(destination);
    }

    @Override
    public Object asSuggested() throws BridgeException {
        return response().asSuggested();
    }
}