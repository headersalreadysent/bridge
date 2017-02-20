package com.afollestad.bridge.conversion;

import com.afollestad.bridge.RequestBuilder;
import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.conversion.base.RequestConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * @author Aidan Follestad (afollestad)
 */
public class JsonRequestConverter extends RequestConverter<JSONObject, JSONArray> {

    public JsonRequestConverter() {
    }

    @Override public void onPrepare(
            @NotNull RequestBuilder request, @NotNull Object object) throws Exception {
        // Not used in the JsonRequestConverter, called before conversion of an object begins
    }

    @Override public void onPrepare(
            @NotNull RequestBuilder request, @NotNull Object[] objects) throws Exception {
        // not used in the JsonRequestConverter, called before conversion of an object array begins
    }

    @Override public JSONObject createOutputObject() {
        return new JSONObject();
    }

    @Override public JSONArray createOutputArray() {
        return new JSONArray();
    }

    @Override public boolean canConvertField(@NotNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @NotNull
    @Override
    public String getFieldOutputName(@NotNull Field field) throws Exception {
        Body bodyAnnotation = field.getAnnotation(Body.class);
        if (!bodyAnnotation.name().trim().isEmpty())
            return bodyAnnotation.name();
        return field.getName();
    }

    @NotNull
    @Override
    public JSONObject getAttachTarget(@NotNull JSONObject parent, @NotNull String[] nameParts) {
        JSONObject currentObj = parent;
        for (int i = 0; i < nameParts.length - 1; i++) {
            if (currentObj.isNull(nameParts[i])) {
                JSONObject newObj = new JSONObject();
                try {
                    currentObj.put(nameParts[i], newObj);
                } catch (JSONException e) {
                    throw new RuntimeException("Failed to add new path part.", e);
                }
                currentObj = newObj;
            } else {
                try {
                    Object val = currentObj.get(nameParts[i]);
                    if (val instanceof JSONObject)
                        currentObj = (JSONObject) val;
                    else {
                        throw new RuntimeException("Path part " + nameParts[i] +
                                " already exists in parent and is not a JSONObject.");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Failed to get" + nameParts[i] +
                            " from the parent object.", e);
                }
            }
        }
        return currentObj;
    }

    @Override public void onAttachValueToObject(@NotNull String name,
                                                @NotNull JSONObject object,
                                                @Nullable Object value,
                                                int fieldType) throws Exception {
        object.put(name, value);
    }

    @Override public void onAttachValueToArray(@NotNull JSONArray array,
                                               @Nullable Object value,
                                               int fieldType) throws Exception {
        array.put(value);
    }

    @Override public void onAttachObjectToParent(@NotNull String name,
                                                 @Nullable JSONObject object,
                                                 @NotNull JSONObject parent) throws Exception {
        parent.put(name, object);
    }

    @Override public void onAttachArrayToParent(@NotNull String name,
                                                @Nullable JSONArray array,
                                                @NotNull JSONObject parent) throws Exception {
        parent.put(name, array);
    }

    @Nullable
    @Override
    public byte[] onFinish(@NotNull JSONObject output,
                           @NotNull RequestBuilder request,
                           @NotNull Object object) throws Exception {
        return output.toString(4).getBytes("UTF-8");
    }

    @Nullable
    @Override
    public byte[] onFinish(@NotNull JSONArray output,
                           @NotNull RequestBuilder request,
                           @NotNull Object[] objects) throws Exception {
        return output.toString(4).getBytes("UTF-8");
    }
}