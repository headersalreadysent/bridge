package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class TransferPipe extends Pipe {

    private final InputStream mIs;
    private final String mContentType;
    private String mHash;

    protected TransferPipe(@NonNull InputStream is, @NonNull String contentType, @NonNull String hash) {
        mIs = is;
        mContentType = contentType;
        mHash = hash;
    }

    @Override
    public String hash() {
        return mHash;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        byte[] buffer = new byte[Bridge.config().mBufferSize];
        int read;
        int totalRead = 0;
        final int available = mIs.available();
        while ((read = mIs.read(buffer)) != -1) {
            os.write(buffer, 0, read);
            totalRead += read;
            if (progressCallback != null)
                progressCallback.publishProgress(totalRead, available);
        }
    }

    @Override
    @NonNull
    public String contentType() {
        return mContentType;
    }

    @Override
    public int contentLength() throws IOException {

        return mIs.available();
    }

    @Override
    public void close() {
        BridgeUtil.closeQuietly(mIs);
    }
}