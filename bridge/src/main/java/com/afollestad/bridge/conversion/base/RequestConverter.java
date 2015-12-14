package com.afollestad.bridge.conversion.base;

import android.support.annotation.NonNull;

import com.afollestad.bridge.RequestBuilder;

import java.lang.reflect.Field;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class RequestConverter extends Converter {

    public abstract void onPrepare(@NonNull RequestBuilder request, @NonNull Object object, @NonNull Class<?> type);

    public abstract boolean canConvertField(@NonNull Field field);

    public abstract void onConvertField(@NonNull Object object, @NonNull Field field, @NonNull RequestBuilder request);

    public abstract void onFinish(@NonNull RequestBuilder request);
}