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
@SuppressWarnings("WeakerAccess") public final class UriPipe extends Pipe {

    private final Context context;
    private final Uri uri;
    private InputStream inputStream;
    private String hash;

    UriPipe(Context context, Uri uri) {
        this.context = context;
        this.uri = uri;
    }

    @Override
    public String hash() {
        if (hash == null) {
            try {
                getStream();
                hash = HashUtil.hash(uri.toString() + "/" + inputStream.available());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return hash;
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressCallback) throws IOException {
        try {
            byte[] buffer = new byte[Bridge.config().bufferSize];
            int read;
            int totalRead = 0;
            getStream();
            final int totalAvailable = inputStream.available();
            while ((read = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalRead += read;
                Log2.d(this, "Wrote %d bytes into the destination stream (%d/%d).", read, totalRead, totalAvailable);
                if (progressCallback != null)
                    progressCallback.publishProgress(totalRead, totalAvailable);
            }
        } finally {
            BridgeUtil.closeQuietly(inputStream);
        }
    }

    @Override
    public int contentLength() throws IOException {
        InputStream stream = getStream();
        if (stream == null) return -1;
        return inputStream.available();
    }

    @Override
    @NonNull
    public String contentType() {
        String type;
        if (uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("file")) {
            type = URLConnection.guessContentTypeFromName(new File(uri.getPath()).getName());
        } else {
            type = context.getContentResolver().getType(uri);
        }
        if (type == null || type.trim().isEmpty())
            type = "application/octet-stream";
        return type;
    }

    public InputStream getStream() throws IOException {
        if (inputStream == null) {
            if (uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("file"))
                inputStream = new FileInputStream(uri.getPath());
            else inputStream = context.getContentResolver().openInputStream(uri);
        }
        return inputStream;
    }

    @Override
    public void close() {
        BridgeUtil.closeQuietly(inputStream);
    }
}