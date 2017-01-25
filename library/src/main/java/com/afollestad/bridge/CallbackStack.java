package com.afollestad.bridge;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
final class CallbackStack {

    @SuppressLint("DefaultLocale")
    static String createKey(Request req) {
        String key = String.format("%d\0%s\0%s", req.method(), req.url().replace("http://", "").replace("https://", ""),
                req.builder().mBody != null ? req.builder().mBody.length + "" : "");
        if (req.builder().mMethod == Method.POST ||
                req.builder().mMethod == Method.PUT) {
            final RequestBuilder builder = req.builder();
            String hash = null;
            if (builder.mPipe != null) {
                hash = builder.mPipe.hash();
            } else if (builder.mBody != null) {
                hash = HashUtil.hash(builder.mBody);
            }
            key += String.format("\0%s\0", hash);
        }
        return key;
    }

    private final Object LOCK = new Object();
    private List<Callback> callbacks;
    private Request driverRequest;
    private int percent = -1;
    private Handler handler;

    CallbackStack() {
        callbacks = new ArrayList<>();
        handler = new Handler();
    }

    public int size() {
        synchronized (LOCK) {
            if (callbacks == null) return -1;
            return callbacks.size();
        }
    }

    void push(Callback callback, Request request) {
        synchronized (LOCK) {
            if (callbacks == null)
                throw new IllegalStateException("This stack has already been fired or cancelled.");
            callback.isCancellable = request.isCancellable();
            callback.tag = request.builder().mTag;
            callbacks.add(callback);
            if (driverRequest == null)
                driverRequest = request;
        }
    }

    void fireAll(final Response response, final BridgeException error) {
        synchronized (LOCK) {
            if (callbacks == null)
                throw new IllegalStateException("This stack has already been fired.");
            for (final Callback cb : callbacks) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.response(driverRequest, response, error);
                    }
                });
            }
            callbacks.clear();
            callbacks = null;
        }
    }

    void fireAllProgress(final Request request, final int current, final int total) {
        synchronized (LOCK) {
            if (callbacks == null)
                throw new IllegalStateException("This stack has already been fired.");
            int newPercent = (int) (((float) current / (float) total) * 100f);
            if (newPercent != percent) {
                percent = newPercent;
                synchronized (LOCK) {
                    for (final Callback cb : callbacks) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                cb.progress(request, current, total, percent);
                            }
                        });
                    }
                }
            }
        }
    }

    boolean cancelAll(Object tag, boolean force) {
        synchronized (LOCK) {
            if (callbacks == null)
                throw new IllegalStateException("This stack has already been cancelled.");
            final Iterator<Callback> callbackIterator = callbacks.iterator();
            while (callbackIterator.hasNext()) {
                final Callback callback = callbackIterator.next();
                if (tag != null && !tag.equals(callback.tag))
                    continue;
                if (callback.isCancellable || force) {
                    callbackIterator.remove();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.response(driverRequest, null, new BridgeException(driverRequest));
                        }
                    });
                }
            }
            if (callbacks.size() == 0) {
                driverRequest.cancelCallbackFired = true;
                driverRequest.cancel(force);
                callbacks = null;
                return true;
            } else {
                return false;
            }
        }
    }
}