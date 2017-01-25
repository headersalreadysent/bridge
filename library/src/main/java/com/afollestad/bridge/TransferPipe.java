package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("WeakerAccess") public final class TransferPipe extends Pipe {

    private final InputStream inputStream;
    private final String contentType;
    private String hash;

    TransferPipe(@NonNull InputStream is, @NonNull String contentType, @NonNull String hash) {
        inputStream = is;
        this.contentType = contentType;
        this.hash = hash;
    }

    @Override
    public String hash() {
        return hash;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        byte[] buffer = new byte[Bridge.config().bufferSize];
        int read;
        int totalRead = 0;
        final int available = inputStream.available();
        while ((read = inputStream.read(buffer)) != -1) {
            os.write(buffer, 0, read);
            totalRead += read;
            if (progressCallback != null)
                progressCallback.publishProgress(totalRead, available);
        }
    }

    @Override
    @NonNull
    public String contentType() {
        return contentType;
    }

    @Override
    public int contentLength() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() {
        BridgeUtil.closeQuietly(inputStream);
    }
}