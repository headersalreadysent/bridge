package com.afollestad.bridge;

import java.lang.reflect.Method;

/**
 * On Android, wraps an android.os.Handler to post Runnables to UI threads.
 * In stock Java, Runnables just run directly.
 *
 * @author Aidan Follestad (afollestad)
 */
class HandlerCompat {

    private Object handlerObject;
    private Method postMethod;

    HandlerCompat() {
        try {
            Class<?> cls = Class.forName("android.os.Handler");
            if (cls != null) {
                handlerObject = BridgeUtil.newInstance(cls);
                postMethod = cls.getDeclaredMethod("post", Runnable.class);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean post(Runnable runnable) {
        if (handlerObject != null) {
            try {
                return (Boolean) postMethod.invoke(handlerObject, runnable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            runnable.run();
            return true;
        }
    }
}