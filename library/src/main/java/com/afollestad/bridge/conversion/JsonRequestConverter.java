package com.afollestad.bridge.conversion;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.RequestBuilder;
import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.conversion.base.RequestConverter;

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

    @Override
    public void onPrepare(@NonNull RequestBuilder request, @NonNull Object object) throws Exception {
        // Not used in the JsonRequestConverter, called before conversion of an object begins
    }

    @Override
    public void onPrepare(@NonNull RequestBuilder request, @NonNull Object[] objects) throws Exception {
        // not used in the JsonRequestConverter, called before conversion of an object array begins
    }

    @Override
    public JSONObject createOutputObject() {
        return new JSONObject();
    }

    @Override
    public JSONArray createOutputArray() {
        return new JSONArray();
    }

    @Override
    public boolean canConvertField(@NonNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @NonNull
    @Override
    public String getFieldOutputName(@NonNull Field field) throws Exception {
        Body bodyAnnotation = field.getAnnotation(Body.class);
        if (bodyAnnotation.name() != null && !bodyAnnotation.name().trim().isEmpty())
            return bodyAnnotation.name();
        return field.getName();
    }

    @NonNull
    @Override
    public JSONObject getAttachTarget(@NonNull JSONObject parent, @NonNull String[] nameParts) {
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
                        throw new RuntimeException("Path part " + nameParts[i] + " already exists in parent and is not a JSONObject.");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Failed to get" + nameParts[i] + " from the parent object.", e);
                }
            }
        }
        return currentObj;
    }

    @Override
    public void onAttachValueToObject(@NonNull String name, @NonNull JSONObject object, @Nullable Object value, @FieldType int fieldType) throws Exception {
        object.put(name, value);
    }

    @Override
    public void onAttachValueToArray(@NonNull JSONArray array, @Nullable Object value, @FieldType int fieldType) throws Exception {
        array.put(value);
    }

    @Override
    public void onAttachObjectToParent(@NonNull String name, @Nullable JSONObject object, @NonNull JSONObject parent) throws Exception {
        parent.put(name, object);
    }

    @Override
    public void onAttachArrayToParent(@NonNull String name, @Nullable JSONArray array, @NonNull JSONObject parent) throws Exception {
        parent.put(name, array);
    }

    @Nullable
    @Override
    public byte[] onFinish(@NonNull JSONObject output, @NonNull RequestBuilder request, @NonNull Object object) throws Exception {
        return output.toString(4).getBytes("UTF-8");
    }

    @Nullable
    @Override
    public byte[] onFinish(@NonNull JSONArray output, @NonNull RequestBuilder request, @NonNull Object[] objects) throws Exception {
        return output.toString(4).getBytes("UTF-8");
    }
}