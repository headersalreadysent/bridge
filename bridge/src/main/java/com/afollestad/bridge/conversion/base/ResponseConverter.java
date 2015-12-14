package com.afollestad.bridge.conversion.base;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.BridgeUtil;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Header;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class ResponseConverter extends Converter {

    private String getName(@NonNull Field fld, @NonNull Header header) {
        if (header.name() == null || header.name().trim().isEmpty())
            return fld.getName();
        return header.name();
    }

    @Nullable
    public final <T> T convert(@NonNull Response response, @NonNull Class<T> targetCls) {
        byte[] responseContent = response.asBytes();
        if (responseContent == null || responseContent.length == 0) return null;
        T object = BridgeUtil.newInstance(targetCls);
        try {
            onPrepare(response, object);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to prepare ResponseConverter for target class %s: %s",
                    targetCls.getName(), e.getMessage()), e);
        }
        object = convertObject(object, targetCls, response);
        try {
            onFinish(object, response);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to finish ResponseConverter for target class %s: %s",
                    targetCls.getName(), e.getMessage()), e);
        }
        return object;
    }

    @SuppressLint("SwitchIntDef")
    private <T> T convertObject(@Nullable T object, @NonNull Class<T> targetCls, @NonNull Response response) {
        if (object == null)
            object = BridgeUtil.newInstance(targetCls);
        final List<Field> fields = getAllFields(targetCls);
        for (Field field : fields) {
            field.setAccessible(true);
            @FieldType
            final int fieldType = getFieldType(field.getType());

            Header headerAnnotation = field.getAnnotation(Header.class);
            if (headerAnnotation != null) {
                final String headerValue = response.header(getName(field, headerAnnotation));
                try {
                    switch (fieldType) {
                        case Converter.FIELD_SHORT:
                            if (headerValue == null)
                                field.setShort(object, (short) 0);
                            else
                                field.setShort(object, Short.parseShort(headerValue));
                            break;
                        case Converter.FIELD_INTEGER:
                            if (headerValue == null)
                                field.setInt(object, 0);
                            else
                                field.setInt(object, Integer.parseInt(headerValue));
                            break;
                        case Converter.FIELD_LONG:
                            if (headerValue == null)
                                field.setLong(object, 0L);
                            else
                                field.setLong(object, Long.parseLong(headerValue));
                            break;
                        case Converter.FIELD_DOUBLE:
                            if (headerValue == null)
                                field.setDouble(object, 0d);
                            else
                                field.setDouble(object, Double.parseDouble(headerValue));
                            break;
                        case Converter.FIELD_FLOAT:
                            if (headerValue == null)
                                field.setFloat(object, 0f);
                            else
                                field.setFloat(object, Float.parseFloat(headerValue));
                            break;
                        case Converter.FIELD_BOOLEAN:
                            if (headerValue == null)
                                field.setBoolean(object, false);
                            else
                                field.setBoolean(object, headerValue.equalsIgnoreCase("true") || headerValue.equals("1"));
                            break;
                        case Converter.FIELD_STRING:
                            field.set(object, headerValue);
                            break;
                        default:
                            throw new Exception("Header annotations can only be applied to primitive field types.");
                    }
                } catch (Throwable t) {
                    throw new RuntimeException(String.format("Failed to set the value of field %s of class %s: %s",
                            field.getName(), targetCls.getName(), t.getMessage()), t);
                }
                continue;
            }

            boolean canConvert;
            try {
                canConvert = canConvertField(field);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to check if field %s can be converted by %s: %s",
                        field.getName(), getClass().getName(), e.getMessage()), e);
            }
            if (canConvert) {
                try {
                    final Object responseValue;
                    try {
                        responseValue = getValueFromResponse(field, fieldType, field.getType());
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Failed to get value from response for field %s of type %s: %s",
                                field.getName(), field.getType().getName(), e.getMessage()), e);
                    }

                    if (isPrimitive(field.getType())) {
                        switch (fieldType) {
                            default:
                                // should never happen
                                throw new RuntimeException("Unknown primitive field type: " + fieldType);
                            case FIELD_SHORT:
                                if (responseValue == null)
                                    field.setShort(object, (short) 0);
                                else
                                    field.setShort(object, (short) responseValue);
                                break;
                            case FIELD_INTEGER:
                                if (responseValue == null)
                                    field.setInt(object, 0);
                                else
                                    field.setInt(object, (int) responseValue);
                                break;
                            case FIELD_LONG:
                                if (responseValue == null)
                                    field.setLong(object, 0L);
                                else
                                    field.setLong(object, (long) responseValue);
                                break;
                            case FIELD_FLOAT:
                                if (responseValue == null)
                                    field.setFloat(object, 0f);
                                else
                                    field.setFloat(object, (float) responseValue);
                                break;
                            case FIELD_DOUBLE:
                                if (responseValue == null)
                                    field.setDouble(object, 0d);
                                else
                                    field.setDouble(object, (double) responseValue);
                                break;
                            case FIELD_BOOLEAN:
                                if (responseValue == null)
                                    field.setBoolean(object, false);
                                else if (responseValue instanceof Integer)
                                    field.setBoolean(object, (Integer) responseValue == 1);
                                else
                                    field.setBoolean(object, (boolean) responseValue);
                                break;
                            case FIELD_STRING:
                                field.set(object, responseValue);
                                break;
                        }
                    } else if (isArray(field.getType())) {
                        if (responseValue == null)
                            field.set(object, null);
                        else ; // TODO
                    } else if (isArrayList(field.getType())) {
                        if (responseValue == null)
                            field.set(object, null);
                        else ; // TODO
                    } else {
                        if (responseValue == null) {
                            field.set(object, null);
                        } else {
                            try {
                                ResponseConverter converter = spawnConverter(field, responseValue, response);
                                field.set(object, converter.convert(response, field.getType()));
                            } catch (Exception e) {
                                throw new RuntimeException(String.format("Failed to spawn a converter for field %s of type %s: %s",
                                        field.getName(), field.getType().getName(), e.getMessage()), e);
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("Failed to set the value of field %s of class %s: %s",
                            field.getName(), targetCls.getName(), e.getMessage()), e);
                }
            }
        }
        return object;
    }

