package com.afollestad.bridge;

import java.lang.reflect.Method;

/**
 * On Android, wraps an android.os.Handler to post Runnables to UI threads.
 * In stock Java, Runnables just run directly.
 *
 * @author Aidan Follestad (afollestad)
 */
class LogCompat {

    private static boolean didInit;
    private static Method dMethod;
    private static Method eMethod;

    private static void init() {
        if (didInit) return;
        didInit = true;
        try {
            Class<?> cls = Class.forName("android.util.Log");
            if (cls != null) {
                dMethod = cls.getMethod("d", String.class, String.class);
                eMethod = cls.getMethod("e", String.class, String.class);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getTag(Object context) {
        if (context instanceof String) return (String) context;
        final Class cls;
        if (context instanceof Class) cls = (Class) context;
        else cls = context.getClass();
        return cls.getSimpleName();
    }

    private static String getMessage(String message, Object... formatArgs) {
        if (formatArgs == null || formatArgs.length == 0)
            return message;
        else return String.format(message, formatArgs);
    }

    static void d(Object tag, String message) {
        if (!Bridge.config().logging) return;
        init();
        tag = getTag(tag);
        if (eMethod == null) {
            System.out.println("debug/[" + tag + "]: " + message);
            return;
        }
        try {
            dMethod.invoke(null, tag, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void d(Object tag, String message, Object... args) {
        d(tag, getMessage(message, args));
    }

    static void e(Object tag, String message) {
        if (!Bridge.config().logging) return;
        init();
        tag = getTag(tag);
        if (eMethod == null) {
            System.out.println("error/[" + tag + "]: " + message);
            return;
        }
        try {
            eMethod.invoke(null, tag, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void e(Object tag, String message, Object... args) {
        e(tag, getMessage(message, args));
    }
}