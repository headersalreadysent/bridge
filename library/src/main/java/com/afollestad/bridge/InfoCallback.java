package com.afollestad.bridge;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class InfoCallback implements Serializable {

    public abstract void onConnected(Request request);

    public void onRequestSent(Request request) {
    }
}
