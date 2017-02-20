package com.afollestad.bridge;

import org.jetbrains.annotations.Nullable;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public interface ResponseConvertCallback<T> {

    void onResponse(
            @Nullable Response response,
            @Nullable T object,
            @Nullable BridgeException e);
}