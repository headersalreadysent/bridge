package com.afollestad.bridge;

import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Util {

    public static void closeQuietly(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static byte[] readEntireStream(@Nullable InputStream is) throws IOException {
        if (is == null) return null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            byte[] buffer = new byte[Bridge.client().config().mBufferSize];
            int read;
            while ((read = is.read(buffer)) != -1)
                os.write(buffer, 0, read);
            os.flush();
            return os.toByteArray();
        } finally {
            Util.closeQuietly(os);
            Util.closeQuietly(is);
        }
    }

    protected static void throwIfNotSuccess(Response response) throws BridgeException {
        if (!response.isSuccess())
            throw new BridgeException(response,
                    String.format("Response was unsuccessful: %s %s", response.code(), response.phrase()),
                    BridgeException.REASON_RESPONSE_UNSUCCESSFUL);
    }

    private Util() {
    }
}
