package com.afollestad.bridge;

import com.afollestad.bridge.annotations.ContentType;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author Aidan Follestad (afollestad) */
class BridgeUtil {

  private BridgeUtil() {}

  static void closeQuietly(@Nullable Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (Throwable ignored) {
      }
    }
  }

  static byte[] readEntireStream(@Nullable InputStream is) throws IOException {
    if (is == null) return null;
    ByteArrayOutputStream os = null;
    try {
      os = new ByteArrayOutputStream();
      byte[] buffer = new byte[Bridge.config().bufferSize];
      int read;
      while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
      os.flush();
      return os.toByteArray();
    } finally {
      BridgeUtil.closeQuietly(os);
      BridgeUtil.closeQuietly(is);
    }
  }

  static void throwIfNotSuccess(Response response) throws BridgeException {
    if (!response.isSuccess())
      throw new BridgeException(
          response,
          String.format("Response was unsuccessful: %s %s", response.code(), response.phrase()),
          BridgeException.REASON_RESPONSE_UNSUCCESSFUL);
  }

  @SuppressWarnings("unchecked")
  static <T> T newInstance(@NotNull Class<T> cls) {
    final Constructor ctor = getDefaultConstructor(cls);
    try {
      return (T) ctor.newInstance();
    } catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException(
          "Failed to instantiate " + cls.getName() + ": " + t.getLocalizedMessage());
    }
  }

  private static Constructor<?> getDefaultConstructor(@NotNull Class<?> cls) {
    final Constructor[] ctors = cls.getDeclaredConstructors();
    Constructor ctor = null;
    for (Constructor ct : ctors) {
      ctor = ct;
      if (ctor.getGenericParameterTypes().length == 0) break;
    }
    if (ctor == null)
      throw new IllegalStateException("No default constructor found for " + cls.getName());
    ctor.setAccessible(true);
    return ctor;
  }

  @NotNull
  static String getContentType(@NotNull Class<?> forClass, @Nullable Object defaultType) {
    ContentType contentTypeAnnotation = forClass.getAnnotation(ContentType.class);
    if (contentTypeAnnotation != null && !contentTypeAnnotation.value().trim().isEmpty()) {
      return contentTypeAnnotation.value();
    }
    if (defaultType == null
        || !(defaultType instanceof String)
        || ((String) defaultType).trim().isEmpty()) {
      defaultType = "application/json";
    }
    return (String) defaultType;
  }
}
