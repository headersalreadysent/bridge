package com.afollestad.bridge;

import android.support.annotation.IntDef;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
public final class Request implements Serializable {

    @IntDef({Method.GET, Method.POST, Method.PUT, Method.DELETE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MethodInt {
    }

    private final RequestBuilder mBuilder;
    private boolean isCancelled;
    protected boolean mCancelCallbackFired;
    private Response mResponse;

    protected Request(RequestBuilder builder) {
        mBuilder = builder;
    }

    protected RequestBuilder builder() {
        return mBuilder;
    }

    protected Request makeRequest() throws BridgeException {
        try {
            URL url = new URL(mBuilder.mUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int responseCode = -1;
            String responseMessage = "";
            HashMap<String, List<String>> responseHeaders = new HashMap<>();

            try {
                conn.setReadTimeout(mBuilder.mReadTimeout);
                conn.setConnectTimeout(mBuilder.mConnectTimeout);
                conn.setRequestMethod(Method.name(mBuilder.mMethod));
                conn.setInstanceFollowRedirects(true);

                if (mBuilder.mHeaders != null && mBuilder.mHeaders.size() > 0) {
                    for (final String key : mBuilder.mHeaders.keySet()) {
                        final Object value = mBuilder.mHeaders.get(key);
                        conn.setRequestProperty(key, value + "");
                    }
                }
                if (mBuilder.mPipe != null)
                    conn.setRequestProperty("Content-Length", mBuilder.mPipe.contentLength() + "");
                else if (mBuilder.mBody != null)
                    conn.setRequestProperty("Content-Length", mBuilder.mBody.length + "");
                conn.setDoInput(true);

                checkCancelled();
                if (mBuilder.mPipe != null || mBuilder.mBody != null) {
                    if (mBuilder.mUploadProgress != null)
                        mBuilder.mUploadProgress.mRequest = this;
                    conn.setDoOutput(true);
                    conn.connect();
                    if (mBuilder.mInfoCallback != null)
                        mBuilder.mInfoCallback.onConnected(this);

                    OutputStream os = null;
                    try {
                        os = conn.getOutputStream();
                        if (mBuilder.mPipe != null) {
                            mBuilder.mPipe.writeTo(os, mBuilder.mUploadProgress);
                            Log.d(Request.this, "Wrote pipe content to %s %s request.",
                                    Method.name(method()), url());
                        } else {
                            os.write(mBuilder.mBody);
                            if (mBuilder.mUploadProgress != null)
                                mBuilder.mUploadProgress.publishProgress(mBuilder.mBody.length, mBuilder.mBody.length);
                            Log.d(Request.this, "Wrote %d bytes to %s %s request.",
                                    mBuilder.mBody.length, Method.name(method()), url());
                        }
                        os.flush();
                        if (mBuilder.mInfoCallback != null)
                            mBuilder.mInfoCallback.onRequestSent(this);
                    } finally {
                        BridgeUtil.closeQuietly(os);
                    }
                } else {
                    conn.connect();
                    if (mBuilder.mInfoCallback != null)
                        mBuilder.mInfoCallback.onConnected(this);
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
                    is = conn.getInputStream();
                    bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[Bridge.client().config().mBufferSize];
                    int read;
                    int totalRead = 0;
                    int totalAvailable;
                    if (conn.getHeaderField("Content-Length") != null)
                        totalAvailable = Integer.parseInt(conn.getHeaderField("Content-Length"));
                    else totalAvailable = is.available();
                    if (totalAvailable != 0)
                        mBuilder.mContext.fireProgress(Request.this, 0, totalAvailable);
                    while ((read = is.read(buf)) != -1) {
                        checkCancelled();
                        bos.write(buf, 0, read);
                        totalRead += read;
                        if (totalAvailable != 0)
                            mBuilder.mContext.fireProgress(Request.this, totalRead, totalAvailable);
                    }
                    if (totalAvailable == 0)
                        mBuilder.mContext.fireProgress(Request.this, 100, 100);
                    data = bos.toByteArray();
                    Log.d(Request.this, "Read %d bytes from the %s %s response.", data != null ?
                            data.length : 0, Method.name(method()), url());
                } finally {
                    BridgeUtil.closeQuietly(is);
                    BridgeUtil.closeQuietly(bos);
                }

                checkCancelled();
                mResponse = new Response(data, url(), responseCode, responseMessage, responseHeaders);
                if (mBuilder.mThrowIfNotSuccess)
                    BridgeUtil.throwIfNotSuccess(mResponse);
                conn.disconnect();
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
                    mResponse = new Response(BridgeUtil.readEntireStream(es), url(),
                            responseCode, responseMessage, responseHeaders);
                } catch (Throwable e3) {
                    Log.e(Request.this, "Unable to get error stream... %s", e3.getMessage());
                    mResponse = new Response(null, url(), responseCode,
                            responseMessage, responseHeaders);
                } finally {
                    BridgeUtil.closeQuietly(es);
                    if (conn != null) conn.disconnect();
                }
            }
        } catch (Exception e) {
            if (e instanceof BridgeException) {
                ((BridgeException) e).mRequest = this;
                throw (BridgeException) e;
            }
            throw new BridgeException(this, e);
        }
        if (mBuilder.mValidators != null) {
            for (ResponseValidator val : mBuilder.mValidators) {
                try {
                    if (!val.validate(mResponse))
                        throw new BridgeException(mResponse, val);
                } catch (Exception e) {
                    if (e instanceof BridgeException)
                        throw (BridgeException) e;
                    throw new BridgeException(mResponse, val, e);
                }
            }
        }
        if (mBuilder.mThrowIfNotSuccess)
            BridgeUtil.throwIfNotSuccess(mResponse);
        return this;
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
        return mResponse;
    }

    public String url() {
        return mBuilder.mUrl;
    }

    @MethodInt
    public int method() {
        return mBuilder.mMethod;
    }

    @Override
    public String toString() {
        if (mResponse != null)
            return String.format("[%s]", mResponse.toString());
        return String.format("%s %s", Method.name(method()), url());
    }
}