//    @Nullable
//    private Object convertArray(@NonNull Class<?> cls, @Nullable JSONArray responseBody) {
//        if (responseBody == null)
//            return null;
//        else if (responseBody.length() == 0)
//            return Array.newInstance(cls, 0);
//        try {
//            if (isShort(cls)) {
//                short[] array = (short[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = ((Integer) responseBody.getInt(i)).shortValue();
//                return array;
//            } else if (isInteger(cls)) {
//                int[] array = (int[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = responseBody.getInt(i);
//                return array;
//            } else if (isLong(cls)) {
//                long[] array = (long[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = responseBody.getLong(i);
//                return array;
//            } else if (isFloat((cls))) {
//                float[] array = (float[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = (float) responseBody.getDouble(i);
//                return array;
//            } else if (isDouble(cls)) {
//                double[] array = (double[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = responseBody.getDouble(i);
//                return array;
//            } else if (isBoolean(cls)) {
//                boolean[] array = (boolean[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++) {
//                    Object val = responseBody.get(i);
//                    if (val instanceof Integer)
//                        array[i] = (Integer) val == 1;
//                    else array[i] = (Boolean) val;
//                }
//                return array;
//            } else if (isString(cls)) {
//                String[] array = (String[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = responseBody.getString(i);
//                return array;
//            } else if (isArray(cls)) {
//                Object[] array = (Object[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = convertArray(cls.getComponentType(), responseBody.getJSONArray(i));
//                return array;
//            } else if (isArrayList(cls)) {
//                Object[] array = (Object[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = convertList(cls.getComponentType(), responseBody.getJSONArray(i));
//                return array;
//            } else {
//                Object[] array = (Object[]) Array.newInstance(cls, responseBody.length());
//                for (int i = 0; i < responseBody.length(); i++)
//                    array[i] = convertObject(null, cls, null);
//                return array;
//            }
//        } catch (JSONException e) {
//            throw new RuntimeException(String.format("Failed to process array value of type %s: %s",
//                    cls.getName(), e.getMessage()), e);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    @Nullable
//    private ArrayList convertList(@NonNull Class<?> cls, @Nullable JSONArray responseBody) {
//        if (responseBody == null) return null;
//        final ArrayList list = new ArrayList(responseBody.length());
//        if (responseBody.length() == 0) return list;
//        try {
//            if (isShort(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(((Integer) responseBody.getInt(i)).shortValue());
//            } else if (isInteger(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(responseBody.getInt(i));
//            } else if (isLong(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(responseBody.getLong(i));
//            } else if (isFloat((cls))) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add((float) responseBody.getDouble(i));
//            } else if (isDouble(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(responseBody.getDouble(i));
//            } else if (isBoolean(cls)) {
//                for (int i = 0; i < responseBody.length(); i++) {
//                    Object val = responseBody.get(i);
//                    if (val instanceof Integer)
//                        list.add((Integer) val == 1);
//                    else list.add(val);
//                }
//            } else if (isString(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(responseBody.getString(i));
//            } else if (isArray(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(convertArray(cls.getComponentType(), responseBody.getJSONArray(i)));
//            } else if (isArrayList(cls)) {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(convertList(cls.getComponentType(), responseBody.getJSONArray(i)));
//            } else {
//                for (int i = 0; i < responseBody.length(); i++)
//                    list.add(convertObject(null, cls, null));
//            }
//            return list;
//        } catch (JSONException e) {
//            throw new RuntimeException(String.format("Failed to process array value of type %s: %s",
//                    cls.getName(), e.getMessage()), e);
//        }
//    }

    public abstract void onPrepare(@NonNull Response response, @NonNull Object object) throws Exception;

    public abstract boolean canConvertField(@NonNull Field field) throws Exception;

    @Nullable
    public abstract Object getValueFromResponse(@NonNull Field field, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception;

    @NonNull
    public abstract ResponseConverter spawnConverter(@NonNull Field field, @NonNull Object responseValue, @NonNull Response response) throws Exception;

    public abstract void onFinish(@NonNull Object object, @NonNull Response response) throws Exception;
}