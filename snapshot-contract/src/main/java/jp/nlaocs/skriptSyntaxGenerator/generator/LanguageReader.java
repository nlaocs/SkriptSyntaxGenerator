package jp.nlaocs.skriptSyntaxGenerator.generator;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads the effective language registry without depending on a particular Skript API version.
 *
 * <p>Skript keeps its loaded values in private static maps. This reader deliberately copies only
 * those runtime maps; it never opens a script file or walks a script-local language provider.</p>
 */
public final class LanguageReader {
    private static final String LANGUAGE_CLASS = "ch.njol.skript.localization.Language";
    private static final String DEFAULT_LANGUAGE_FIELD = "defaultLanguage";
    private static final String LOCALIZED_LANGUAGE_FIELD = "localizedLanguage";

    private LanguageReader() {
    }

    /**
     * Returns the values visible through Skript's effective language lookup.
     *
     * <p>Skript checks the default map first and the active localized map second. The returned map
     * follows that precedence and is sorted for deterministic JSON output. Missing runtime fields
     * are fatal because an empty map is valid data and cannot also represent collection failure.</p>
     *
     * @param classLoader the class loader that loaded Skript
     * @return a deterministic copy of the effective language key/value pairs
     * @throws IllegalArgumentException if {@code classLoader} is null
     * @throws IllegalStateException if the runtime registry cannot be read exactly
     */
    public static Map<String, String> read(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("Skript class loader cannot be null");
        }
        try {
            Class<?> languageClass = Class.forName(LANGUAGE_CLASS, false, classLoader);
            Map<?, ?> defaults = asMap(
                readField(languageClass, DEFAULT_LANGUAGE_FIELD),
                DEFAULT_LANGUAGE_FIELD,
                false
            );
            Map<?, ?> localized = asMap(
                readField(languageClass, LOCALIZED_LANGUAGE_FIELD),
                LOCALIZED_LANGUAGE_FIELD,
                true
            );
            return effectiveValues(defaults, localized);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            throw new IllegalStateException("Cannot read Skript's runtime language registry", failure);
        }
    }

    /**
     * Merges language maps using the same precedence as {@code Language.get_i}.
     *
     * <p>This is package-visible for contract tests and intentionally accepts untyped maps because
     * the caller is reading fields from version-dependent runtime classes.</p>
     */
    static Map<String, String> effectiveValues(Map<?, ?> defaults, Map<?, ?> localized) {
        TreeMap<String, String> result = new TreeMap<String, String>();
        copyStrings(localized, result);
        copyStrings(defaults, result);
        return Collections.unmodifiableMap(result);
    }

    private static void copyStrings(Map<?, ?> source, Map<String, String> destination) {
        if (source == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                throw new IllegalStateException("Skript language registry contains a non-string entry");
            }
            destination.put((String) entry.getKey(), (String) entry.getValue());
        }
    }

    private static Map<?, ?> asMap(Object value, String fieldName, boolean nullable) {
        if (value == null && nullable) {
            return Collections.emptyMap();
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalStateException(LANGUAGE_CLASS + "." + fieldName + " is not a Map");
        }
        return (Map<?, ?>) value;
    }

    private static Object readField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = findField(type, name);
        if (field == null) {
            throw new NoSuchFieldException(type.getName() + "." + name);
        }
        field.setAccessible(true);
        return field.get(null);
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Continue through the hierarchy for older or wrapped implementations.
            }
        }
        return null;
    }
}
