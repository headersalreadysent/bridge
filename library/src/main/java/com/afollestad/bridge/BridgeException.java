package com.afollestad.bridge;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"}) public class BridgeException extends Exception {

    public static final int REASON_REQUEST_CANCELLED = 1;
    public static final int REASON_REQUEST_FAILED = 2;
    public static final int REASON_REQUEST_TIMEOUT = 3;

    public static final int REASON_RESPONSE_UNSUCCESSFUL = 4;
    public static final int REASON_RESPONSE_UNPARSEABLE = 5;
    public static final int REASON_RESPONSE_IOERROR = 6;

    public static final int REASON_RESPONSE_VALIDATOR_FALSE = 7;
    public static final int REASON_RESPONSE_VALIDATOR_ERROR = 8;

    @IntDef({REASON_REQUEST_CANCELLED, REASON_REQUEST_FAILED, REASON_RESPONSE_UNSUCCESSFUL,
            REASON_REQUEST_TIMEOUT, REASON_RESPONSE_UNPARSEABLE, REASON_RESPONSE_IOERROR,
            REASON_RESPONSE_VALIDATOR_FALSE, REASON_RESPONSE_VALIDATOR_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {
    }

    @Reason private int reason;
    @Nullable protected Request request;
    @Nullable private Response response;
    @Nullable private String validatorId;
    @Nullable private Exception underlyingException;

    // Request constructors

    protected BridgeException(@SuppressWarnings("NullableProblems") @NonNull Request request, Exception wrap) {
        super(wrap);
        if (wrap instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        this.request = request;
        if (wrap instanceof TimeoutException || wrap instanceof SocketTimeoutException)
            reason = REASON_REQUEST_TIMEOUT;
        else reason = REASON_REQUEST_FAILED;
    }

    protected BridgeException(@SuppressWarnings("NullableProblems") @NonNull Request cancelledRequest) {
        super("Request was cancelled.");
        request = cancelledRequest;
        reason = REASON_REQUEST_CANCELLED;
    }

    // Response constructors

    protected BridgeException(@Nullable Response response, @NonNull String message, @Reason int reason) {
        super(message);
        this.response = response;
        this.reason = reason;
    }

    protected BridgeException(@Nullable Request request, @NonNull String message, @Reason int reason) {
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

    protected BridgeException(@Nullable Response response, ResponseValidator validator, @NonNull Exception e) {
        super(e.getLocalizedMessage(), e);
        if (e instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        underlyingException = e;
        this.response = response;
        reason = REASON_RESPONSE_VALIDATOR_ERROR;
        validatorId = validator.id();
    }

    protected BridgeException(@NonNull Response response, @NonNull Exception e, @Reason int reason) {
        this(response, e, reason, false);
    }

    protected BridgeException(@NonNull Response response, @NonNull Exception e, @Reason int reason, boolean forceString) {
        super(String.format("%s: %s", forceString ? response.asString() : response.toString(), e.getLocalizedMessage()), e);
        if (e instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        underlyingException = e;
        this.response = response;
        this.reason = reason;
    }

    // Getters

    @Nullable public Request request() {
        return request;
    }

    @Nullable public Response response() {
        return response;
    }

    @Reason public int reason() {
        return reason;
    }

    @Nullable public Exception underlyingException() {
        return underlyingException;
    }

    @Nullable public String validatorId() {
        return validatorId;
    }
}
