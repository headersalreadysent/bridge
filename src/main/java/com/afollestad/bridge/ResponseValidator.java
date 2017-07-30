package com.afollestad.bridge;

import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public abstract class ResponseValidator implements Serializable {

  public abstract boolean validate(@NotNull Response response) throws Exception;

  @NotNull
  public abstract String id();

  @Override
  public String toString() {
    return "Validator: " + id();
  }
}
