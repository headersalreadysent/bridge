package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.annotations.Header;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
class Converter {

    private Converter() {
    }

    private static List<Field> getAllFields(@NonNull Class<?> cls) {
        final List<Field> fields = new ArrayList<>(Arrays.asList(cls.getDeclaredFields()));
        while (cls.getSuperclass() != null) {
            cls = cls.getSuperclass();
            Collections.addAll(fields, cls.getDeclaredFields());
        }
        return fields;
    }

    private static boolean isShort(@NonNull Class<?> fieldType) {
        return fieldType == short.class || fieldType == Short.class;
    }

    private static boolean isInteger(@NonNull Class<?> fieldType) {
        return fieldType == int.class || fieldType == Integer.class;
    }

    private static boolean isLong(@NonNull Class<?> fieldType) {
        return fieldType == long.class || fieldType == Long.class;
    }

    private static boolean isFloat(@NonNull Class<?> fieldType) {
        return fieldType == float.class || fieldType == Float.class;
    }

    private static boolean isDouble(@NonNull Class<?> fieldType) {
        return fieldType == double.class || fieldType == Double.class;
    }

    private static boolean isBoolean(@NonNull Class<?> fieldType) {
        return fieldType == boolean.class || fieldType == Boolean.class;
    }

    private static boolean isString(@NonNull Class<?> fieldType) {
        return fieldType == String.class;
    }

    private static boolean isArray(@NonNull Class<?> fieldType) {
        return fieldType.isArray();
    }

    private static boolean isArrayList(@NonNull Class<?> fieldType) {
        return fieldType == List.class || fieldType == ArrayList.class;
    }

