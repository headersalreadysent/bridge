package com.afollestad.bridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Request {

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
            try {
                conn.setReadTimeout(mBuilder.mReadTimeout);
                conn.setConnectTimeout(mBuilder.mConnectTimeout);
                conn.setRequestMethod(mBuilder.mMethod.name());
                conn.setInstanceFollowRedirects(true);

                if (mBuilder.mHeaders != null && mBuilder.mHeaders.size() > 0) {
                    for (final String key : mBuilder.mHeaders.keySet()) {
                        final Object value = mBuilder.mHeaders.get(key);
                        conn.setRequestProperty(key, value + "");
                    }
                }
                conn.setDoInput(true);

                checkCancelled();
                if (mBuilder.mPipe != null || mBuilder.mBody != null) {
                    conn.setDoOutput(true);
                    OutputStream os = null;
                    try {
                        os = conn.getOutputStream();
                        if (mBuilder.mPipe != null) {
                            mBuilder.mPipe.writeTo(os);
                        } else {
                            os.write(mBuilder.mBody);
                        }
                        os.flush();
                    } finally {
                        Util.closeQuietly(os);
                    }
                }

                checkCancelled();
                byte[] data = null;
                InputStream is = null;
                ByteArrayOutputStream bos = null;
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
                } finally {
                    Util.closeQuietly(is);
                    Util.closeQuietly(bos);
                }

                checkCancelled();
                mResponse = new Response(data, url(), conn);
                if (mBuilder.mThrowIfNotSuccess)
                    Util.throwIfNotSuccess(mResponse);
            } catch (Exception fnf) {
                if (fnf instanceof BridgeException) {
                    if (((BridgeException) fnf).reason() != BridgeException.REASON_RESPONSE_UNSUCCESSFUL)
                        throw fnf; // redirect to outside catch
                }
                InputStream es = null;
                try {
                    es = conn.getErrorStream();
                    mResponse = new Response(Util.readEntireStream(es), url(), conn);
                } catch (IOException e3) {
                    mResponse = new Response(null, url(), conn);
                } finally {
                    Util.closeQuietly(es);
                }
                if (mBuilder.mThrowIfNotSuccess)
                    Util.throwIfNotSuccess(mResponse);
            } finally {
                conn.disconnect();
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
                    if (!val.validate(mResponse)) {
                        throw new BridgeException(mResponse, val);
                    }
                } catch (Exception e) {
                    throw new BridgeException(mResponse, val, e);
                }
            }
        }
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

    public Method method() {
        return mBuilder.mMethod;
    }

    @Override
    public String toString() {
        if (mResponse != null)
            return String.format("[%s]: %s %s", mResponse.toString(), method().name(), url());
        return String.format("%s %s", method().name(), url());
    }
}