package com.afollestad.bridge;

import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.regex.Pattern;

/**
 * @author Aidan Follestad (afollestad)
 */
public class CancelCriteria {

    private Bridge mClient;
    private final Object LOCK;
    private final Handler mHandler;

    @Request.MethodInt
    private int mMethod = Method.UNSPECIFIED;
    private String mUrlRegex = null;
    private Object mTag = null;
    private boolean mForce = false;

    protected CancelCriteria(Bridge client, Object lock) {
        mClient = client;
        LOCK = lock;
        mHandler = new Handler();
    }

    private boolean passesMethod(Map.Entry<String, CallbackStack> entry) {
        if (mMethod == Method.UNSPECIFIED) return true;
        final String[] splitKey = entry.getKey().split("\0");
        final int keyMethod = Integer.parseInt(splitKey[0]);
        //noinspection ResourceType
        return keyMethod == mMethod;
    }

    private boolean passesUrlRegex(Map.Entry<String, CallbackStack> entry) {
        if (mUrlRegex == null) return true;
        final Pattern pattern = Pattern.compile(mUrlRegex);
        final String[] splitKey = entry.getKey().split("\0");
        final String keyUrl = splitKey[1];
        return pattern.matcher(keyUrl).find();
    }

    public CancelCriteria method(@Request.MethodInt int method) {
        mMethod = method;
        return this;
    }

    public CancelCriteria url(@Nullable String urlRegex) {
        mUrlRegex = urlRegex;
        return this;
    }

    public CancelCriteria tag(@Nullable String tag) {
        mTag = tag;
        return this;
    }

    public CancelCriteria force() {
        mForce = true;
        return this;
    }

    public int commit() {
        synchronized (LOCK) {
            if (mClient.mRequestMap == null) return 0;
            int cancelledCount = 0;
            final Iterator<Map.Entry<String, CallbackStack>> iter = mClient.mRequestMap.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<String, CallbackStack> entry = iter.next();
                if (passesMethod(entry) && passesUrlRegex(entry)) {
                    if (mClient.mRequestMap.get(entry.getKey()).cancelAll(mTag, mForce)) {
                        iter.remove();
                        cancelledCount++;
                    }
                }
            }
            if (mClient.mRequestMap.size() == 0)
                mClient.mRequestMap = null;
            return cancelledCount;
        }
    }

    @Deprecated
    public void allAsync(@Nullable final CancelCallback callback) {
        commitAsync(callback);
    }

    public void commitAsync(@Nullable final CancelCallback callback) {
        new Thread(new TimerTask() {
            @Override
            public void run() {
                final int count = commit();
                if (callback != null) {
                    mHandler.post(new TimerTask() {
                        @Override
                        public void run() {
                            callback.onRequestsCancelled(count);
                        }
                    });
                }
            }
        }).start();
    }

    public interface CancelCallback {
        void onRequestsCancelled(int count);
    }
}