    private static Class<?> getArrayListType(@NonNull Field field) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        return (Class<?>) stringListType.getActualTypeArguments()[0];
    }

    private static String getName(@NonNull Field fld, @NonNull Header header) {
        if (header.name() == null || header.name().trim().isEmpty())
            return fld.getName();
        return header.name();
    }

    private static String getName(@NonNull Field fld, @NonNull Body body) {
        if (body.name() == null || body.name().trim().isEmpty())
            return fld.getName();
        return body.name();
    }

    @Nullable
    public static <T> T[] convertArray(@NonNull Class<T> cls, @NonNull Response response) {
        final String contentType = response.contentType();
        if (response.asBytes() == null || contentType == null ||
                (!contentType.startsWith("application/json") &&
                        !contentType.startsWith("text/plain"))) {
            throw new RuntimeException("Unexpected Content-Type for Object conversion: " + contentType);
        }
        final JSONArray responseBody;
        try {
            responseBody = response.asJsonArray();
            if (responseBody == null)
                throw new RuntimeException("Response body is null.");
        } catch (BridgeException e) {
            throw new RuntimeException("Failed to convert response body to JSONArray: " + e.getMessage(), e);
        }
        //noinspection unchecked
        return (T[]) convertArray(cls, responseBody);
    }

    @Nullable
    public static Object convertArray(@NonNull Class<?> cls, @Nullable JSONArray responseBody) {
        if (responseBody == null)
            return null;
        else if (responseBody.length() == 0)
            return Array.newInstance(cls, 0);
        try {
            if (isShort(cls)) {
                short[] array = (short[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = ((Integer) responseBody.getInt(i)).shortValue();
                return array;
            } else if (isInteger(cls)) {
                int[] array = (int[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = responseBody.getInt(i);
                return array;
            } else if (isLong(cls)) {
                long[] array = (long[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = responseBody.getLong(i);
                return array;
            } else if (isFloat((cls))) {
                float[] array = (float[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = (float) responseBody.getDouble(i);
                return array;
            } else if (isDouble(cls)) {
                double[] array = (double[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = responseBody.getDouble(i);
                return array;
            } else if (isBoolean(cls)) {
                boolean[] array = (boolean[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++) {
                    Object val = responseBody.get(i);
                    if (val instanceof Integer)
                        array[i] = (Integer) val == 1;
                    else array[i] = (Boolean) val;
                }
                return array;
            } else if (isString(cls)) {
                String[] array = (String[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = responseBody.getString(i);
                return array;
            } else {
                Object[] array = (Object[]) Array.newInstance(cls, responseBody.length());
                for (int i = 0; i < responseBody.length(); i++)
                    array[i] = convert(array[i], cls, responseBody.getJSONObject(i), null);
                return array;
            }
        } catch (JSONException e) {
            throw new RuntimeException(String.format("Failed to process array value of type %s: %s",
                    cls.getName(), e.getMessage()), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static ArrayList convertList(@NonNull Class<?> cls, @Nullable JSONArray responseBody) {
        if (responseBody == null) return null;
        final ArrayList list = new ArrayList(responseBody.length());
        if (responseBody.length() == 0) return list;
        try {
            if (isShort(cls)) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(((Integer) responseBody.getInt(i)).shortValue());
            } else if (isInteger(cls)) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(responseBody.getInt(i));
            } else if (isLong(cls)) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(responseBody.getLong(i));
            } else if (isFloat((cls))) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add((float) responseBody.getDouble(i));
            } else if (isDouble(cls)) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(responseBody.getDouble(i));
            } else if (isBoolean(cls)) {
                for (int i = 0; i < responseBody.length(); i++) {
                    Object val = responseBody.get(i);
                    if (val instanceof Integer)
                        list.add((Integer) val == 1);
                    else list.add(val);
                }
            } else if (isString(cls)) {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(responseBody.getString(i));
            } else {
                for (int i = 0; i < responseBody.length(); i++)
                    list.add(convert(null, cls, responseBody.getJSONObject(i), null));
            }
            return list;
        } catch (JSONException e) {
            throw new RuntimeException(String.format("Failed to process array value of type %s: %s",
                    cls.getName(), e.getMessage()), e);
        }
    }

    @Nullable
    public static <T> T convert(@NonNull Class<T> cls, @NonNull Response response) {
        final String contentType = response.contentType();
        if (response.asBytes() == null || contentType == null ||
                (!contentType.startsWith("application/json") &&
                        !contentType.startsWith("text/plain"))) {
            return null;
        }

        final JSONObject responseBody;
        try {
            responseBody = response.asJsonObject();
            if (responseBody == null)
                throw new RuntimeException("Response body is null.");
        } catch (BridgeException e) {
            throw new RuntimeException("Failed to convert response body to JSONObject: " + e.getMessage(), e);
        }

        //noinspection unchecked
        return (T) convert(null, cls, responseBody, response.headers());
    }

    @SuppressWarnings({"RedundantCast", "ConstantConditions"})
    @Nullable
    public static Object convert(@Nullable Object object, @NonNull Class<?> cls, @NonNull JSONObject responseBody, @Nullable Map<String, List<String>> headers) {
        if (object == null)
            object = newInstance(cls);
        final List<Field> fields = getAllFields(cls);

        for (Field fld : fields) {
            fld.setAccessible(true);
            final Class<?> fieldType = fld.getType();
            if (headers != null) {

                final Header headerAnnotation = fld.getAnnotation(Header.class);
                if (headerAnnotation != null) {
                    final String headerName = getName(fld, headerAnnotation);
                    final String headerValue = headers.get(headerName).get(0);

                    try {
                        if (headerValue == null) {
                            fld.set(object, null);
                        } else if (isShort(fieldType)) {
                            fld.set(object, Short.parseShort(headerValue));
                        } else if (isInteger(fieldType)) {
                            fld.set(object, Integer.parseInt(headerValue));
                        } else if (isLong((fieldType))) {
                            fld.set(object, Long.parseLong(headerValue));
                        } else if (isFloat(fieldType)) {
                            fld.set(object, Float.parseFloat(headerValue));
                        } else if (isDouble(fieldType)) {
                            fld.set(object, Double.parseDouble(headerValue));
                        } else if (isBoolean(fieldType)) {
                            fld.set(object, Boolean.parseBoolean(headerValue));
                        } else if (isString(fieldType)) {
                            fld.set(object, headerValue);
                        } else {
                            throw new RuntimeException("Header annotations can only be used on primitive types (e.g. strings, shorts, integers, longs, booleans, floats, doubles).");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Failed to load header %s (value = %s) into class field %s (of type %s): %s",
                                headerName, headerValue, fld.getName(), fld.getType().getName(), e.getMessage()), e);
                    }
                }
            }

            final Body bodyAnnotation = fld.getAnnotation(Body.class);
            if (bodyAnnotation != null) {
                final String bodyName = getName(fld, bodyAnnotation);
                try {
                    final Object bodyValue = responseBody.isNull(bodyName) ? null : responseBody.get(bodyName);
                    if (isShort(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, (short) 0);
                        else if (bodyValue instanceof Short)
                            fld.set(object, bodyValue);
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isInteger(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, 0);
                        else if (bodyValue instanceof Integer)
                            fld.set(object, bodyValue);
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isLong(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, 0L);
                        else if (bodyValue instanceof Long)
                            fld.set(object, bodyValue);
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isFloat(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, 0f);
                        else if (bodyValue instanceof Double)
                            fld.set(object, ((Double) bodyValue).floatValue());
                        else if (bodyValue instanceof Float)
                            fld.set(object, bodyValue);
                        else if (bodyValue instanceof Integer)
                            fld.set(object, ((Integer) bodyValue).floatValue());
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isDouble(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, 0d);
                        else if (bodyValue instanceof Float)
                            fld.set(object, ((Float) bodyValue).doubleValue());
                        else if (bodyValue instanceof Double)
                            fld.set(object, bodyValue);
                        else if (bodyValue instanceof Integer)
                            fld.set(object, ((Integer) bodyValue).doubleValue());
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isBoolean(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, false);
                        else if (bodyValue instanceof Integer)
                            fld.set(object, (Integer) bodyValue == 1);
                        else if (bodyValue instanceof Boolean)
                            fld.set(object, bodyValue);
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isString(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, null);
                        else if (bodyValue instanceof String)
                            fld.set(object, bodyValue);
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isArray(fieldType)) {
                        if (bodyValue == null)
                            fld.set(object, null);
                        else if (bodyValue instanceof JSONArray)
                            fld.set(object, convertArray(fieldType.getComponentType(), (JSONArray) bodyValue));
                        else
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                    } else if (isArrayList(fieldType)) {
                        if (bodyValue == null) {
                            fld.set(object, null);
                        } else if (bodyValue instanceof JSONArray) {
                            final Class<?> genericType = getArrayListType(fld);
                            fld.set(object, convertList(genericType, (JSONArray) bodyValue));
                        } else {
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                        }
                    } else {
                        if (bodyValue == null)
                            fld.set(object, null);
                        else if (bodyValue instanceof JSONObject) {
                            fld.set(object, convert(null, fld.getType(), (JSONObject) bodyValue, null));
                        } else {
                            throw new Exception("Unexpected JSON value type " + bodyValue.getClass().getName());
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Failed to load JSON field %s into class field %s (of type %s): %s",
                            bodyName, fld.getName(), fld.getType().getName(), e.getMessage()), e);
                }
            }
        }

        return object;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(@NonNull Class<T> cls) {
        final Constructor ctor = getDefaultConstructor(cls);
        try {
            return (T) ctor.newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Failed to instantiate " + cls.getName() + ": " + t.getLocalizedMessage());
        }
    }

    public static Constructor<?> getDefaultConstructor(@NonNull Class<?> cls) {
        final Constructor[] ctors = cls.getDeclaredConstructors();
        Constructor ctor = null;
        for (Constructor ct : ctors) {
            ctor = ct;
            if (ctor.getGenericParameterTypes().length == 0)
                break;
        }
        if (ctor == null)
            throw new IllegalStateException("No default constructor found for " + cls.getName());
        ctor.setAccessible(true);
        return ctor;
    }
}