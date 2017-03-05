package com.afollestad.bridge;

import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("WeakerAccess")
public interface RetryCallback {

  /** Return false to cancel retrying. */
  boolean onWillRetry(
      @Nullable Response previousResponse, BridgeException problem, RequestBuilder newRequest);
}
