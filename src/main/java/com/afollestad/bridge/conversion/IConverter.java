package com.afollestad.bridge.conversion;

import com.afollestad.bridge.Response;

import java.util.List;

/** @author Aidan Follestad (afollestad) */
public abstract class IConverter {

  public abstract byte[] serialize(Object object) throws Exception;

  public abstract byte[] serializeArray(Object[] objects) throws Exception;

  public abstract byte[] serializeList(List<Object> objects) throws Exception;

  public abstract <T> T deserialize(Response response, Class<T> cls) throws Exception;

  public abstract <T> T[] deserializeArray(Response response, Class<T> cls) throws Exception;

  public abstract <T> List<T> deserializeList(Response response, Class<T> cls) throws Exception;
}
