package com.afollestad.bridge.conversion.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.RequestBuilder;
import com.afollestad.bridge.annotations.ContentType;
import com.afollestad.bridge.annotations.Header;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class RequestConverter<ObjectType, ArrayType> extends Converter {

    @NonNull
    public static String getContentType(@NonNull Class<?> forClass, @Nullable Object defaultType) {
        ContentType contentTypeAnnotation = forClass.getAnnotation(ContentType.class);
        if (contentTypeAnnotation != null && contentTypeAnnotation.value() != null && !contentTypeAnnotation.value().trim().isEmpty())
            return contentTypeAnnotation.value();
        if (defaultType == null || !(defaultType instanceof String) || ((String) defaultType).trim().isEmpty())
            defaultType = "application/json";
        return (String) defaultType;
    }

    public final byte[] convertObject(@NonNull Object object, @NonNull RequestBuilder request) {
        try {
            onPrepare(request, object);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to prepare RequestConverter for object of class %s: %s",
                    object.getClass().getName(), e.getMessage()), e);
        }
        ObjectType output = processObject(object, request);
        try {
            return onFinish(output, request, object);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to finish RequestConverter for object of class %s: %s",
                    object.getClass().getName(), e.getMessage()), e);
        }
    }

    public final byte[] convertList(@NonNull List list, @NonNull RequestBuilder request) {
        Object[] array = list.toArray();
        return convertArray(array, request);
    }

    public final byte[] convertArray(@NonNull Object[] objects, @NonNull RequestBuilder request) {
        try {
            onPrepare(request, objects);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to prepare RequestConverter for objects (array) of class %s: %s",
                    objects[0].getClass().getName(), e.getMessage()), e);
        }
        final ArrayType output = createOutputArray();
        for (Object obj : objects) {
            ObjectType element = processObject(obj, request);
            try {
                onAttachValueToArray(output, element, FIELD_OTHER);
            } catch (Exception e) {
                throw new RuntimeException("Failed to attach value to output array: " + e.getMessage(), e);
            }
        }
        try {
            return onFinish(output, request, objects);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to finish RequestConverter for objects (array) of class %s: %s",
                    objects[0].getClass().getName(), e.getMessage()), e);
        }
    }

    private ObjectType processObject(@NonNull Object object, @NonNull RequestBuilder request) {
        final List<Field> fields = getAllFields(object.getClass());
        final ObjectType output = createOutputObject();

        for (Field field : fields) {
            field.setAccessible(true);
            @FieldType
            final int fieldType = getFieldType(field.getType());
            final Object fieldValue;
            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Failed to get the value of field %s (type %s): %s",
                        field.getName(), field.getType().getName(), e.getMessage()), e);
            }

            Header headerAnnotation = field.getAnnotation(Header.class);
            if (headerAnnotation != null)
                request.header(getHeaderName(field, headerAnnotation), fieldValue);

            boolean canConvert;
            try {
                canConvert = canConvertField(field);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to check if field %s can be converted by %s: %s",
                        field.getName(), getClass().getName(), e.getMessage()), e);
            }

            if (canConvert) {
                final String name;
                try {
                    name = getFieldOutputName(field);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Failed to get output name for field %s of type %s: %s",
                            field.getName(), field.getType().getName(), e.getMessage()), e);
                }
                try {
                    if (isPrimitive(field.getType())) {
                        onAttachValueToObject(name, output, fieldValue, fieldType);
                    } else if (isArray(field.getType())) {
                        processArray(output, fieldValue, field, fieldType, request);
                    } else if (isArrayList(field.getType())) {
                        processList(output, fieldValue, field, fieldType, request);
                    } else {
                        ObjectType child = processObject(fieldValue, request);
                        onAttachObjectToParent(name, child, output);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to attach " + name);
                }
            }
        }

        return output;
    }

    private void processArray(@NonNull ObjectType parent, @NonNull Object fieldValue, @NonNull Field field, @FieldType int fieldType, @NonNull RequestBuilder request) throws Exception {
        final ArrayType array = createOutputArray();
        final Class<?> elementType = field.getType().getComponentType();
        final int arrayLength = Array.getLength(fieldValue);

        for (int i = 0; i < arrayLength; i++) {
            final Object fieldArrayValue = Array.get(fieldValue, i);
            if (isPrimitive(elementType)) {
                onAttachValueToArray(array, fieldArrayValue, fieldType);
            } else if (isArray(elementType) || isArrayList(elementType)) {
                throw new IllegalStateException("2D arrays/lists are currently not supported for request conversion.");
            } else {
                final ObjectType arrayElement = processObject(fieldArrayValue, request);
                onAttachValueToArray(array, arrayElement, fieldType);
            }
        }

        onAttachArrayToParent(getFieldOutputName(field), array, parent);
    }

    private void processList(@NonNull ObjectType parent, @NonNull Object fieldValue, @NonNull Field field, @FieldType int fieldType, @NonNull RequestBuilder request) throws Exception {
        final ArrayType array = createOutputArray();
        final Class<?> elementType = getArrayListType(field);
        final List list = (List) fieldValue;

        for (int i = 0; i < list.size(); i++) {
            final Object fieldArrayValue = list.get(i);
            if (isPrimitive(elementType)) {
                onAttachValueToArray(array, fieldArrayValue, fieldType);
            } else if (isArray(elementType) || isArrayList(elementType)) {
                throw new IllegalStateException("2D arrays/lists are currently not supported for request conversion.");
            } else {
                final ObjectType arrayElement = processObject(fieldArrayValue, request);
                onAttachValueToArray(array, arrayElement, fieldType);
            }
        }

        onAttachArrayToParent(getFieldOutputName(field), array, parent);
    }

    public abstract void onPrepare(@NonNull RequestBuilder request, @NonNull Object object) throws Exception;

    public abstract void onPrepare(@NonNull RequestBuilder request, @NonNull Object[] objects) throws Exception;

    public abstract ObjectType createOutputObject();

    public abstract ArrayType createOutputArray();

    public abstract boolean canConvertField(@NonNull Field field) throws Exception;

    @NonNull
    public abstract String getFieldOutputName(@NonNull Field field) throws Exception;

    public abstract void onAttachValueToObject(@NonNull String name, @NonNull ObjectType object, @NonNull Object value, @FieldType int fieldType) throws Exception;

    public abstract void onAttachValueToArray(@NonNull ArrayType array, @NonNull Object value, @FieldType int fieldType) throws Exception;

    public abstract void onAttachObjectToParent(@NonNull String name, @NonNull ObjectType object, @NonNull ObjectType parent) throws Exception;

    public abstract void onAttachArrayToParent(@NonNull String name, @NonNull ArrayType array, @NonNull ObjectType parent) throws Exception;

    @Nullable
    public abstract byte[] onFinish(@NonNull ObjectType output, @NonNull RequestBuilder request, @NonNull Object object) throws Exception;

    @Nullable
    public abstract byte[] onFinish(@NonNull ArrayType output, @NonNull RequestBuilder request, @NonNull Object[] objects) throws Exception;
}