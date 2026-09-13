package jp.nlaocs.skriptSyntaxGenerator.generator;

import jp.nlaocs.skriptSyntaxGenerator.data.BlockDataBlockData;
import jp.nlaocs.skriptSyntaxGenerator.data.BlockDataFailureData;
import jp.nlaocs.skriptSyntaxGenerator.data.BlockDataSnapshotData;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class BlockDataReader {
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String MATERIAL_CLASS = "org.bukkit.Material";
    private static final String BLOCK_DATA_CLASS = "org.bukkit.block.data.BlockData";
    private static final String PROVIDER = "bukkit-runtime-registry";

    private BlockDataReader() {
    }

    public static BlockDataSnapshotData read(ClassLoader classLoader) {
        try {
            Class<?> bukkit = Class.forName(BUKKIT_CLASS, false, classLoader);
            Class<?> material = Class.forName(MATERIAL_CLASS, false, classLoader);
            Class<?> blockData = Class.forName(BLOCK_DATA_CLASS, false, classLoader);
            if (findStaticMethod(bukkit, "createBlockData", String.class) == null) {
                return unsupported();
            }
            return read(bukkit, material, blockData);
        } catch (ClassNotFoundException ignored) {
            return unsupported();
        } catch (LinkageError error) {
            return unresolved(errorMessage(error));
        }
    }

    static BlockDataSnapshotData read(
        Class<?> bukkitClass,
        Class<?> materialClass,
        Class<?> blockDataClass
    ) {
        Object[] constants = materialClass.getEnumConstants();
        if (constants == null) return unresolved("Bukkit Material is not an enum");

        TreeMap<String, Object> materials = new TreeMap<String, Object>();
        for (Object material : constants) {
            if (!Boolean.TRUE.equals(invokeOrNull(material, "isBlock"))) continue;
            if (Boolean.TRUE.equals(invokeOrNull(material, "isLegacy"))) continue;
            materials.put(materialId(material), material);
        }

        Map<String, BlockDataBlockData> blocks = new LinkedHashMap<String, BlockDataBlockData>();
        List<BlockDataFailureData> failures = new ArrayList<BlockDataFailureData>();
        for (Map.Entry<String, Object> entry : materials.entrySet()) {
            try {
                Object blockData = invokeStatic(bukkitClass, "createBlockData", entry.getKey());
                if (!blockDataClass.isInstance(blockData)) {
                    throw new IllegalStateException("Bukkit returned a non-BlockData value");
                }
                blocks.put(entry.getKey(), block(blockData));
            } catch (RuntimeException exception) {
                failures.add(new BlockDataFailureData(entry.getKey(), errorMessage(exception)));
            }
        }

        if (blocks.isEmpty() && !materials.isEmpty()) {
            return new BlockDataSnapshotData(
                "unresolved",
                false,
                PROVIDER,
                blocks,
                failures.isEmpty()
                    ? Collections.singletonList(new BlockDataFailureData(null, "No BlockData states could be read"))
                    : failures
            );
        }
        return new BlockDataSnapshotData(
            "collected",
            failures.isEmpty(),
            PROVIDER,
            blocks,
            failures
        );
    }

    private static BlockDataBlockData block(Object blockData) {
        Object state = requireInvoke(blockData, "getState");
        Map<?, ?> values = stateValues(state);
        Map<String, List<String>> properties = new LinkedHashMap<String, List<String>>();
        TreeMap<String, Object> sorted = new TreeMap<String, Object>();
        for (Object property : values.keySet()) {
            Object name = invokeOrNull(property, "getName");
            if (name != null) sorted.put(String.valueOf(name), property);
        }
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            Object possibleValues = invokeFirst(entry.getValue(), "getPossibleValues", "getValues");
            if (!(possibleValues instanceof Collection<?>)) {
                throw new IllegalStateException("Cannot read values for BlockData property " + entry.getKey());
            }
            TreeSet<String> names = new TreeSet<String>();
            for (Object value : (Collection<?>) possibleValues) {
                Object name = invokeOrNull(entry.getValue(), "getName", value);
                names.add(name == null
                    ? String.valueOf(value).toLowerCase(Locale.ROOT)
                    : String.valueOf(name));
            }
            properties.put(entry.getKey(), new ArrayList<String>(names));
        }
        Object serialized = requireInvoke(blockData, "getAsString");
        return new BlockDataBlockData(String.valueOf(serialized), properties);
    }

    private static Map<?, ?> stateValues(Object state) {
        Object values = invokeFirst(state, "getValues", "getStateMap");
        if (values instanceof Map<?, ?>) return (Map<?, ?>) values;

        for (Class<?> type = state.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object candidate = field.get(state);
                    if (candidate instanceof Map<?, ?>) return (Map<?, ?>) candidate;
                } catch (ReflectiveOperationException ignored) {
                    // Try the next map field.
                }
            }
        }
        throw new IllegalStateException("Cannot read the Minecraft BlockState property map");
    }

    private static String materialId(Object material) {
        Object key = invokeOrNull(material, "getKey");
        if (key != null) return String.valueOf(key);
        return "minecraft:" + String.valueOf(material).toLowerCase(Locale.ROOT);
    }

    private static BlockDataSnapshotData unsupported() {
        return new BlockDataSnapshotData(
            "unsupported",
            false,
            null,
            Collections.<String, BlockDataBlockData>emptyMap(),
            Collections.<BlockDataFailureData>emptyList()
        );
    }

    private static BlockDataSnapshotData unresolved(String message) {
        return new BlockDataSnapshotData(
            "unresolved",
            false,
            PROVIDER,
            Collections.<String, BlockDataBlockData>emptyMap(),
            Collections.singletonList(new BlockDataFailureData(null, message))
        );
    }

    private static Object requireInvoke(Object target, String name, Object... arguments) {
        Object value = invokeOrNull(target, name, arguments);
        if (value == null) {
            throw new IllegalStateException("Method returned no value: " + target.getClass().getName() + "." + name);
        }
        return value;
    }

    private static Object invokeFirst(Object target, String... names) {
        for (String name : names) {
            Object value = invokeOrNull(target, name);
            if (value != null) return value;
        }
        return null;
    }

    private static Object invokeOrNull(Object target, String name, Object... arguments) {
        Method method = findMethod(target.getClass(), name, arguments);
        if (method == null) return null;
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("Cannot invoke " + method, cause);
        }
    }

    private static Object invokeStatic(Class<?> type, String name, Object... arguments) {
        Method method = findMethod(type, name, arguments);
        if (method == null || !Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("Static method not found: " + type.getName() + "." + name);
        }
        try {
            method.setAccessible(true);
            return method.invoke(null, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("Cannot invoke " + method, cause);
        }
    }

    private static Method findStaticMethod(Class<?> type, String name, Class<?> parameter) {
        try {
            Method method = type.getMethod(name, parameter);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, Object[] arguments) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (compatible(method, name, arguments)) return method;
            }
        }
        for (Method method : type.getMethods()) {
            if (compatible(method, name, arguments)) return method;
        }
        return null;
    }

    private static boolean compatible(Method method, String name, Object[] arguments) {
        if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) {
            return false;
        }
        Class<?>[] parameters = method.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (arguments[index] != null && !wrap(parameters[index]).isAssignableFrom(arguments[index].getClass())) {
                return false;
            }
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

    private static String errorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.trim().isEmpty()
            ? current.getClass().getName()
            : current.getClass().getSimpleName() + ": " + message;
    }
}
