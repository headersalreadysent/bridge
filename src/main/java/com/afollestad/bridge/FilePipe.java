package com.afollestad.bridge;

import java.io.*;
import java.net.URLConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public final class FilePipe extends Pipe {

  private String filePath;
  private InputStream inputStream;
  private String hash;

  FilePipe(String filePath) {
    this.filePath = filePath;
  }

  FilePipe(File file) {
    this(file.getAbsolutePath());
  }

  @Override
  public String hash() {
    if (hash == null) {
      try {
        getStream();
        hash = BridgeHashUtil.hash(filePath + "/" + inputStream.available());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return hash;
  }

  @Override
  public void writeTo(@NotNull OutputStream os, @Nullable ProgressCallback progressCallback)
      throws IOException {
    try {
      byte[] buffer = new byte[Bridge.config().bufferSize];
      int read;
      int totalRead = 0;
      getStream();
      final int totalAvailable = inputStream.available();
      while ((read = inputStream.read(buffer)) != -1) {
        os.write(buffer, 0, read);
        totalRead += read;
        if (progressCallback != null) progressCallback.publishProgress(totalRead, totalAvailable);
      }
    } finally {
      BridgeUtil.closeQuietly(inputStream);
    }
  }

  @Override
  public int contentLength() throws IOException {
    InputStream stream = getStream();
    if (stream == null) {
      return -1;
    }
    return inputStream.available();
  }

  @Override
  @NotNull
  public String contentType() {
    String type = URLConnection.guessContentTypeFromName(new File(filePath).getName());
    if (type == null || type.trim().isEmpty()) {
      type = "application/octet-stream";
    }
    return type;
  }

  public InputStream getStream() throws IOException {
    if (inputStream == null) {
      inputStream = new FileInputStream(filePath);
    }
    return inputStream;
  }

  @Override
  public void close() {
    BridgeUtil.closeQuietly(inputStream);
  }
}
