package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public abstract class ResponseValidator implements Serializable {

    public abstract boolean validate(@NotNull Response response) throws Exception;

    @NotNull public abstract String id();

    @Override
    public String toString() {
        return "Validator: " + id();
    }
}