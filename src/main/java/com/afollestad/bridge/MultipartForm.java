package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MultipartForm {

  final String BOUNDARY;
  private final byte[] LINE_FEED = "\r\n".getBytes();
  private ByteArrayOutputStream byteArrayOutputStream;
  private int count;
  private String encoding;

  public MultipartForm() {
    BOUNDARY = String.format(Locale.US, "------%d------", System.currentTimeMillis());
    byteArrayOutputStream = new ByteArrayOutputStream();
    encoding = "UTF-8";
  }

  public MultipartForm(@NotNull String encoding) {
    BOUNDARY = String.format(Locale.US, "------%d------", System.currentTimeMillis());
    byteArrayOutputStream = new ByteArrayOutputStream();
    this.encoding = encoding;
  }

  public MultipartForm add(@NotNull String fieldName, @NotNull final File file) throws IOException {
    add(fieldName, file.getName(), Pipe.forFile(file));
    return this;
  }

  public MultipartForm add(@NotNull String fieldName, @NotNull String fileName, @NotNull Pipe pipe)
      throws IOException {
    if (byteArrayOutputStream == null)
      throw new IllegalStateException("This MultipartForm is already consumed.");
    if (count > 0) byteArrayOutputStream.write(LINE_FEED);
    byteArrayOutputStream.write(("--" + BOUNDARY).getBytes());
    byteArrayOutputStream.write(LINE_FEED);
    byteArrayOutputStream.write(
        ("Content-Disposition: form-data; name=\""
                + fieldName
                + "\"; filename=\""
                + fileName
                + "\"")
            .getBytes());
    byteArrayOutputStream.write(LINE_FEED);
    byteArrayOutputStream.write(("Content-Type: " + pipe.contentType()).getBytes());
    byteArrayOutputStream.write(LINE_FEED);
    byteArrayOutputStream.write("Content-Transfer-Encoding: binary".getBytes());
    byteArrayOutputStream.write(LINE_FEED);
    byteArrayOutputStream.write(LINE_FEED);
    pipe.writeTo(byteArrayOutputStream, null);
    count++;
    return this;
  }

  public MultipartForm add(@NotNull String fieldName, @NotNull Object value) {
    if (byteArrayOutputStream == null)
      throw new IllegalStateException("This MultipartForm is already consumed.");
    try {
      if (count > 0) byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write(("--" + BOUNDARY).getBytes());
      byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write(
          String.format("Content-Disposition: form-data; name=\"%s\"", fieldName).getBytes());
      byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write(("Content-Type: text/plain; charset=" + encoding).getBytes());
      byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write((value + "").getBytes(encoding));
    } catch (Exception e) {
      // Shouldn't happen
      throw new RuntimeException(e);
    }
    count++;
    return this;
  }

  byte[] data() {
    try {
      byteArrayOutputStream.write(LINE_FEED);
      byteArrayOutputStream.write(String.format("--%s--", BOUNDARY).getBytes());
      byteArrayOutputStream.write(LINE_FEED);
    } catch (Exception e) {
      // Shouldn't happen
      throw new RuntimeException(e);
    }
    final byte[] data = byteArrayOutputStream.toByteArray();
    BridgeUtil.closeQuietly(byteArrayOutputStream);
    byteArrayOutputStream = null;
    return data;
  }
}
