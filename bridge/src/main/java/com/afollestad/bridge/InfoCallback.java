package com.afollestad.bridge;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class InfoCallback {

    public abstract void onConnected(Request request);

    public void onRequestSent(Request request) {
    }
}
