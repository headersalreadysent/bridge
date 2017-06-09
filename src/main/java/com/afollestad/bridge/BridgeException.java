package com.afollestad.bridge;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BridgeException extends Exception {

  public static final int REASON_REQUEST_CANCELLED = 1;
  public static final int REASON_REQUEST_FAILED = 2;
  public static final int REASON_REQUEST_TIMEOUT = 3;
  public static final int REASON_REQUEST_MAX_RETRIES = 4;

  public static final int REASON_RESPONSE_UNSUCCESSFUL = 5;
  public static final int REASON_RESPONSE_UNPARSEABLE = 6;
  public static final int REASON_RESPONSE_IOERROR = 7;

  public static final int REASON_RESPONSE_VALIDATOR_FALSE = 8;
  public static final int REASON_RESPONSE_VALIDATOR_ERROR = 9;
  @Nullable protected Request request;
  private int reason;
  @Nullable private Response response;
  @Nullable private String validatorId;
  @Nullable private Exception underlyingException;

  // Request constructors

  protected BridgeException(
      @SuppressWarnings("NullableProblems") @NotNull Request request, Exception wrap) {
    super(wrap);
    if (wrap instanceof BridgeException)
      throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
    this.request = request;
    if (wrap instanceof TimeoutException || wrap instanceof SocketTimeoutException)
      reason = REASON_REQUEST_TIMEOUT;
    else reason = REASON_REQUEST_FAILED;
  }

  protected BridgeException(
      @SuppressWarnings("NullableProblems") @NotNull Request cancelledRequest) {
    super("Request was cancelled.");
    request = cancelledRequest;
    reason = REASON_REQUEST_CANCELLED;
  }

  // Response constructors

  protected BridgeException(@Nullable Response response, @NotNull String message, int reason) {
    super(message);
    this.response = response;
    this.reason = reason;
  }

  protected BridgeException(@Nullable Request request, @NotNull String message, int reason) {
    super(message);
    this.request = request;
    this.reason = reason;
  }

  protected BridgeException(@Nullable Response response, ResponseValidator validator) {
    super(String.format("Validation %s didn't pass.", validator.id()));
    this.response = response;
    reason = REASON_RESPONSE_VALIDATOR_FALSE;
    validatorId = validator.id();
  }

  protected BridgeException(
      @Nullable Response response, ResponseValidator validator, @NotNull Exception e) {
    super(e.getLocalizedMessage(), e);
    if (e instanceof BridgeException)
      throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
    underlyingException = e;
    this.response = response;
    reason = REASON_RESPONSE_VALIDATOR_ERROR;
    validatorId = validator.id();
  }

  protected BridgeException(@NotNull Response response, @NotNull Exception e, int reason) {
    this(response, e, reason, false);
  }

  protected BridgeException(
      @NotNull Response response, @NotNull Exception e, int reason, boolean forceString) {
    super(
        String.format(
            "%s: %s",
            forceString ? response.asString() : response.toString(), e.getLocalizedMessage()),
        e);
    if (e instanceof BridgeException)
      throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
    underlyingException = e;
    this.response = response;
    this.reason = reason;
  }

  // Getters

  @Nullable
  public Request request() {
    return request;
  }

  @Nullable
  public Response response() {
    return response;
  }

  public int reason() {
    return reason;
  }

  @Nullable
  public Exception underlyingException() {
    return underlyingException;
  }

  @Nullable
  public String validatorId() {
    return validatorId;
  }
}
