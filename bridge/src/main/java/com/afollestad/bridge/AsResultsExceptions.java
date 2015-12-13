package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * A version of {@link AsResults} that throws exceptions for all methods. Used in {@link RequestBuilder}.
 *
 * @author Aidan Follestad (afollestad)
 */
interface AsResultsExceptions {

    @Nullable
    byte[] asBytes() throws BridgeException;

    @Nullable
    String asString() throws BridgeException;

    @Nullable
    Spanned asHtml() throws BridgeException;

    @Nullable
    Bitmap asBitmap() throws BridgeException;

    @Nullable
    JSONObject asJsonObject() throws BridgeException;

    @Nullable
    JSONArray asJsonArray() throws BridgeException;

    void asFile(File destination) throws BridgeException;

    @Nullable
    <T> T asClass(@NonNull Class<T> cls) throws BridgeException;

    <T> void asClass(@NonNull Class<T> cls, @NonNull ResponseConvertCallback<T> callback);

    @Nullable
    <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException;

    <T> void asClassArray(@NonNull Class<T> cls, @NonNull ResponseConvertCallback<T[]> callback);

    @Nullable
    Object asSuggested() throws BridgeException;
}
