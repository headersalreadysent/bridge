package com.afollestad.bridge;

import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A version of {@link AsResults} that throws exceptions for all methods. Used in {@link
 * RequestBuilder}.
 *
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("unused")
interface AsResultsExceptions {

  @Nullable
  byte[] asBytes() throws BridgeException;

  void asBytes(@NotNull ResponseConvertCallback<byte[]> callback);

  @Nullable
  String asString() throws BridgeException;

  void asString(@NotNull ResponseConvertCallback<String> callback);

  @Nullable
  Ason asAsonObject() throws BridgeException;

  void asAsonObject(@NotNull ResponseConvertCallback<Ason> callback);

  @Nullable
  AsonArray<?> asAsonArray() throws BridgeException;

  void asAsonArray(@NotNull ResponseConvertCallback<AsonArray<?>> callback);

  @Nullable
  JSONObject asJsonObject() throws BridgeException;

  void asJsonObject(@NotNull ResponseConvertCallback<JSONObject> callback);

  @Nullable
  JSONArray asJsonArray() throws BridgeException;

  void asJsonArray(@NotNull ResponseConvertCallback<JSONArray> callback);

  void asFile(@NotNull File destination) throws BridgeException;

  void asFile(@NotNull File destination, @NotNull ResponseConvertCallback<File> callback);

  @Nullable
  <T> T asClass(@NotNull Class<T> cls) throws BridgeException;

  <T> void asClass(@NotNull Class<T> cls, @NotNull ResponseConvertCallback<T> callback);

  @Nullable
  <T> T[] asClassArray(@NotNull Class<T> cls) throws BridgeException;

  <T> void asClassArray(@NotNull Class<T> cls, @NotNull ResponseConvertCallback<T[]> callback);

  @Nullable
  <T> List<T> asClassList(@NotNull Class<T> cls) throws BridgeException;

  <T> void asClassList(@NotNull Class<T> cls, @NotNull ResponseConvertCallback<List<T>> callback);
}
