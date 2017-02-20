package com.afollestad.bridge.conversion.base;

import com.afollestad.bridge.annotations.Header;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class Converter {

    protected String getHeaderName(@NotNull Field fld, @NotNull Header header) {
        if (header.name() == null || header.name().trim().isEmpty())
            return fld.getName();
        return header.name();
    }

    protected List<Field> getAllFields(@NotNull Class<?> cls) {
        final List<Field> fields = new ArrayList<>(Arrays.asList(cls.getDeclaredFields()));
        while (cls.getSuperclass() != null) {
            cls = cls.getSuperclass();
            Collections.addAll(fields, cls.getDeclaredFields());
        }
        return fields;
    }

    public static final int FIELD_OTHER = -1;
    public static final int FIELD_SHORT = 1;
    public static final int FIELD_INTEGER = 2;
    public static final int FIELD_LONG = 3;
    public static final int FIELD_FLOAT = 4;
    public static final int FIELD_DOUBLE = 5;
    public static final int FIELD_BOOLEAN = 6;
    public static final int FIELD_STRING = 7;

    protected int getFieldType(@NotNull Class<?> cls) {
        if (cls == short.class || cls == Short.class)
            return FIELD_SHORT;
        else if (cls == int.class || cls == Integer.class)
            return FIELD_INTEGER;
        else if (cls == long.class || cls == Long.class)
            return FIELD_LONG;
        else if (cls == float.class || cls == Float.class)
            return FIELD_FLOAT;
        else if (cls == double.class || cls == Double.class)
            return FIELD_DOUBLE;
        else if (cls == boolean.class || cls == Boolean.class)
            return FIELD_BOOLEAN;
        else if (cls == String.class)
            return FIELD_STRING;
        return FIELD_OTHER;
    }

    protected boolean isPrimitive(@NotNull Class<?> cls) {
        return getFieldType(cls) != FIELD_OTHER;
    }

    protected boolean isArray(@NotNull Class<?> fieldType) {
        return fieldType.isArray();
    }

    protected boolean isArrayList(@NotNull Class<?> fieldType) {
        return fieldType == List.class || fieldType == ArrayList.class;
    }

    protected Class<?> getArrayListType(@NotNull Field field) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        return (Class<?>) stringListType.getActualTypeArguments()[0];
    }
}