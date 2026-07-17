package jp.nlaocs.skriptSyntaxGenerator.generator;

import jp.nlaocs.skriptSyntaxGenerator.data.AliasItemData;
import jp.nlaocs.skriptSyntaxGenerator.data.AliasSnapshotData;
import jp.nlaocs.skriptSyntaxGenerator.data.AliasTargetData;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class GlobalAliasesReader {
    private static final String ALIASES_CLASS = "ch.njol.skript.aliases.Aliases";

    private GlobalAliasesReader() {
    }

    public static boolean isSupported(ClassLoader classLoader) {
        try {
            Class<?> aliasesClass = Class.forName(ALIASES_CLASS, false, classLoader);
            Field provider = findField(aliasesClass, "provider");
            return provider != null && findField(provider.getType(), "aliases") != null;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static AliasSnapshotData read(ClassLoader classLoader) {
        try {
            Class<?> aliasesClass = Class.forName(ALIASES_CLASS, true, classLoader);
            Object provider = readField(null, requireField(aliasesClass, "provider"));
            Object rawAliases = readField(provider, requireField(provider.getClass(), "aliases"));
            if (!(rawAliases instanceof Map<?, ?>)) {
                throw new IllegalStateException("Global AliasesProvider.aliases is not a Map");
            }

            TreeMap<String, Object> sortedAliases = new TreeMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawAliases).entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    sortedAliases.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            Map<String, Integer> aliases = new LinkedHashMap<String, Integer>();
            List<AliasTargetData> targets = new ArrayList<AliasTargetData>();
            Map<AliasTargetData, Integer> targetIndexes = new LinkedHashMap<AliasTargetData, Integer>();
            IdentityHashMap<Object, AliasTargetData> snapshots = new IdentityHashMap<Object, AliasTargetData>();
            for (Map.Entry<String, Object> entry : sortedAliases.entrySet()) {
                AliasTargetData target = snapshots.get(entry.getValue());
                if (target == null) {
                    target = target(provider, entry.getValue());
                    snapshots.put(entry.getValue(), target);
                }
                Integer index = targetIndexes.get(target);
                if (index == null) {
                    index = targets.size();
                    targetIndexes.put(target, index);
                    targets.add(target);
                }
                aliases.put(entry.getKey(), index);
            }
            return new AliasSnapshotData(aliases, targets);
        } catch (ClassNotFoundException exception) {
            return new AliasSnapshotData(
                Collections.<String, Integer>emptyMap(),
                Collections.<AliasTargetData>emptyList()
            );
        }
    }

    private static AliasTargetData target(Object provider, Object itemType) {
        int amount = intValue(invoke(itemType, "getAmount"), 1);
        boolean all = booleanValue(invoke(itemType, "isAll"), false);
        List<AliasItemData> types = new ArrayList<AliasItemData>();
        for (Object itemData : list(invoke(itemType, "getTypes"))) {
            if (itemData != null) types.add(item(provider, itemData));
        }
        return new AliasTargetData(amount, all, types);
    }

    private static AliasItemData item(Object provider, Object itemData) {
        Object materialValue = invoke(itemData, "getType");
        String material = materialValue instanceof Enum<?>
            ? ((Enum<?>) materialValue).name()
            : String.valueOf(materialValue);
        Object minecraftIdValue = invokeOrNull(provider, "getMinecraftId", itemData);
        Object blockValuesValue = invokeOrNull(itemData, "getBlockValues");

        return new AliasItemData(
            material,
            minecraftIdValue == null ? null : String.valueOf(minecraftIdValue),
            intValue(invokeOrNull(itemData, "getDurability"), 0),
            booleanValue(invokeOrNull(itemData, "isPlain"), false),
            booleanValue(invokeOrNull(itemData, "isAlias"), false),
            blockValues(blockValuesValue),
            itemMeta(itemData)
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> itemMeta(Object itemData) {
        Object stack = invokeOrNull(itemData, "getStack");
        if (stack == null || !booleanValue(invokeOrNull(stack, "hasItemMeta"), false)) return null;
        Object meta = invokeOrNull(stack, "getItemMeta");
        Object serialized = invokeOrNull(meta, "serialize");
        Object normalized = normalize(serialized, new IdentityHashMap<Object, Boolean>());
        return normalized instanceof Map<?, ?> ? (Map<String, Object>) normalized : null;
    }

    private static Object blockValues(Object value) {
        if (value == null) return null;
        Object serialized = invokeOrNull(value, "serialize");
        return normalize(
            serialized == null ? value : serialized,
            new IdentityHashMap<Object, Boolean>()
        );
    }

    private static Object normalize(Object value, IdentityHashMap<Object, Boolean> path) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?>) return ((Enum<?>) value).name();
        if (path.put(value, Boolean.TRUE) != null) return unresolvedValue(value, "cycle");
        try {
            if (value.getClass().getName().equals("ch.njol.yggdrasil.Fields")) {
                Map<String, Object> result = new TreeMap<String, Object>();
                for (Object context : list(value)) {
                    String id = String.valueOf(invoke(context, "getID"));
                    boolean primitive = booleanValue(invoke(context, "isPrimitive"), false);
                    Object fieldValue = invokeOrNull(context, primitive ? "getPrimitive" : "getObject");
                    result.put(id, normalize(fieldValue, path));
                }
                return new LinkedHashMap<String, Object>(result);
            }
            if (value instanceof Map<?, ?>) {
                List<Map.Entry<?, ?>> entries = new ArrayList<Map.Entry<?, ?>>(((Map<?, ?>) value).entrySet());
                Collections.sort(entries, new Comparator<Map.Entry<?, ?>>() {
                    @Override
                    public int compare(Map.Entry<?, ?> first, Map.Entry<?, ?> second) {
                        return String.valueOf(first.getKey()).compareTo(String.valueOf(second.getKey()));
                    }
                });
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                for (Map.Entry<?, ?> entry : entries) {
                    result.put(String.valueOf(entry.getKey()), normalize(entry.getValue(), path));
                }
                return result;
            }
            if (value instanceof Iterable<?>) {
                List<Object> result = new ArrayList<Object>();
                for (Object element : (Iterable<?>) value) result.add(normalize(element, path));
                return result;
            }
            if (value.getClass().isArray()) {
                List<Object> result = new ArrayList<Object>();
                for (int index = 0; index < Array.getLength(value); index++) {
                    result.add(normalize(Array.get(value, index), path));
                }
                return result;
            }
            Object serialized = invokeOrNull(value, "serialize");
            return serialized == null || serialized == value
                ? unresolvedValue(value, "unresolved")
                : normalize(serialized, path);
        } finally {
            path.remove(value);
        }
    }

    private static Map<String, Object> unresolvedValue(Object value, String state) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("type", value.getClass().getName());
        result.put("state", state);
        return result;
    }
    private static List<Object> list(Object value) {
        if (value == null) return Collections.emptyList();
        List<Object> result = new ArrayList<Object>();
        if (value instanceof Iterable<?>) {
            for (Object element : (Iterable<?>) value) result.add(element);
        } else if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) result.add(Array.get(value, index));
        } else {
            result.add(value);
        }
        return result;
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static Object invoke(Object target, String name, Object... arguments) {
        Method method = findMethod(target.getClass(), name, arguments);
        if (method == null) {
            throw new IllegalStateException("Method not found: " + target.getClass().getName() + "." + name);
        }
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception.getCause());
        }
    }

    private static Object invokeOrNull(Object target, String name, Object... arguments) {
        if (target == null) return null;
        Method method = findMethod(target.getClass(), name, arguments);
        if (method == null) return null;
        try {
            method.setAccessible(true);
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException ignored) {
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
        if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) return false;
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

    private static Field requireField(Class<?> type, String name) {
        Field field = findField(type, name);
        if (field == null) throw new IllegalStateException("Field not found: " + type.getName() + "." + name);
        return field;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Continue through the hierarchy.
            }
        }
        return null;
    }

    private static Object readField(Object target, Field field) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read " + field, exception);
        }
    }
}