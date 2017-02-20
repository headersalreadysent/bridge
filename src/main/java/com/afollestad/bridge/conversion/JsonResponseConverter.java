package com.afollestad.bridge.conversion;

import com.afollestad.bridge.conversion.base.ResponseConverter;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Body;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public void onPrepare(@NotNull Response response,
                          @NotNull Object object) throws Exception {
        if (currentObject == null)
            currentObject = response.asJsonObject();
    }

    @Override public boolean canConvertField(@NotNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @Nullable
    @Override
    public Object getValueFromResponse(@NotNull String name,
                                       int fieldType,
                                       @NotNull Class<?> cls) throws Exception {
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

    @Nullable
    @Override
    public Object getValueFromResponse(@NotNull String[] nameParts,
                                       int fieldType,
                                       @NotNull Class<?> cls) throws Exception {
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

    @NotNull
    @Override
    public String getFieldInputName(@NotNull Field field) throws Exception {
        final Body bodyAnnotation = field.getAnnotation(Body.class);
        return getName(field, bodyAnnotation);
    }

    @Override public int getResponseArrayLength(@NotNull Response response) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NotNull Response response,
                                            int index) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().get(index);
    }

    @Override public int getResponseArrayLength(@NotNull Object array) {
        return ((JSONArray) array).length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NotNull Object array,
                                            int index) throws Exception {
        return ((JSONArray) array).get(index);
    }

    @NotNull
    @Override
    public ResponseConverter spawnConverter(@NotNull Class<?> forType,
                                            @NotNull Object responseValue,
                                            @NotNull Response response) throws Exception {
        return new JsonResponseConverter((JSONObject) responseValue);
    }

    @Override public void onFinish(@NotNull Response response, @NotNull Object object) throws Exception {
    }

    private String getName(@NotNull Field fld, @NotNull Body body) {
        if (body.name().trim().isEmpty())
            return fld.getName();
        return body.name();
    }
}