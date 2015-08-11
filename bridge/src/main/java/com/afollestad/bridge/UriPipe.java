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
class UriPipe extends Pipe {

    private final Context mContext;
    private final Uri mUri;

    public UriPipe(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        InputStream is = null;
        try {
            if (mUri.getScheme() == null || mUri.getScheme().equalsIgnoreCase("file"))
                is = new FileInputStream(mUri.getPath());
            else is = mContext.getContentResolver().openInputStream(mUri);
            byte[] buffer = new byte[Bridge.client().config().mBufferSize];
            int read;
            int totalRead = 0;
            final int totalAvailable = is.available();
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                if (progressCallback != null)
                    progressCallback.publishProgress(totalRead, totalAvailable);
            }
        } finally {
            BridgeUtil.closeQuietly(is);
        }
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
}
