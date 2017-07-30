package com.afollestad.bridge;

import com.afollestad.bridge.conversion.IConverter;
import com.afollestad.bridge.conversion.JsonConverter;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Config {

  String host;
  HashMap<String, Object> defaultHeaders;
  int connectTimeout = 10000;
  int readTimeout = 15000;
  int bufferSize = 1024 * 4;
  boolean logging = false;
  ResponseValidator[] validators;
  boolean autoFollowRedirects = true;
  int maxRedirects = 4;
  private HashMap<String, Class<? extends IConverter>> converters;

  Config() {
    defaultHeaders = new HashMap<>();
    defaultHeaders.put("User-Agent", "afollestad/Bridge");
    defaultHeaders.put("Content-Type", "text/plain");

    converters = new HashMap<>();
    converters.put("application/json", JsonConverter.class);
    converters.put("text/plain", JsonConverter.class);
  }

  public Config host(@Nullable String host) {
    this.host = host;
    return this;
  }

  public Config logging(boolean enabled) {
    logging = enabled;
    return this;
  }

  public Config defaultHeader(@NotNull String name, @Nullable Object value) {
    if (value == null) {
      defaultHeaders.remove(name);
    } else {
      defaultHeaders.put(name, value);
    }
    return this;
  }

  public Config connectTimeout(int timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Connect timeout must be greater than 0.");
    }
    connectTimeout = timeout;
    return this;
  }

  public Config readTimeout(int timeout) {
    if (timeout <= 0) {
      throw new IllegalArgumentException("Read timeout must be greater than 0.");
    }
    readTimeout = timeout;
    return this;
  }

  public Config bufferSize(int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("The buffer size must be greater than 0.");
    }
    bufferSize = size;
    return this;
  }

  public Config validators(ResponseValidator... validators) {
    this.validators = validators;
    return this;
  }

  @NotNull
  public IConverter converter(@Nullable String contentType) {
    if (contentType == null || contentType.trim().isEmpty()) {
      contentType = "application/json";
    } else if (contentType.contains(";")) {
      contentType = contentType.split(";")[0];
    }
    final Class<? extends IConverter> converterCls = converters.get(contentType);
    if (converterCls == null) {
      throw new IllegalStateException("No converter available for content type: " + contentType);
    }
    return BridgeUtil.newInstance(converterCls);
  }

  public Config converter(
      @NotNull String contentType, @Nullable Class<? extends IConverter> converter) {
    if (converter == null) {
      converters.remove(contentType);
    } else {
      converters.put(contentType, converter);
    }
    return this;
  }

  public Config autoFollowRedirects(boolean follow) {
    autoFollowRedirects = follow;
    return this;
  }

  public Config maxRedirects(int maxRedirects) {
    this.maxRedirects = maxRedirects;
    return this;
  }

  void destroy() {
    host = null;
    defaultHeaders.clear();
    defaultHeaders = null;
    bufferSize = 0;
  }
}
