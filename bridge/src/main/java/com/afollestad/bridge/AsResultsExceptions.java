package com.afollestad.bridge;

import android.graphics.Bitmap;
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

    byte[] asBytes() throws BridgeException;

    String asString() throws BridgeException;

    Spanned asHtml() throws BridgeException;

    Bitmap asBitmap() throws BridgeException;

    JSONObject asJsonObject() throws BridgeException;

    JSONArray asJsonArray() throws BridgeException;

    void asFile(File destination) throws BridgeException;
}
