package com.afollestad.bridge;

import java.io.Serializable;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class ProgressCallback implements Serializable {

    protected Request mRequest;
    protected int mLastPercent = -1;

    public abstract void progress(Request request, int current, int total, int percent);

    public final void publishProgress(int current, int total) {
        final int percent = (int) (((double) current / (double) total) * 100d);
        if (percent != mLastPercent) {
            progress(mRequest, current, total, percent);
            mLastPercent = percent;
        }
    }
}
