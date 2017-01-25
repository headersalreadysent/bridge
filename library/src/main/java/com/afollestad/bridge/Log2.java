package com.afollestad.bridge;

/**
 * Used for "detailed request logging", seperate from "basic" logging.
 *
 * @author Aidan Follestad (afollestad)
 */
final class Log2 {

    private static String getTag(Object context) {
        if (context instanceof String) return (String) context;
        final Class cls;
        if (context instanceof Class) cls = (Class) context;
        else cls = context.getClass();
        return String.format("DETAILED-%s", cls.getSimpleName());
    }

    private static String getMessage(String message, Object... formatArgs) {
        if (formatArgs == null || formatArgs.length == 0)
            return message;
        else return String.format(message, formatArgs);
    }

    public static void d(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.d(getTag(context), getMessage(message, formatArgs));
    }

    public static void v(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.v(getTag(context), getMessage(message, formatArgs));
    }

    public static void e(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.e(getTag(context), getMessage(message, formatArgs));
    }

    public static void i(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.i(getTag(context), getMessage(message, formatArgs));
    }

    public static void w(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.w(getTag(context), getMessage(message, formatArgs));
    }

    public static void wtf(Object context, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.wtf(getTag(context), getMessage(message, formatArgs));
    }

    public static void println(Object context, int priority, String message, Object... formatArgs) {
        if (!Bridge.config().detailedRequestLogging) return;
        android.util.Log.println(priority, getTag(context), getMessage(message, formatArgs));
    }

    private Log2() {
    }
}
