package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * @author Aidan Follestad (afollestad)
 */
interface AsResults {

    byte[] asBytes();

    String asString();

    Spanned asHtml();

    Bitmap asBitmap();

    JSONObject asJsonObject() throws BridgeException;

    JSONArray asJsonArray() throws BridgeException;

    void asFile(File destination) throws BridgeException;
}
