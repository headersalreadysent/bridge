package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Response implements AsResults, Serializable {

    private final String mUrl;
    private final byte[] mData;
    private int mCode = -1;
    private String mMessage;
    private Bitmap mBitmapCache;
    private JSONObject mJsonObjCache;
    private JSONArray mJsonArrayCache;
    private Map<String, List<String>> mHeaders;

    protected Response(byte[] data, String url, int code, String message, Map<String, List<String>> headers) throws IOException {
        mData = data;
        mUrl = url;
        mCode = code;
        mMessage = message;
        mHeaders = headers;
    }

    public String url() {
        return mUrl;
    }

    public int code() {
        return mCode;
    }

    public String phrase() {
        return mMessage;
    }

    @Nullable
    public String header(String name) {
        List<String> header = mHeaders.get(name);
        if (header == null || header.isEmpty())
            return null;
        return header.get(0);
    }

    public Map<String, List<String>> headers() {
        return mHeaders;
    }

    @Nullable
    public List<String> headerList(String name) {
        return mHeaders.get(name);
    }

    public int contentLength() {
        String contentLength = header("Content-Length");
        if (contentLength == null) return -1;
        return Integer.parseInt(contentLength);
    }

    @Nullable
    public String contentType() {
        return header("Content-Type");
    }

    public boolean isSuccess() {
        //noinspection PointlessBooleanExpression
        return mCode == -1 || mCode >= 200 && mCode < 300;
    }

    @Nullable
    public byte[] asBytes() {
        return mData;
    }

    @Nullable
    public String asString() {
        try {
            final byte[] bytes = asBytes();
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public Spanned asHtml() {
        final String content = asString();
        if (content == null)
            return null;
        return Html.fromHtml(content);
    }

    @Override
    public Bitmap asBitmap() {
        if (mBitmapCache == null) {
            final InputStream is = new ByteArrayInputStream(asBytes());
            mBitmapCache = BitmapFactory.decodeStream(is);
            BridgeUtil.closeQuietly(is);
        }
        return mBitmapCache;
    }

    public JSONObject asJsonObject() throws BridgeException {
        final String content = asString();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        try {
            if (mJsonObjCache == null)
                mJsonObjCache = new JSONObject(content);
            return mJsonObjCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public JSONArray asJsonArray() throws BridgeException {
        final String content = asString();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        try {
            if (mJsonArrayCache == null)
                mJsonArrayCache = new JSONArray(content);
            return mJsonArrayCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public void asFile(File destination) throws BridgeException {
        final byte[] content = asBytes();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destination);
            os.write(content);
            os.flush();
        } catch (IOException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_IOERROR);
        } finally {
            BridgeUtil.closeQuietly(os);
        }
    }

    @Override
    public Object asSuggested() throws BridgeException {
        final String contentType = contentType();
        if (contentType == null) {
            return asBytes();
        } else if (contentType.startsWith("text/html")) {
            return asHtml();
        } else if (contentType.startsWith("text/")) {
            return asString();
        } else if (contentType.startsWith("application/json")) {
            try {
                final String contentStr = asString();
                if (contentStr == null) return asBytes();
                if (contentStr.startsWith("["))
                    return new JSONArray(contentStr);
                else return new JSONObject(contentStr);
            } catch (JSONException e) {
                throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
            }
        } else if (contentType.startsWith("image/")) {
            return asBitmap();
        }
        return asBytes();
    }

    @Override
    public String toString() {
        Object suggested;
        try {
            suggested = asSuggested();
        } catch (BridgeException e) {
            suggested = asBytes();
        }
        String bodyDescriptor = suggested instanceof byte[] ?
                String.format("%d bytes", ((byte[]) suggested).length) : suggested.toString();
        return String.format("%s, %d %s, %s", mUrl, mCode, mMessage, bodyDescriptor);
    }
}
