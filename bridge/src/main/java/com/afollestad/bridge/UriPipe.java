package com.afollestad.bridge;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class UriPipe extends Pipe {

    private final Context mContext;
    private final Uri mUri;
    private InputStream mStream;

    protected UriPipe(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        try {
            byte[] buffer = new byte[Bridge.config().mBufferSize];
            int read;
            int totalRead = 0;
            final int totalAvailable = mStream.available();
            while ((read = mStream.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                if (progressCallback != null)
                    progressCallback.publishProgress(totalRead, totalAvailable);
            }
        } finally {
            BridgeUtil.closeQuietly(mStream);
        }
    }

    @Override
    public int contentLength() throws IOException {
        InputStream stream = getStream();
        if (stream == null) return -1;
        return mStream.available();
    }

    @Override
    @NonNull
    public String contentType() {
        String type;
        if (mUri.getScheme() == null || mUri.getScheme().equalsIgnoreCase("file")) {
            type = URLConnection.guessContentTypeFromName(new File(mUri.getPath()).getName());
        } else {
            type = mContext.getContentResolver().getType(mUri);
        }
        if (type == null || type.trim().isEmpty())
            type = "application/octet-stream";
        return type;
    }

    public InputStream getStream() throws IOException {
        if (mStream == null) {
            if (mUri.getScheme() == null || mUri.getScheme().equalsIgnoreCase("file"))
                mStream = new FileInputStream(mUri.getPath());
            else mStream = mContext.getContentResolver().openInputStream(mUri);
        }
        return mStream;
    }
}