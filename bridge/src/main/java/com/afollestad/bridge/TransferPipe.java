package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Aidan Follestad (afollestad)
 */
class TransferPipe extends Pipe {

    private final InputStream mIs;
    private final String mContentType;

    public TransferPipe(@NonNull InputStream is, @NonNull String contentType) {
        mIs = is;
        mContentType = contentType;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        try {
            byte[] buffer = new byte[Bridge.client().config().mBufferSize];
            int read;
            int totalRead = 0;
            while ((read = mIs.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                if (progressCallback != null)
                    progressCallback.publishProgress(totalRead, mIs.available());
            }
        } finally {
            BridgeUtil.closeQuietly(mIs);
        }
    }

    @Override
    @NonNull
    public String contentType() {
        return mContentType;
    }

    @Override
    public int contentLength() throws IOException {
        if (mIs == null)
            return -1;
        return mIs.available();
    }
}
