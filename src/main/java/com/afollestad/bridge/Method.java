package com.afollestad.bridge;

/** @author Aidan Follestad (afollestad) */
final class Method {

  static final int UNSPECIFIED = -1;
  static final int GET = 1;
  static final int PUT = 2;
  static final int POST = 3;
  static final int DELETE = 4;

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
