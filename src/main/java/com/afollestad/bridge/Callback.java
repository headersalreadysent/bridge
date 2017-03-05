package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public abstract class Callback {

  boolean isCancellable;
  Object tag;

  public abstract void response(
      @NotNull Request request, @Nullable Response response, @Nullable BridgeException e);

  @SuppressWarnings("unused")
  public void progress(Request request, int current, int total, int percent) {}
}
