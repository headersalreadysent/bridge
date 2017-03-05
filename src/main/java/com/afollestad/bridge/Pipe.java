package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Pipe implements Serializable, Closeable {

  public Pipe() {}

  /** Creates a Pipe that reads the contents of a File into the Pipe. */
  public static Pipe forFile(@NotNull File file) {
    return new FilePipe(file);
  }

  /** Creates a Pipe that reads the contents of a File into the Pipe. */
  public static Pipe forFile(@NotNull String path) {
    return new FilePipe(path);
  }

  /** Creates a Pipe that reads an InputStream and transfers the content into the Pipe. */
  public static Pipe forStream(
      @NotNull InputStream is, @NotNull String contentType, @NotNull String hash) {
    return new TransferPipe(is, contentType, hash);
  }

  public abstract String hash();

  public abstract void writeTo(
      @NotNull OutputStream os, @Nullable ProgressCallback progressListener) throws IOException;

  @NotNull
  public abstract String contentType();

  public abstract int contentLength() throws IOException;

  public abstract void close();
}
