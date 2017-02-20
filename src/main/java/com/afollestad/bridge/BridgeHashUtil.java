package com.afollestad.bridge;

import java.security.MessageDigest;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public final class BridgeHashUtil {

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String hash(byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(data);
            return toHexString(digest);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static String hash(String str) {
        try {
            final byte[] bytes = str.getBytes("UTF-8");
            return hash(bytes);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}