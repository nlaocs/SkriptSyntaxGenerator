package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class LegacyReflection {
    private LegacyReflection() {
    }

    static Class<?> classOrNull(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    static boolean hasClass(String name, ClassLoader loader) {
        return classOrNull(name, loader) != null;
    }

    static boolean hasMethod(Class<?> type, String name, int parameterCount) {
        return findMethod(type, name, parameterCount) != null;
    }

    static Object invokeStatic(Class<?> type, String name, Object... arguments) {
        return invokeMethod(null, requireMethod(type, name, arguments), arguments);
    }

    static Object invoke(Object target, String name, Object... arguments) {
        if (target == null) return null;
        return invokeMethod(target, requireMethod(target.getClass(), name, arguments), arguments);
    }

    static Object invokeOrNull(Object target, String name, Object... arguments) {
        try {
            return invoke(target, name, arguments);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static Object invokeStaticOrNull(Class<?> type, String name, Object... arguments) {
        try {
            return invokeStatic(type, name, arguments);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static Object field(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target instanceof Class<?> ? null : target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot read field " + name, exception);
            }
        }
        return null;
    }

    static List<Object> list(Object value) {
        if (value == null) return Collections.emptyList();
        List<Object> result = new ArrayList<Object>();
        if (value instanceof Iterator<?>) {
            Iterator<?> iterator = (Iterator<?>) value;
            while (iterator.hasNext()) result.add(iterator.next());
            return result;
        }
        if (value instanceof Iterable<?>) {
            for (Object element : (Iterable<?>) value) result.add(element);
            return result;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                result.add(Array.get(value, index));
            }
            return result;
        }
        if (value instanceof Collection<?>) {
            result.addAll((Collection<?>) value);
            return result;
        }
        result.add(value);
        return result;
    }

    static List<String> strings(Object value) {
        List<String> result = new ArrayList<String>();
        for (Object element : list(value)) {
            if (element != null) {
                String text = String.valueOf(element).trim();
                if (!text.isEmpty()) result.add(text);
            }
        }
        return result;
    }

    static List<Class<?>> classes(Object value) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        for (Object element : list(value)) {
            if (element instanceof Class<?>) result.add((Class<?>) element);
        }
        return result;
    }

    private static Method requireMethod(Class<?> type, String name, Object[] arguments) {
        Method method = findCompatibleMethod(type, name, arguments);
        if (method == null) {
            throw new IllegalStateException(
                "Method not found: " + type.getName() + "." + name + "/" + arguments.length
            );
        }
        return method;
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == parameterCount) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Method findCompatibleMethod(Class<?> type, String name, Object[] arguments) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (isCompatible(method, name, arguments)) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        for (Method method : type.getMethods()) {
            if (isCompatible(method, name, arguments)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static boolean isCompatible(Method method, String name, Object[] arguments) {
        if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < arguments.length; index++) {
            Object argument = arguments[index];
            if (argument == null) continue;
            if (!wrap(parameterTypes[index]).isAssignableFrom(argument.getClass())) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static Object invokeMethod(Object target, Method method, Object[] arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Cannot invoke " + method.getDeclaringClass().getName() + "." + method.getName(),
                exception
            );
        }
    }
}
