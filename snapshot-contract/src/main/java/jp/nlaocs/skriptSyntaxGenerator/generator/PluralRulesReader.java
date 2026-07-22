package jp.nlaocs.skriptSyntaxGenerator.generator;

import jp.nlaocs.skriptSyntaxGenerator.data.PluralAlgorithm;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralOverrideRegistration;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleAddonData;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleData;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleOrigin;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRulesData;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PluralRulesReader {
    private static final String UTILS_CLASS = "ch.njol.skript.util.Utils";

    private PluralRulesReader() {
    }

    public static PluralRulesData read(
        ClassLoader classLoader,
        PluralRuleAddonData skriptAddon,
        List<PluralOverrideRegistration> overrides
    ) {
        try {
            Class<?> utilsClass = Class.forName(UTILS_CLASS, true, classLoader);
            Field plurals = requireField(utilsClass, "plurals");
            Object rawRules = plurals.get(null);
            boolean overrideSupported = findMethod(
                utilsClass,
                "addPluralOverride",
                String.class,
                String.class
            ) != null;
            return readRaw(rawRules, overrideSupported, skriptAddon, overrides);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Skript Utils class is missing", exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read Skript plural rules", exception);
        }
    }

    static PluralRulesData readRaw(
        Object rawRules,
        boolean overrideSupported,
        PluralRuleAddonData skriptAddon,
        List<PluralOverrideRegistration> overrides
    ) {
        if (rawRules == null || skriptAddon == null || overrides == null) {
            throw new IllegalArgumentException("Plural rule input cannot be null");
        }

        List<Object> entries = list(rawRules);
        if (entries.isEmpty()) {
            throw new IllegalStateException("Skript plural rule table is empty");
        }

        List<RawRule> rules = new ArrayList<RawRule>();
        PluralAlgorithm algorithm;
        if (entries.get(0) instanceof String[]) {
            algorithm = PluralAlgorithm.LEGACY_FIRST_MATCH;
            for (Object entry : entries) {
                if (!(entry instanceof String[]) || ((String[]) entry).length < 2) {
                    throw new IllegalStateException("Legacy Skript plural rule has an invalid shape");
                }
                String[] pair = (String[]) entry;
                rules.add(new RawRule(pair[0], pair[1], null));
            }
        } else {
            algorithm = PluralAlgorithm.SINGULAR_AWARE;
            for (Object entry : entries) {
                rules.add(new RawRule(
                    stringValue(invoke(entry, "singular"), "singular"),
                    stringValue(invoke(entry, "plural"), "plural"),
                    booleanValue(invoke(entry, "isCompleteWord"), "isCompleteWord")
                ));
            }
        }

        List<PluralOverrideRegistration> effectiveOverrides =
            new ArrayList<PluralOverrideRegistration>(overrides);
        Collections.sort(effectiveOverrides, new Comparator<PluralOverrideRegistration>() {
            @Override
            public int compare(PluralOverrideRegistration first, PluralOverrideRegistration second) {
                return Integer.compare(second.getRegistrationOrder(), first.getRegistrationOrder());
            }
        });
        if (!overrideSupported && !effectiveOverrides.isEmpty()) {
            throw new IllegalStateException("Plural overrides were captured for a runtime without override support");
        }
        if (effectiveOverrides.size() > rules.size()) {
            throw new IllegalStateException("Captured plural overrides exceed the runtime rule table");
        }

        List<PluralRuleData> result = new ArrayList<PluralRuleData>();
        for (int index = 0; index < rules.size(); index++) {
            RawRule rule = rules.get(index);
            if (index < effectiveOverrides.size()) {
                PluralOverrideRegistration override = effectiveOverrides.get(index);
                if (!rule.singular.equals(override.getSingular()) || !rule.plural.equals(override.getPlural())) {
                    throw new IllegalStateException(
                        "Captured plural override order does not match the runtime rule table at index " + index
                    );
                }
                if (override.getAddon() == null) {
                    throw new IllegalStateException(
                        "Cannot resolve the addon for plural override " + rule.singular + " -> " + rule.plural
                    );
                }
                result.add(new PluralRuleData(
                    index,
                    rule.singular,
                    rule.plural,
                    rule.completeWord,
                    PluralRuleOrigin.OVERRIDE,
                    override.getRegistrationOrder(),
                    override.getAddon()
                ));
            } else {
                result.add(new PluralRuleData(
                    index,
                    rule.singular,
                    rule.plural,
                    rule.completeWord,
                    PluralRuleOrigin.BUILT_IN,
                    null,
                    skriptAddon
                ));
            }
        }

        return new PluralRulesData(algorithm, overrideSupported, result);
    }

    private static List<Object> list(Object value) {
        List<Object> result = new ArrayList<Object>();
        if (value instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) value) result.add(item);
            return result;
        }
        if (value.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(value); index++) {
                result.add(Array.get(value, index));
            }
            return result;
        }
        throw new IllegalStateException("Unsupported Skript plural rule container: " + value.getClass().getName());
    }

    private static String stringValue(Object value, String field) {
        if (!(value instanceof String)) {
            throw new IllegalStateException("Skript plural rule " + field + " is not a String");
        }
        return (String) value;
    }

    private static Boolean booleanValue(Object value, String field) {
        if (!(value instanceof Boolean)) {
            throw new IllegalStateException("Skript plural rule " + field + " is not a boolean");
        }
        return (Boolean) value;
    }

    private static Object invoke(Object target, String name) {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            throw new IllegalStateException("Method not found: " + target.getClass().getName() + "." + name);
        }
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Cannot invoke " + method, exception.getCause());
        }
    }

    private static Field requireField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Continue through the hierarchy.
            }
        }
        throw new IllegalStateException("Field not found: " + type.getName() + "." + name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Continue through the hierarchy.
            }
        }
        return null;
    }

    private static final class RawRule {
        private final String singular;
        private final String plural;
        private final Boolean completeWord;

        private RawRule(String singular, String plural, Boolean completeWord) {
            this.singular = singular;
            this.plural = plural;
            this.completeWord = completeWord;
        }
    }
}
