package com.afollestad.bridge;

import android.support.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Method {

    private String mName;

    private Method(@NonNull String name) {
        mName = name;
    }

    public final static Method GET = new Method("GET");
    public final static Method PUT = new Method("PUT");
    public final static Method POST = new Method("POST");
    public final static Method DELETE = new Method("DELETE");

    public String name() {
        return mName;
    }

    @Override
    public String toString() {
        return name();
    }
}