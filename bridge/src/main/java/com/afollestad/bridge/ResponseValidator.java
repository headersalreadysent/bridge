package com.afollestad.bridge;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class ResponseValidator implements Serializable {

    public abstract boolean validate(@NonNull Response response) throws Exception;

    @NonNull
    public abstract String id();

    @Override
    public String toString() {
        return "Validator: " + id();
    }
}