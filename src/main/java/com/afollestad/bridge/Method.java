package com.afollestad.bridge;

/**
 * @author Aidan Follestad (afollestad)
 */
final class Method {

    final static int UNSPECIFIED = -1;
    final static int GET = 1;
    final static int PUT = 2;
    final static int POST = 3;
    final static int DELETE = 4;

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