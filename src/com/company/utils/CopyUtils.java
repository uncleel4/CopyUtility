package com.company.utils;

import com.company.exceptions.ObjectCopyException;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.IntStream;

import static com.company.utils.CommonUtils.isNull;

public class CopyUtils {

    public static <T> T deepCopy(T original) {

        if (isNull(original)) {
            throw new ObjectCopyException("Переданный параметр содержит null");
        }

        Class<?> originalType = resolveClass(original.getClass().getName());

        if (isSimpleType(originalType)) {
            return original;
        } else if (isListType(originalType)) {
            return mirrorListType(original);
        } else {
            return mirrorObject(original);
        }
    }

    private static Class<?> resolveClass(String className) {
        switch (className) {
            case "byte":
            case "java.lang.Byte":
                return byte.class;
            case "short":
            case "java.lang.Short":
                return short.class;
            case "int":
            case "java.lang.Integer":
                return int.class;
            case "long":
            case "java.lang.Long":
                return long.class;
            case "double":
            case "java.lang.Double":
                return double.class;
            case "float":
            case "java.lang.Float":
                return float.class;
            case "char":
            case "java.lang.Character":
                return char.class;
            default:
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new ObjectCopyException("Искомый класс не найден", e);
                }
        }
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return !clazz.isArray() && (clazz.isPrimitive() || clazz == String.class);
    }

    private static boolean isListType(Class<?> clazz) {
        return clazz.isArray() || Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz);
    }

    private static Object getDefaultValue(Class<?> type) {
        return switch (type.getName()) {
            case "boolean" -> false;
            case "char" -> '\0';
            case "byte" -> (byte) 0;
            case "short" -> (short) 0;
            case "int" -> 0;
            case "long" -> 0L;
            case "float" -> 0.0f;
            case "double" -> 0.0d;
            default -> null;
        };
    }

    private static <T> T createObject(T original) {
        Constructor<?>[] constructors = original.getClass().getDeclaredConstructors();
        try {
            // Если есть конструктор без параметров
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterTypes().length == 0) {
                    constructor.setAccessible(true);
                    return (T) constructor.newInstance();
                }
            }

            // Если нет, то...
            Constructor<?> constructor = Arrays.stream(constructors)
                    .min(Comparator.comparingInt(Constructor::getParameterCount))
                    .orElseThrow(() -> new ObjectCopyException("Доступный конструктор не найден"));
            constructor.setAccessible(true);

            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = Arrays.stream(parameterTypes)
                    .map(CopyUtils::getDefaultValue)
                    .toArray();

            return (T) constructor.newInstance(parameters);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new ObjectCopyException("Ошибка в процессе создания объекта", e);
        }
    }

    private static <T> T mirrorObject(T original) {
        try {
            T copiedObject = createObject(original);
            for (Field originalField : original.getClass().getDeclaredFields()) {
                originalField.setAccessible(true);
                Field copiedField = copiedObject.getClass().getDeclaredField(originalField.getName());
                copiedField.setAccessible(true);
                if (isNull(originalField.get(original))) {
                    copiedField.set(copiedObject, null);
                } else if (isSimpleType(resolveClass(originalField.getType().getName()))
                        || originalField.getType().isEnum()) {
                    copiedField.set(copiedObject, originalField.get(original));
                } else if (originalField.getType().isArray()) {
                    copiedField.set(copiedObject, cloneArray(originalField.get(original)));
                } else {
                    copiedField.set(copiedObject, deepCopy(originalField.get(original)));
                }
            }
            return copiedObject;
        } catch (IllegalAccessException e) {
            throw new ObjectCopyException("Ошибка доступа");
        } catch (NoSuchFieldException e) {
            throw new ObjectCopyException("Поле с таким названием не существует");
        }
    }

    private static <T> T mirrorListType(T original) {
        if (original.getClass().isArray()) {
            return cloneArray(original);
        } else if (Collection.class.isAssignableFrom(original.getClass())) {
            return (T) cloneCollection(original);
        } else if (Map.class.isAssignableFrom(original.getClass())) {
            return (T) cloneMap(original);
        } else {
            throw new ObjectCopyException("Незвестный тип объекта");
        }
    }

    private static <T> T cloneArray(T original) {
        int length = Array.getLength(original);
        T copiedArray = (T) Array.newInstance(resolveClass(original.getClass().getComponentType().getTypeName()), length);

        if (isSimpleType(original.getClass())) {
            System.arraycopy(original, 0, copiedArray, 0, length);
        } else {
            IntStream.range(0, length).forEach(i -> Array.set(copiedArray, i, deepCopy(Array.get(original, i))));
        }

        return copiedArray;
    }

    private static <T> Collection<T> cloneCollection(T original) {
        Collection<T> collection = (Collection<T>) original;
        Collection<T> copiedCollection = createObject(collection);
        collection.stream()
                .map(e -> isSimpleType(e.getClass()) ? e : deepCopy(e))
                .forEach(copiedCollection::add);
        return copiedCollection;
    }

    private static <V, K, T> Map<K, V> cloneMap(T original) {
        Map<K, V> map = (Map<K, V>) original;
        Map<K, V> copiedMap = createObject(map);

        map.forEach((key, value) -> {
            K copiedKey = isSimpleType(key.getClass()) ? key :
                    isListType(key.getClass()) ? mirrorListType(key) :
                            deepCopy(key);
            V copiedValue = isSimpleType(value.getClass()) ? value :
                    isListType(value.getClass()) ? mirrorListType(value) :
                            deepCopy(value);
            copiedMap.put(copiedKey, copiedValue);
        });

        return copiedMap;
    }
}