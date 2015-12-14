package com.afollestad.bridge.conversion;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.conversion.base.ResponseConverter;

import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * @author Aidan Follestad (afollestad)
 */
public class JsonResponseConverter extends ResponseConverter {

    private JSONObject mCurrentObject;

    public JsonResponseConverter() {
    }

    public JsonResponseConverter(JSONObject currentObject) {
        mCurrentObject = currentObject;
    }

    @Override
    public void onPrepare(@NonNull Response response, @NonNull Object object) throws Exception {
        if (mCurrentObject == null)
            mCurrentObject = response.asJsonObject();
    }

    @Override
    public boolean canConvertField(@NonNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @SuppressLint("SwitchIntDef")
    @Nullable
    @Override
    public Object getValueFromResponse(@NonNull Field field, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception {
        final Body bodyAnnotation = field.getAnnotation(Body.class);
        final String name = getName(field, bodyAnnotation);
        if (mCurrentObject.isNull(name)) return null;

        switch (fieldType) {
            case FIELD_SHORT:
                return ((Integer) mCurrentObject.getInt(name)).shortValue();
            case FIELD_INTEGER:
                return mCurrentObject.getInt(name);
            case FIELD_LONG:
                return mCurrentObject.getLong(name);
            case FIELD_FLOAT:
                return ((Double) mCurrentObject.getDouble(name)).floatValue();
            case FIELD_DOUBLE:
                return mCurrentObject.getDouble(name);
            case FIELD_BOOLEAN:
                Object value = mCurrentObject.get(name);
                if (value instanceof Integer)
                    return (Integer) value == 1;
                else if (value instanceof Boolean)
                    return value;
                else throw new Exception("Unexpected type for JSON field " + value);
            case FIELD_STRING:
                return mCurrentObject.getString(name);
            default:
                return mCurrentObject.get(name);
        }
    }

    @NonNull
    @Override
    public ResponseConverter spawnConverter(@NonNull Field field, @NonNull Object responseValue, @NonNull Response response) throws Exception {
        return new JsonResponseConverter((JSONObject) responseValue);
    }

    @Override
    public void onFinish(@NonNull Object object, @NonNull Response response) throws Exception {
    }

    private String getName(@NonNull Field fld, @NonNull Body body) {
        if (body.name() == null || body.name().trim().isEmpty())
            return fld.getName();
        return body.name();
    }
}