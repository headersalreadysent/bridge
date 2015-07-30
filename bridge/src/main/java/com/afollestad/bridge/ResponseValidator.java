package com.afollestad.bridge;

import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface ResponseValidator {

    boolean validate(@NonNull Response response) throws Exception;

    @NonNull
    String id();
}