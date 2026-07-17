package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LegacyPropertyCollector {
    private static final String PROPERTY =
        "org.skriptlang.skript.lang.properties.Property";
    private static final String CLASSES =
        "ch.njol.skript.registrations.Classes";
    private static final String CHANGE_MODE =
        "ch.njol.skript.classes.Changer$ChangeMode";

    private final ClassLoader classLoader;
    private final LegacyAddonResolver addonResolver;
    private final LegacyClassHierarchy hierarchy;

    LegacyPropertyCollector(
        ClassLoader classLoader,
        LegacyAddonResolver addonResolver,
        LegacyClassHierarchy hierarchy
    ) {
        this.classLoader = classLoader;
        this.addonResolver = addonResolver;
        this.hierarchy = hierarchy;
    }

    boolean isAvailable() {
        return LegacyReflection.hasClass(PROPERTY, classLoader) &&
            LegacyReflection.hasClass(CLASSES, classLoader);
    }

    List<Map<String, Object>> collect() {
        Class<?> propertyClass = LegacyReflection.classOrNull(PROPERTY, classLoader);
        Class<?> classesClass = LegacyReflection.classOrNull(CLASSES, classLoader);
        if (propertyClass == null || classesClass == null) return Collections.emptyList();

        Set<Object> properties = new LinkedHashSet<Object>();
        Object registry = LegacyReflection.field(propertyClass, "PROPERTY_REGISTRY");
        if (registry != null) {
            properties.addAll(LegacyReflection.list(LegacyReflection.invokeOrNull(registry, "elements")));
        }
        List<Object> classInfos = LegacyReflection.list(
            LegacyReflection.invokeStatic(classesClass, "getClassInfos")
        );
        for (Object classInfo : classInfos) {
            properties.addAll(
                LegacyReflection.list(LegacyReflection.invokeOrNull(classInfo, "getAllProperties"))
            );
        }

        List<Object> ordered = new ArrayList<Object>(properties);
        Collections.sort(ordered, new Comparator<Object>() {
            @Override
            public int compare(Object first, Object second) {
                return propertySortKey(first).compareTo(propertySortKey(second));
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object property : ordered) {
            String name = string(accessor(property, "name", "name"));
            if (name.isEmpty()) continue;
            Class<?> handlerClass = classValue(accessor(property, "handler", "handler"));
            Object provider = accessor(property, "provider", "provider");
            Map<String, Object> addon = providerData(provider, handlerClass);

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("name", name);
            put(data, "documentationId", LegacyReflection.invokeOrNull(property, "getDocumentationID"));
            put(data, "description", accessor(property, "description", "description"));
            putList(data, "since", LegacyReflection.strings(accessor(property, "since", "since")));
            if (handlerClass != null) data.put("handlerClass", className(handlerClass));
            data.put("relatedTypes", relatedTypes(classesClass, property, handlerClass));
            data.put("addon", addon);
            data.put("registrationId", LegacyStableIds.record("property", addon, name));
            result.add(data);
        }
        return result;
    }

    private List<Map<String, Object>> relatedTypes(
        Class<?> classesClass,
        Object property,
        Class<?> declaredHandlerClass
    ) {
        List<Object> classInfos = LegacyReflection.list(
            LegacyReflection.invokeStatic(classesClass, "getClassInfosByProperty", property)
        );
        Collections.sort(classInfos, new Comparator<Object>() {
            @Override
            public int compare(Object first, Object second) {
                return string(LegacyReflection.invokeOrNull(first, "getCodeName"))
                    .compareTo(string(LegacyReflection.invokeOrNull(second, "getCodeName")));
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object classInfo : classInfos) {
            Object propertyInfo = LegacyReflection.invokeOrNull(classInfo, "getPropertyInfo", property);
            Object handler = accessor(propertyInfo, "handler", "handler");
            Class<?> typeClass = classValue(LegacyReflection.invokeOrNull(classInfo, "getC"));
            if (handler == null || typeClass == null) continue;

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("typeCodeName", string(LegacyReflection.invokeOrNull(classInfo, "getCodeName")));
            data.put("typeClass", className(typeClass));

            Object docs = LegacyReflection.invokeOrNull(classInfo, "getPropertyDocumentation", property);
            put(data, "description", accessor(docs, "description", "description"));
            Object docsProvider = accessor(docs, "provider", "provider");
            if (docsProvider != null) data.put("provider", providerData(docsProvider, handler));

            Class<?> handlerClass = handler.getClass();
            if (isHidden(handlerClass) && declaredHandlerClass != null) handlerClass = declaredHandlerClass;
            data.put("handlerClass", className(handlerClass));

            String kind = handlerKind(handler);
            data.put("handlerKind", kind);
            if ("expression".equals(kind) || "typedValue".equals(kind)) {
                addExpressionMetadata(data, handler);
            } else if ("contains".equals(kind)) {
                putList(data, "elementTypes", classNames(
                    LegacyReflection.classes(LegacyReflection.invokeOrNull(handler, "elementTypes"))
                ));
            }
            result.add(data);
        }
        return result;
    }

    private void addExpressionMetadata(Map<String, Object> data, Object handler) {
        try {
            Class<?> returnType = classValue(LegacyReflection.invoke(handler, "returnType"));
            if (returnType == null) throw new IllegalStateException("Property return type is missing");
            data.put("returnType", className(returnType));
            data.put(
                "possibleReturnTypes",
                classNames(LegacyReflection.classes(LegacyReflection.invoke(handler, "possibleReturnTypes")))
            );

            Map<String, Object> acceptedChangers = new LinkedHashMap<String, Object>();
            Class<?> changeMode = LegacyReflection.classOrNull(CHANGE_MODE, classLoader);
            if (changeMode != null && changeMode.isEnum()) {
                for (Object mode : changeMode.getEnumConstants()) {
                    Object accepted = LegacyReflection.invoke(handler, "acceptChange", mode);
                    if (accepted != null) {
                        acceptedChangers.put(
                            String.valueOf(mode),
                            classNames(LegacyReflection.classes(accepted))
                        );
                    }
                }
            }
            data.put("acceptedChangers", acceptedChangers);
            Object requiresChange = LegacyReflection.invoke(handler, "requiresSourceExprChange");
            if (requiresChange instanceof Boolean) {
                data.put("requiresSourceExpressionChange", requiresChange);
            }
            data.put("expressionMetadataState", "resolved");
        } catch (RuntimeException ignored) {
            data.put("expressionMetadataState", "unresolved");
        }
    }

    private String handlerKind(Object handler) {
        if (isInstance("org.skriptlang.skript.lang.properties.PropertyHandler$TypedValuePropertyHandler", handler) ||
            isInstance("org.skriptlang.skript.lang.properties.handlers.TypedValueHandler", handler)) {
            return "typedValue";
        }
        if (isInstance("org.skriptlang.skript.lang.properties.PropertyHandler$ContainsHandler", handler) ||
            isInstance("org.skriptlang.skript.lang.properties.handlers.ContainsHandler", handler)) {
            return "contains";
        }
        if (isInstance("org.skriptlang.skript.lang.properties.PropertyHandler$ExpressionPropertyHandler", handler) ||
            isInstance("org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler", handler)) {
            return "expression";
        }
        if (isInstance("org.skriptlang.skript.lang.properties.PropertyHandler$ConditionPropertyHandler", handler) ||
            isInstance("org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler", handler)) {
            return "condition";
        }
        if (isInstance("org.skriptlang.skript.lang.properties.handlers.WXYZHandler", handler)) {
            return "wxyz";
        }
        return "custom";
    }

    private boolean isInstance(String className, Object value) {
        Class<?> type = LegacyReflection.classOrNull(className, classLoader);
        return type != null && type.isInstance(value);
    }

    private boolean isHidden(Class<?> type) {
        Object hidden = LegacyReflection.invokeOrNull(type, "isHidden");
        return Boolean.TRUE.equals(hidden) || type.getName().contains("$$Lambda$");
    }

    private String propertySortKey(Object property) {
        Object provider = accessor(property, "provider", "provider");
        Object providerName = accessor(provider, "name", "name");
        Class<?> handler = classValue(accessor(property, "handler", "handler"));
        return string(accessor(property, "name", "name")) + "\u0000" +
            string(providerName) + "\u0000" + (handler == null ? "" : handler.getName());
    }

    private Map<String, Object> providerData(Object provider, Object fallback) {
        Object source = accessor(provider, "source", "source");
        return addonResolver.addonForCandidates(source, fallback);
    }

    private Object accessor(Object target, String methodName, String fieldName) {
        if (target == null) return null;
        Object result = LegacyReflection.invokeOrNull(target, methodName);
        return result != null ? result : LegacyReflection.field(target, fieldName);
    }

    private List<String> classNames(List<Class<?>> classes) {
        List<String> result = new ArrayList<String>();
        for (Class<?> type : classes) result.add(className(type));
        return result;
    }

    private String className(Class<?> type) {
        hierarchy.add(type);
        return LegacyStableIds.stableName(type);
    }

    private static Class<?> classValue(Object value) {
        return value instanceof Class<?> ? (Class<?>) value : null;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void put(Map<String, Object> data, String field, Object value) {
        if (value != null) data.put(field, value);
    }

    private static void putList(Map<String, Object> data, String field, List<?> value) {
        if (value != null && !value.isEmpty()) data.put(field, value);
    }
}
