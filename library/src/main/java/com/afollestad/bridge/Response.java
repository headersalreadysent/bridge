package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Response implements AsResults, Serializable {

    private final static int BUFFER_SIZE_GZIP = 32;

    private final String url;
    private final byte[] data;
    private int code = -1;
    private String message;
    private transient Bitmap bitmapCache;
    private transient JSONObject jsonObjCache;
    private transient JSONArray jsonArrayCache;
    private HashMap<String, List<String>> headers;
    private boolean didRedirect;

    protected Response(byte[] data, String url, int code, String message, HashMap<String, List<String>> headers, boolean didRedirect) throws IOException {
        this.data = data;
        this.url = url;
        this.code = code;
        this.message = message;
        this.headers = headers;
        this.didRedirect = didRedirect;
    }

    public boolean didRedirect() {
        return didRedirect;
    }

    public String url() {
        return url;
    }

    public int code() {
        return code;
    }

    public String phrase() {
        return message;
    }

    @Nullable
    public String header(String name) {
        List<String> header = headers.get(name);
        if (header == null || header.isEmpty())
            return null;
        return header.get(0);
    }

    public boolean headerEquals(String name, String value) {
        String headerVal = header(name);
        return headerVal == null && value == null ||
                (headerVal != null && headerVal.equals(value));
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    @Nullable
    public List<String> headerList(String name) {
        return headers.get(name);
    }

    public int contentLength() {
        String contentLength = header("Content-Length");
        if (contentLength == null) contentLength = header("content-length");
        if (contentLength == null) return -1;
        return Integer.parseInt(contentLength);
    }

    @Nullable
    public String contentType() {
        String contentType = header("Content-Type");
        if (contentType == null) contentType = header("content-type");
        return contentType;
    }

    @Nullable
    public String contentEncoding() {
        String contentEncoding = header("Content-Encoding");
        if (contentEncoding == null) contentEncoding = header("content-encoding");
        return contentEncoding;
    }

    public boolean isSuccess() {
        //noinspection PointlessBooleanExpression
        boolean success = code == -1 || code >= 200 && code <= 303;
        if (!success)
            Log2.d(this, "HTTP status %d was considered unsuccessful.", code);
        return success;
    }

    @Nullable
    public byte[] asBytes() {
        String encoding = contentEncoding();
        if (encoding != null && encoding.contains("gzip")) {
            try {
                return decompressGZIP(data);
            } catch (IOException e) {
                // GZIP content might be corrupted
                throw new RuntimeException(e);
            }
        }
        return data;
    }

    @Nullable
    public String asString() {
        try {
            final byte[] bytes = asBytes();
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, "UTF-8");

        } catch (IOException e) {
            // This should never happend
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public Spanned asHtml() {
        final String content = asString();
        if (content == null)
            return null;
        return Html.fromHtml(content);
    }

    @Nullable
    @Override
    public Bitmap asBitmap() {
        if (bitmapCache == null) {
            final InputStream is = new ByteArrayInputStream(asBytes());
            bitmapCache = BitmapFactory.decodeStream(is);
            BridgeUtil.closeQuietly(is);
        }
        return bitmapCache;
    }

    @Nullable
    public JSONObject asJsonObject() throws BridgeException {
        final String content = asString();
        if (content == null) return null;
        try {
            if (jsonObjCache == null)
                jsonObjCache = new JSONObject(content);
            return jsonObjCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    @Nullable
    public JSONArray asJsonArray() throws BridgeException {
        final String content = asString();
        if (content == null) return null;
        try {
            if (jsonArrayCache == null)
                jsonArrayCache = new JSONArray(content);
            return jsonArrayCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public void asFile(@NonNull File destination) throws BridgeException {
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

    @Nullable
    public <T> T asClass(@NonNull Class<T> cls) throws BridgeException {
        String contentType = contentType();
        if (contentType == null) {
            String msg = String.format(Locale.getDefault(),
                    "Response has no Content-Type, cannot determine appropriate response converter. Response status: %d.", code);
            for (String key : headers.keySet())
                msg += String.format(Locale.getDefault(), "\n    %s = %s", key, headers.get(key).get(0));
            throw new BridgeException(this, msg, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
        final long start = System.currentTimeMillis();
        T result = Bridge.config()
                .responseConverter(contentType)
                .convertObject(this, cls);
        final long diff = System.currentTimeMillis() - start;
        Log.d(this, "Response conversion to class %s took %d milliseconds (%d seconds).",
                cls.getName(), diff, diff / 1000);
        return result;
    }

    @Nullable
    public <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException {
        String contentType = contentType();
        if (contentType == null) {
            String msg = String.format(Locale.getDefault(),
                    "Response has no Content-Type, cannot determine appropriate response converter. Response status: %d.", code);
            for (String key : headers.keySet())
                msg += String.format(Locale.getDefault(), "\n    %s = %s", key, headers.get(key).get(0));
            throw new BridgeException(this, msg, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
        final long start = System.currentTimeMillis();
        T[] result = Bridge.config()
                .responseConverter(contentType)
                .convertArray(this, cls);
        final long diff = System.currentTimeMillis() - start;
        Log.d(this, "Response conversion to array of class %s took %d milliseconds (%d seconds).",
                cls.getName(), diff, diff / 1000);
        return result;
    }

    @Nullable
    @Override
    public <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException {
        T[] array = asClassArray(cls);
        return array == null ? null : Arrays.asList(array);
    }

    @Nullable
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
                throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE, true);
            }
        } else if (contentType.startsWith("image/")) {
            return asBitmap();
        }
        return asBytes();
    }

    @Nullable
    @Override
    public String toString() {
        Object suggested;
        try {
            suggested = asSuggested();
        } catch (BridgeException e) {
            suggested = asBytes();
        }
        String bodyDescriptor = suggested instanceof byte[] ?
                String.format(Locale.US, "%d bytes", ((byte[]) suggested).length) :
                suggested != null ? suggested.toString() : "(null)";
        return String.format(Locale.US, "%s, %d %s, %s", url, code, message, bodyDescriptor);
    }

    @Nullable
    private byte[] decompressGZIP(byte[] compressed) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE_GZIP);
        try {
            byte[] data = new byte[BUFFER_SIZE_GZIP];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1) {
                os.write(data, 0, bytesRead);
            }
        } finally {
            gis.close();
            is.close();
        }

        return os.toByteArray();
    }
}