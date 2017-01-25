package com.afollestad.bridge;

import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public interface LineCallback {

    void onLine(@NonNull String line);
}
