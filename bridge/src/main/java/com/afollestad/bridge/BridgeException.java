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
public class BridgeException extends Exception {

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

    @Reason
    private int mReason;
    @Nullable
    protected Request mRequest;
    @Nullable
    private Response mResponse;
    @Nullable
    private String mValidatorId;

    // Request constructors

    protected BridgeException(@SuppressWarnings("NullableProblems") @NonNull Request request, Exception wrap) {
        super(wrap);
        if (wrap instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        mRequest = request;
        if (wrap instanceof TimeoutException || wrap instanceof SocketTimeoutException)
            mReason = REASON_REQUEST_TIMEOUT;
        else mReason = REASON_REQUEST_FAILED;
    }

    protected BridgeException(@SuppressWarnings("NullableProblems") @NonNull Request cancelledRequest) {
        super("Request was cancelled.");
        mRequest = cancelledRequest;
        mReason = REASON_REQUEST_CANCELLED;
    }

    // Response constructors

    protected BridgeException(@Nullable Response response, @NonNull String message, @Reason int reason) {
        super(message);
        mResponse = response;
        mReason = reason;
    }

    protected BridgeException(@Nullable Request request, @NonNull String message, @Reason int reason) {
        super(message);
        mRequest = request;
        mReason = reason;
    }

    protected BridgeException(@Nullable Response response, ResponseValidator validator) {
        super(String.format("Validation %s didn't pass.", validator.id()));
        mResponse = response;
        mReason = REASON_RESPONSE_VALIDATOR_FALSE;
        mValidatorId = validator.id();
    }

    protected BridgeException(@Nullable Response response, ResponseValidator validator, @NonNull Exception e) {
        super(e.getLocalizedMessage(), e);
        if (e instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        mResponse = response;
        mReason = REASON_RESPONSE_VALIDATOR_ERROR;
        mValidatorId = validator.id();
    }

    protected BridgeException(@NonNull Response response, @NonNull Exception e, @Reason int reason) {
        super(String.format("%s: %s", response.toString(), e.getLocalizedMessage()), e);
        if (e instanceof BridgeException)
            throw new IllegalArgumentException("BridgeException cannot wrap a BridgeException.");
        mResponse = response;
        mReason = reason;
    }

    // Getters

    @Nullable
    public Request request() {
        return mRequest;
    }

    @Nullable
    public Response response() {
        return mResponse;
    }

    @Reason
    public int reason() {
        return mReason;
    }

    @Nullable
    public String validatorId() {
        return mValidatorId;
    }
}
