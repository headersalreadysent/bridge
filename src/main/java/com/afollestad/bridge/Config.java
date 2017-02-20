package com.afollestad.bridge;

import com.afollestad.bridge.conversion.JsonRequestConverter;
import com.afollestad.bridge.conversion.JsonResponseConverter;
import com.afollestad.bridge.conversion.base.RequestConverter;
import com.afollestad.bridge.conversion.base.ResponseConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings({"WeakerAccess", "unused"}) public final class Config {

    Config() {
        defaultHeaders = new HashMap<>();
        defaultHeaders.put("User-Agent", "afollestad/Bridge");
        defaultHeaders.put("Content-Type", "text/plain");

        requestConverters = new HashMap<>();
        requestConverters.put("application/json", JsonRequestConverter.class);
        requestConverters.put("text/plain", JsonRequestConverter.class);

        responseConverters = new HashMap<>();
        responseConverters.put("application/json", JsonResponseConverter.class);
        responseConverters.put("text/plain", JsonResponseConverter.class);
    }

    String host;
    HashMap<String, Object> defaultHeaders;
    int connectTimeout = 10000;
    int readTimeout = 15000;
    int bufferSize = 1024 * 4;
    boolean logging = false;
    ResponseValidator[] validators;
    private HashMap<String, Class<? extends RequestConverter>> requestConverters;
    private HashMap<String, Class<? extends ResponseConverter>> responseConverters;
    boolean autoFollowRedirects = true;

    public Config host(@Nullable String host) {
        this.host = host;
        return this;
    }

    public Config logging(boolean enabled) {
        logging = enabled;
        return this;
    }

    public Config defaultHeader(@NotNull String name, @Nullable Object value) {
        if (value == null)
            defaultHeaders.remove(name);
        else defaultHeaders.put(name, value);
        return this;
    }

    public Config connectTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Connect timeout must be greater than 0.");
        connectTimeout = timeout;
        return this;
    }

    public Config readTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Read timeout must be greater than 0.");
        readTimeout = timeout;
        return this;
    }

    public Config bufferSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("The buffer size must be greater than 0.");
        bufferSize = size;
        return this;
    }

    public Config validators(ResponseValidator... validators) {
        this.validators = validators;
        return this;
    }

    @NotNull public ResponseConverter responseConverter(@Nullable String contentType) {
        if (contentType == null || contentType.trim().isEmpty())
            contentType = "application/json";
        else if (contentType.contains(";"))
            contentType = contentType.split(";")[0];
        final Class<? extends ResponseConverter> converterCls = responseConverters.get(contentType);
        if (converterCls == null)
            throw new IllegalStateException("No response converter available for content type " + contentType);
        return BridgeUtil.newInstance(converterCls);
    }

    @Deprecated public Config responseConverter(@NotNull String contentType, @Nullable ResponseConverter converter) {
        return responseConverter(contentType, converter != null ? converter.getClass() : null);
    }

    public Config responseConverter(@NotNull String contentType, @Nullable Class<? extends ResponseConverter> converter) {
        if (converter == null)
            responseConverters.remove(contentType);
        else
            responseConverters.put(contentType, converter);
        return this;
    }

    @NotNull public RequestConverter requestConverter(@Nullable String contentType) {
        if (contentType == null || contentType.trim().isEmpty())
            contentType = "application/json";
        else if (contentType.contains(";"))
            contentType = contentType.split(";")[0];
        final Class<? extends RequestConverter> converterCls = requestConverters.get(contentType);
        if (converterCls == null)
            throw new IllegalStateException("No request converter available for content type " + contentType);
        return BridgeUtil.newInstance(converterCls);
    }

    @Deprecated public Config requestConverter(@NotNull String contentType, @Nullable RequestConverter converter) {
        return requestConverter(contentType, converter != null ? converter.getClass() : null);
    }

    public Config requestConverter(@NotNull String contentType, @Nullable Class<? extends RequestConverter> converter) {
        if (converter == null)
            requestConverters.remove(contentType);
        else
            requestConverters.put(contentType, converter);
        return this;
    }

    public Config autoFollowRedirects(boolean follow) {
        autoFollowRedirects = follow;
        return this;
    }

    void destroy() {
        host = null;
        defaultHeaders.clear();
        defaultHeaders = null;
        bufferSize = 0;
    }
}