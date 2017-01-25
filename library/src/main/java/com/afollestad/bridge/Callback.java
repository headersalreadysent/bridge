package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public abstract class Callback {

    boolean isCancellable;
    Object tag;

    public abstract void response(@NonNull Request request,
                                  @Nullable Response response,
                                  @Nullable BridgeException e);

    public void progress(Request request, int current, int total, int percent) {
    }
}