package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.conversion.JsonRequestConverter;
import com.afollestad.bridge.conversion.JsonResponseConverter;
import com.afollestad.bridge.conversion.base.RequestConverter;
import com.afollestad.bridge.conversion.base.ResponseConverter;

import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Config {

    protected Config() {
        mDefaultHeaders = new HashMap<>();
        mDefaultHeaders.put("User-Agent", "afollestad/Bridge");
        mDefaultHeaders.put("Content-Type", "text/plain");

        mRequestConverters = new HashMap<>();
        mRequestConverters.put("application/json", JsonRequestConverter.class);
        mRequestConverters.put("text/plain", JsonRequestConverter.class);

        mResponseConverters = new HashMap<>();
        mResponseConverters.put("application/json", JsonResponseConverter.class);
        mResponseConverters.put("text/plain", JsonResponseConverter.class);
    }

    protected String mHost;
    protected HashMap<String, Object> mDefaultHeaders;
    protected int mConnectTimeout = 10000;
    protected int mReadTimeout = 15000;
    protected int mBufferSize = 1024 * 4;
    protected boolean mLogging = false;
    protected boolean mDetailedRequestLogging = false;
    protected ResponseValidator[] mValidators;
    protected HashMap<String, Class<? extends RequestConverter>> mRequestConverters;
    protected HashMap<String, Class<? extends ResponseConverter>> mResponseConverters;
    protected boolean mAutoFollowRedirects = true;

    public Config host(@Nullable String host) {
        mHost = host;
        return this;
    }

    public Config logging(boolean enabled) {
        mLogging = enabled;
        return this;
    }

    public Config advancedRequestLogging(boolean enabled) {
        mDetailedRequestLogging = enabled;
        return this;
    }

    public Config defaultHeader(@NonNull String name, @Nullable Object value) {
        if (value == null)
            mDefaultHeaders.remove(name);
        else mDefaultHeaders.put(name, value);
        return this;
    }

    public Config connectTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Connect timeout must be greater than 0.");
        mConnectTimeout = timeout;
        return this;
    }

    public Config readTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Read timeout must be greater than 0.");
        mReadTimeout = timeout;
        return this;
    }

    public Config bufferSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("The buffer size must be greater than 0.");
        mBufferSize = size;
        return this;
    }

    public Config validators(ResponseValidator... validators) {
        mValidators = validators;
        return this;
    }

    @NonNull
    public ResponseConverter responseConverter(@Nullable String contentType) {
        if (contentType == null || contentType.trim().isEmpty())
            contentType = "application/json";
        else if (contentType.contains(";"))
            contentType = contentType.split(";")[0];
        final Class<? extends ResponseConverter> converterCls = mResponseConverters.get(contentType);
        if (converterCls == null)
            throw new IllegalStateException("No response converter available for content type " + contentType);
        return BridgeUtil.newInstance(converterCls);
    }

    @Deprecated
    public Config responseConverter(@NonNull String contentType, @Nullable ResponseConverter converter) {
        return responseConverter(contentType, converter != null ? converter.getClass() : null);
    }

    public Config responseConverter(@NonNull String contentType, @Nullable Class<? extends ResponseConverter> converter) {
        if (converter == null)
            mResponseConverters.remove(contentType);
        else
            mResponseConverters.put(contentType, converter);
        return this;
    }

    @NonNull
    public RequestConverter requestConverter(@Nullable String contentType) {
        if (contentType == null || contentType.trim().isEmpty())
            contentType = "application/json";
        else if (contentType.contains(";"))
            contentType = contentType.split(";")[0];
        final Class<? extends RequestConverter> converterCls = mRequestConverters.get(contentType);
        if (converterCls == null)
            throw new IllegalStateException("No request converter available for content type " + contentType);
        return BridgeUtil.newInstance(converterCls);
    }

    @Deprecated
    public Config requestConverter(@NonNull String contentType, @Nullable RequestConverter converter) {
        return requestConverter(contentType, converter != null ? converter.getClass() : null);
    }

    public Config requestConverter(@NonNull String contentType, @Nullable Class<? extends RequestConverter> converter) {
        if (converter == null)
            mRequestConverters.remove(contentType);
        else
            mRequestConverters.put(contentType, converter);
        return this;
    }

    public Config autoFollowRedirects(boolean follow) {
        mAutoFollowRedirects = follow;
        return this;
    }

    protected void destroy() {
        mHost = null;
        mDefaultHeaders.clear();
        mDefaultHeaders = null;
        mBufferSize = 0;
    }
}