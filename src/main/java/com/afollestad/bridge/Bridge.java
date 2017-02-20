package com.afollestad.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"}) public class Bridge implements Serializable {

    private static Bridge bridge;
    private static Config config;

    private static final Object LOCK = new Object();
    HashMap<String, CallbackStack> requestMap;

    boolean pushCallback(Request request, Callback callback) {
        synchronized (LOCK) {
            if (requestMap == null)
                requestMap = new HashMap<>();
            final String key = CallbackStack.createKey(request);
            CallbackStack cbs = requestMap.get(key);
            if (cbs != null) {
                LogCompat.d(this, "Pushing callback to EXISTING stack for %s", key);
                cbs.push(callback, request);
                return false;
            } else {
                LogCompat.d(this, "Pushing callback to NEW stack for %s", key);
                cbs = new CallbackStack();
                cbs.push(callback, request);
                requestMap.put(key, cbs);
                return true;
            }
        }
    }

    void fireProgress(Request request, int current, int total) {
        synchronized (LOCK) {
            if (requestMap == null) return;
            final String key = CallbackStack.createKey(request);
            final CallbackStack cbs = requestMap.get(key);
            if (cbs != null)
                cbs.fireAllProgress(request, current, total);
        }
    }

    void fireCallbacks(final Request request, final Response response, final BridgeException error) {
        synchronized (LOCK) {
            final String key = CallbackStack.createKey(request);
            LogCompat.d(this, "Attempting to fire callbacks for %s", key);
            if (requestMap == null) {
                LogCompat.d(this, "Request map is null, can't fire callbacks.");
                return;
            }
            final CallbackStack cbs = requestMap.get(key);
            if (cbs != null) {
                LogCompat.d(this, "Firing %d callback(s) for %s", cbs.size(), key);
                cbs.fireAll(response, error);
                requestMap.remove(key);
                if (requestMap.size() == 0)
                    requestMap = null;
            } else {
                LogCompat.d(this, "No callback stack found for %s", key);
            }
        }
    }

    private Bridge() {
        config = new Config();
    }

    @NotNull private static Bridge client() {
        if (bridge == null)
            bridge = new Bridge();
        return bridge;
    }

    @NotNull public static Config config() {
        if (bridge == null)
            bridge = new Bridge();
        if (config == null)
            config = new Config();
        return config;
    }

    private static String processUrl(String url, @Nullable Object... formatArgs) {
        if (formatArgs != null && formatArgs.length > 0) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (formatArgs[i] instanceof String) {
                    try {
                        formatArgs[i] = URLEncoder.encode((String) formatArgs[i], "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // this should never happen
                        throw new RuntimeException(e);
                    }
                }
            }
            return String.format(url, formatArgs);
        } else return url;
    }

    public static RequestBuilder get(@NotNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.GET, client());
    }

    public static RequestBuilder post(@NotNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.POST, client());
    }

    public static RequestBuilder put(@NotNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.PUT, client());
    }

    public static RequestBuilder delete(@NotNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.DELETE, client());
    }

    public static CancelCriteria cancelAll() {
        return new CancelCriteria(client(), LOCK);
    }

    public static void destroy() {
        if (bridge != null) {
            if (config != null) {
                config.destroy();
                config = null;
            }
            cancelAll().commit();
            LogCompat.d(bridge, "Bridge singleton was destroyed.");
        }
    }
}