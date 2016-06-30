package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface ResponseConvertCallback<T> {

    void onResponse(@Nullable Response response, @Nullable T object, @Nullable BridgeException e);
}