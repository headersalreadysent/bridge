package com.afollestad.bridge;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Method {

    public final static int GET = 1;
    public final static int PUT = 2;
    public final static int POST = 3;
    public final static int DELETE = 4;

    public static String name(int methodValue) {
        switch (methodValue) {
            default:
                return "GET";
            case 2:
                return "PUT";
            case 3:
                return "POST";
            case 4:
                return "DELETE";
        }
    }
}