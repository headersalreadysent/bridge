package com.afollestad.bridge;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public abstract class InfoCallback implements Serializable {

    public abstract void onConnected(Request request);

    @SuppressWarnings("unused") public void onRequestSent(Request request) {
    }
}
