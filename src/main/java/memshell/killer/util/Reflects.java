package memshell.killer.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public final class Reflects {
    private Reflects() {
    }

    public static Field field(Object obj, String name) throws NoSuchFieldException {
        Class<?> type = obj instanceof Class ? (Class<?>) obj : obj.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static Object get(Object obj, String name) throws Exception {
        return field(obj, name).get(obj instanceof Class ? null : obj);
    }

    public static Object getQuiet(Object obj, String name) {
        try {
            return get(obj, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void set(Object obj, String name, Object value) throws Exception {
        field(obj, name).set(obj instanceof Class ? null : obj, value);
    }

    public static Method method(Object obj, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> type = obj instanceof Class ? (Class<?>) obj : obj.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    public static Object invoke(Object obj, String name, Class<?>[] parameterTypes, Object[] args) throws Exception {
        return method(obj, name, parameterTypes == null ? new Class<?>[0] : parameterTypes)
                .invoke(obj instanceof Class ? null : obj, args == null ? new Object[0] : args);
    }

    public static Object invokeQuiet(Object obj, String name, Class<?>[] parameterTypes, Object[] args) {
        try {
            return invoke(obj, name, parameterTypes, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object invokeAnyQuiet(Object obj, String name, Object... args) {
        if (obj == null) {
            return null;
        }
        Object[] actualArgs = args == null ? new Object[0] : args;
        Class<?> type = obj instanceof Class ? (Class<?>) obj : obj.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != actualArgs.length) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(obj instanceof Class ? null : obj, actualArgs);
                } catch (Throwable ignored) {
                    // Try another overload with the same arity.
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    public static List<Object> asList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Map) {
            return new ArrayList<>(((Map<?, ?>) value).values());
        }
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        if (value instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                list.add(item);
            }
            return list;
        }
        if (value instanceof Enumeration) {
            List<Object> list = new ArrayList<>();
            Enumeration<?> enumeration = (Enumeration<?>) value;
            while (enumeration.hasMoreElements()) {
                list.add(enumeration.nextElement());
            }
            return list;
        }
        if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(value, i));
            }
            return list;
        }
        List<Object> one = new ArrayList<>();
        one.add(value);
        return one;
    }
}
