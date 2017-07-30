package com.afollestad.bridge;

import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Response implements AsResults, Serializable {

  private static final int BUFFER_SIZE_GZIP = 32;

  private final String url;
  private final byte[] data;
  private int code = -1;
  private String message;
  private transient Ason asonObjCache;
  private transient AsonArray<?> asonArrayCache;
  private HashMap<String, List<String>> headers;
  private boolean didRedirect;
  private int redirectCount;

  protected Response(
      byte[] data,
      String url,
      int code,
      String message,
      HashMap<String, List<String>> headers,
      boolean didRedirect,
      int redirectCount)
      throws IOException {
    this.data = data;
    this.url = url;
    this.code = code;
    this.message = message;
    this.headers = headers;
    this.didRedirect = didRedirect;
    this.redirectCount = redirectCount;
  }

  public boolean didRedirect() {
    return didRedirect;
  }

  public int redirectCount() {
    return redirectCount;
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

  public boolean isSuccess() {
    //noinspection PointlessBooleanExpression
    return code == -1 || code >= 200 && code <= 303;
  }

  @Nullable
  public String header(String name) {
    List<String> header = headers.get(name);
    if (header == null || header.isEmpty()) {
      return null;
    }
    return header.get(0);
  }

  public boolean headerEquals(String name, String value) {
    String headerVal = header(name);
    return headerVal == null && value == null || (headerVal != null && headerVal.equals(value));
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
    if (contentType == null) contentType = header("Content-type");
    return contentType;
  }

  @Nullable
  public String contentEncoding() {
    String contentEncoding = header("Content-Encoding");
    if (contentEncoding == null) contentEncoding = header("content-encoding");
    return contentEncoding;
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
      // This should never happen
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public Ason asAsonObject() throws BridgeException {
    if (asonObjCache != null) {
      return asonObjCache;
    }
    final String content = asString();
    if (content == null) return null;
    try {
      if (asonObjCache == null) {
        asonObjCache = new Ason(content);
      }
      return asonObjCache;
    } catch (JSONException e) {
      throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
    }
  }

  @Nullable
  public AsonArray<?> asAsonArray() throws BridgeException {
    final String content = asString();
    if (content == null) return null;
    try {
      if (asonArrayCache == null) {
        asonArrayCache = new AsonArray(content);
      }
      return asonArrayCache;
    } catch (JSONException e) {
      throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
    }
  }

  @Nullable
  public JSONObject asJsonObject() throws BridgeException {
    Ason ason = asAsonObject();
    if (ason == null) {
      return null;
    }
    return ason.toStockJson();
  }

  @Nullable
  public JSONArray asJsonArray() throws BridgeException {
    AsonArray ason = asAsonArray();
    if (ason == null) {
      return null;
    }
    return ason.toStockJson();
  }

  public void asFile(@NotNull File destination) throws BridgeException {
    final byte[] content = asBytes();
    if (content == null)
      throw new BridgeException(
          this,
          "No content was returned in this response.",
          BridgeException.REASON_RESPONSE_UNPARSEABLE);
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

  private void throwIfNoContentType(@Nullable String contentType) throws BridgeException {
    if (contentType == null) {
      String msg =
          String.format(
              Locale.getDefault(),
              "Response has no Content-Type, cannot determine appropriate response converter. Response status: %d.",
              code);
      for (String key : headers.keySet())
        msg += String.format(Locale.getDefault(), "\n    %s = %s", key, headers.get(key).get(0));
      throw new BridgeException(this, msg, BridgeException.REASON_RESPONSE_UNPARSEABLE);
    }
  }

  @Nullable
  public <T> T asClass(@NotNull Class<T> cls) throws BridgeException {
    String contentType = contentType();
    throwIfNoContentType(contentType);
    final long start = System.currentTimeMillis();
    T result;
    try {
      result = Bridge.config().converter(contentType).deserialize(this, cls);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize response to object!", e);
    }
    final long diff = System.currentTimeMillis() - start;
    LogCompat.d(
        this,
        "Response conversion to object %s took %d milliseconds (%d seconds).",
        cls.getName(),
        diff,
        diff / 1000);
    return result;
  }

  @Nullable
  public <T> T[] asClassArray(@NotNull Class<T> cls) throws BridgeException {
    String contentType = contentType();
    throwIfNoContentType(contentType);
    final long start = System.currentTimeMillis();
    T[] result;
    try {
      result = Bridge.config().converter(contentType).deserializeArray(this, cls);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize response to array!", e);
    }
    final long diff = System.currentTimeMillis() - start;
    LogCompat.d(
        this,
        "Response conversion to array of class %s took %d milliseconds (%d seconds).",
        cls.getName(),
        diff,
        diff / 1000);
    return result;
  }

  @Nullable
  public <T> List<T> asClassList(@NotNull Class<T> cls) throws BridgeException {
    String contentType = contentType();
    throwIfNoContentType(contentType);
    final long start = System.currentTimeMillis();
    List<T> result;
    try {
      result = Bridge.config().converter(contentType).deserializeList(this, cls);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize response to list!", e);
    }
    final long diff = System.currentTimeMillis() - start;
    LogCompat.d(
        this,
        "Response conversion to list of class %s took %d milliseconds (%d seconds).",
        cls.getName(),
        diff,
        diff / 1000);
    return result;
  }

  @Nullable
  @Override
  public String toString() {
    return String.format(Locale.US, "%d %s (%s)", code, message, url);
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
