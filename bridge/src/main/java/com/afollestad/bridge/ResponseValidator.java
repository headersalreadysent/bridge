package com.afollestad.bridge;

import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class ResponseValidator {

    public abstract boolean validate(@NonNull Response response) throws Exception;

    @NonNull
    public abstract String id();

    @Override
    public String toString() {
        return "Validator: " + id();
    }
}