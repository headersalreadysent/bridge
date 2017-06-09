package com.afollestad.bridge.conversion;

import com.afollestad.ason.Ason;
import com.afollestad.ason.AsonArray;
import com.afollestad.bridge.Response;
import java.util.List;

/** @author Aidan Follestad (afollestad) */
@SuppressWarnings("unchecked")
public class JsonConverter extends IConverter {

  @Override
  public byte[] serialize(Object object) throws Exception {
    Ason ason = Ason.serialize(object);
    return ason.toString().getBytes("UTF-8");
  }

  @Override
  public byte[] serializeArray(Object[] objects) throws Exception {
    AsonArray ason = Ason.serializeArray(objects);
    return ason.toString().getBytes("UTF-8");
  }

  @Override
  public byte[] serializeList(List<Object> objects) throws Exception {
    AsonArray ason = Ason.serializeList(objects);
    return ason.toString().getBytes("UTF-8");
  }

  @Override
  public <T> T deserialize(Response response, Class<T> cls) throws Exception {
    return Ason.deserialize(response.asAsonObject(), cls);
  }

  @Override
  public <T> T[] deserializeArray(Response response, Class<T> cls) throws Exception {
    return (T[]) Ason.deserialize(response.asAsonArray(), cls);
  }

  @Override
  public <T> List<T> deserializeList(Response response, Class<T> cls) throws Exception {
    return Ason.deserializeList(response.asAsonArray(), cls);
  }
}
