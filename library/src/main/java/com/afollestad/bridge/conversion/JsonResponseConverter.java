package com.afollestad.bridge.conversion;

import android.annotation.SuppressLint;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.conversion.base.ResponseConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * @author Aidan Follestad (afollestad)
 */
public class JsonResponseConverter extends ResponseConverter {

    private JSONObject currentObject;

    @SuppressWarnings("unused") public JsonResponseConverter() {
    }

    @SuppressWarnings("WeakerAccess") public JsonResponseConverter(JSONObject currentObject) {
        this.currentObject = currentObject;
    }

    @Override
    public void onPrepare(@NonNull Response response, @NonNull Object object) throws Exception {
        if (currentObject == null)
            currentObject = response.asJsonObject();
    }

    @Override public boolean canConvertField(@NonNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @SuppressLint("SwitchIntDef")
    @Nullable
    @Override
    public Object getValueFromResponse(@NonNull String name, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception {
        if (currentObject.isNull(name)) return null;
        switch (fieldType) {
            case FIELD_SHORT:
                return ((Integer) currentObject.getInt(name)).shortValue();
            case FIELD_INTEGER:
                return currentObject.getInt(name);
            case FIELD_LONG:
                return currentObject.getLong(name);
            case FIELD_FLOAT:
                return ((Double) currentObject.getDouble(name)).floatValue();
            case FIELD_DOUBLE:
                return currentObject.getDouble(name);
            case FIELD_BOOLEAN:
                Object value = currentObject.get(name);
                if (value instanceof Integer)
                    return (Integer) value == 1;
                else if (value instanceof Boolean)
                    return value;
                else throw new Exception("Unexpected type for JSON field " + value);
            case FIELD_STRING:
                return currentObject.getString(name);
            default:
                return currentObject.get(name);
        }
    }

    @SuppressLint("SwitchIntDef")
    @Nullable
    @Override
    public Object getValueFromResponse(@NonNull String[] nameParts, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception {
        JSONObject currentObj = null;
        for (int i = 0; i < nameParts.length - 1; i++) {
            Object val = currentObj != null ? currentObj.opt(nameParts[i]) : currentObject.opt(nameParts[i]);
            if (val == null)
                return null;
            if (val instanceof JSONObject) {
                currentObj = (JSONObject) val;
            } else {
                throw new RuntimeException(String.format("Expected JSONObject for name part %s, but found %s.",
                        nameParts[i], val.getClass().getName()));
            }
        }

        final String name = nameParts[nameParts.length - 1];
        final JSONObject pullObj = currentObj != null ? currentObj : currentObject;
        if (pullObj.isNull(name)) return null;
        switch (fieldType) {
            case FIELD_SHORT:
                return ((Integer) pullObj.getInt(name)).shortValue();
            case FIELD_INTEGER:
                return pullObj.getInt(name);
            case FIELD_LONG:
                return pullObj.getLong(name);
            case FIELD_FLOAT:
                return ((Double) pullObj.getDouble(name)).floatValue();
            case FIELD_DOUBLE:
                return pullObj.getDouble(name);
            case FIELD_BOOLEAN:
                Object value = pullObj.get(name);
                if (value instanceof Integer)
                    return (Integer) value == 1;
                else if (value instanceof Boolean)
                    return value;
                else throw new Exception("Unexpected type for JSON field " + value);
            case FIELD_STRING:
                return pullObj.getString(name);
            default:
                return pullObj.get(name);
        }
    }

    @NonNull
    @Override
    public String getFieldInputName(@NonNull Field field) throws Exception {
        final Body bodyAnnotation = field.getAnnotation(Body.class);
        return getName(field, bodyAnnotation);
    }

    @Override
    public int getResponseArrayLength(@NonNull Response response) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NonNull Response response, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().get(index);
    }

    @Override
    public int getResponseArrayLength(@NonNull Object array) {
        return ((JSONArray) array).length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NonNull Object array, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception {
        return ((JSONArray) array).get(index);
    }

    @NonNull
    @Override
    public ResponseConverter spawnConverter(@NonNull Class<?> forType, @NonNull Object responseValue, @NonNull Response response) throws Exception {
        return new JsonResponseConverter((JSONObject) responseValue);
    }

    @Override
    public void onFinish(@NonNull Response response, @NonNull Object object) throws Exception {
    }

    private String getName(@NonNull Field fld, @NonNull Body body) {
        if (body.name() == null || body.name().trim().isEmpty())
            return fld.getName();
        return body.name();
    }
}