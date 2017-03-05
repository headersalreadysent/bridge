package com.afollestad.bridge;

import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/** @author Aidan Follestad (afollestad) */
interface AsResults {

  @Nullable
  byte[] asBytes();

  @Nullable
  String asString();

  @Nullable
  Ason asAsonObject() throws BridgeException;

  @Nullable
  AsonArray<?> asAsonArray() throws BridgeException;

  @Nullable
  JSONObject asJsonObject() throws BridgeException;

  @Nullable
  JSONArray asJsonArray() throws BridgeException;

  void asFile(File destination) throws BridgeException;

  @Nullable
  <T> T asClass(Class<T> cls) throws BridgeException;

  @Nullable
  <T> T[] asClassArray(@NotNull Class<T> cls) throws BridgeException;

  @Nullable
  <T> List<T> asClassList(@NotNull Class<T> cls) throws BridgeException;
}
