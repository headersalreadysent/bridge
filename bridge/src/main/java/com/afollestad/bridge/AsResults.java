package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * @author Aidan Follestad (afollestad)
 */
interface AsResults {

    @Nullable
    byte[] asBytes();

    @Nullable
    String asString();

    @Nullable
    Spanned asHtml();

    @Nullable
    Bitmap asBitmap();

    @Nullable
    JSONObject asJsonObject() throws BridgeException;

    @Nullable
    JSONArray asJsonArray() throws BridgeException;

    void asFile(File destination) throws BridgeException;

    @Nullable
    <T> T asClass(@NonNull Class<T> cls);

    @Nullable
    <T> T[] asClassArray(@NonNull Class<T> cls);

    @Nullable
    Object asSuggested() throws BridgeException;
}
