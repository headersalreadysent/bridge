package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public interface LineCallback {

    void onLine(@NotNull String line);
}
