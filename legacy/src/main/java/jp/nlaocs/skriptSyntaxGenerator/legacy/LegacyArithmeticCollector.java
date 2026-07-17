package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LegacyArithmeticCollector {
    private static final String ARITHMETICS =
        "org.skriptlang.skript.lang.arithmetic.Arithmetics";
    private static final String OPERATOR =
        "org.skriptlang.skript.lang.arithmetic.Operator";

    private final ClassLoader classLoader;
    private final LegacyAddonResolver addonResolver;
    private final LegacyClassHierarchy hierarchy;

    LegacyArithmeticCollector(
        ClassLoader classLoader,
        LegacyAddonResolver addonResolver,
        LegacyClassHierarchy hierarchy
    ) {
        this.classLoader = classLoader;
        this.addonResolver = addonResolver;
        this.hierarchy = hierarchy;
    }

    boolean isAvailable() {
        return arithmeticClass() != null && operatorClass() != null;
    }

    List<Map<String, Object>> collectOperators() {
        List<Object> operators = operators();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < operators.size(); index++) {
            Object operator = operators.get(index);
            String sign = sign(operator);
            Map<String, Object> addon = addonResolver.addonForCandidates(operator);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("sign", sign);
            data.put("priority", priority(operator, sign));
            put(data, "key", operatorKey(operator));
            data.put("registrationOrder", index);
            data.put("addon", addon);
            data.put("registrationId", LegacyStableIds.record("operator", addon, sign, string(operatorKey(operator))));
            result.add(data);
        }
        return result;
    }

    Map<String, List<Map<String, Object>>> collectOperations() {
        Class<?> arithmetics = arithmeticClass();
        if (arithmetics == null) return Collections.emptyMap();

        Map<String, List<Map<String, Object>>> result =
            new LinkedHashMap<String, List<Map<String, Object>>>();
        int registrationOrder = 0;
        for (Object operator : operators()) {
            String sign = sign(operator);
            List<Map<String, Object>> operationData = new ArrayList<Map<String, Object>>();
            for (Object info : LegacyReflection.list(
                LegacyReflection.invokeStatic(arithmetics, "getOperations", operator)
            )) {
                Class<?> left = classValue(value(info, "getLeft", "left"));
                Class<?> right = classValue(value(info, "getRight", "right"));
                Class<?> returnType = classValue(value(info, "getReturnType", "returnType"));
                if (left == null || right == null || returnType == null) continue;
                Object operation = value(info, "getOperation", "operation");
                Map<String, Object> addon = addonResolver.addonForCandidates(
                    operation, operator, left, right, returnType
                );
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("operatorSign", sign);
                data.put("left", className(left));
                data.put("right", className(right));
                data.put("returnType", className(returnType));
                data.put("registrationOrder", registrationOrder++);
                data.put("addon", addon);
                data.put(
                    "registrationId",
                    LegacyStableIds.record(
                        "operation", addon, sign, className(left), className(right), className(returnType)
                    )
                );
                operationData.add(data);
            }
            result.put(sign, operationData);
        }
        return result;
    }

    List<Map<String, Object>> collectDifferences() {
        Class<?> arithmetics = arithmeticClass();
        if (arithmetics == null) return Collections.emptyList();

        Object rawDifferences = LegacyReflection.field(arithmetics, "differences");
        if (rawDifferences == null) rawDifferences = LegacyReflection.field(arithmetics, "DIFFERENCES");
        List<Object> infos = rawDifferences instanceof Map<?, ?>
            ? new ArrayList<Object>(((Map<?, ?>) rawDifferences).values())
            : LegacyReflection.list(rawDifferences);
        Collections.sort(infos, new Comparator<Object>() {
            @Override
            public int compare(Object first, Object second) {
                return stableTypeName(first).compareTo(stableTypeName(second));
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object info : infos) {
            Class<?> type = classValue(value(info, "getType", "type"));
            Class<?> returnType = classValue(value(info, "getReturnType", "returnType"));
            if (type == null || returnType == null) continue;
            Object operation = value(info, "getOperation", "operation");
            Map<String, Object> addon = addonResolver.addonForCandidates(operation, type, returnType);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("type", className(type));
            data.put("returnType", className(returnType));
            data.put("registrationOrder", result.size());
            data.put("addon", addon);
            data.put(
                "registrationId",
                LegacyStableIds.record("difference", addon, className(type), className(returnType))
            );
            result.add(data);
        }
        return result;
    }

    private List<Object> operators() {
        Class<?> type = operatorClass();
        Class<?> arithmetics = arithmeticClass();
        if (type == null || arithmetics == null) return Collections.emptyList();

        if (!type.isEnum() && LegacyReflection.hasMethod(arithmetics, "getAllOperators", 0)) {
            return LegacyReflection.list(LegacyReflection.invokeStatic(arithmetics, "getAllOperators"));
        }

        List<Object> result = LegacyReflection.list(type.getEnumConstants());
        Collections.sort(result, new Comparator<Object>() {
            @Override
            public int compare(Object first, Object second) {
                return Integer.compare(precedence(sign(first)), precedence(sign(second)));
            }
        });
        return result;
    }

    private Map<String, Object> priority(Object operator, String sign) {
        Object actualPriority = accessor(operator, "priority", "priority");
        if (actualPriority != null) return priorityData(actualPriority);

        int level = 0;
        if ("*".equals(sign) || "/".equals(sign)) level = 1;
        else if ("^".equals(sign)) level = 2;
        return priorityLevel(level);
    }

    private Map<String, Object> priorityData(Object priority) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> after = new ArrayList<Map<String, Object>>();
        for (Object item : LegacyReflection.list(LegacyReflection.invokeOrNull(priority, "after"))) {
            after.add(priorityData(item));
        }
        result.put("after", after);
        List<Map<String, Object>> before = new ArrayList<Map<String, Object>>();
        for (Object item : LegacyReflection.list(LegacyReflection.invokeOrNull(priority, "before"))) {
            before.add(priorityData(item));
        }
        result.put("before", before);
        return result;
    }

    private Map<String, Object> priorityLevel(int level) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("after", Collections.emptyList());
        List<Map<String, Object>> before = new ArrayList<Map<String, Object>>();
        for (int lowerLevel = 0; lowerLevel < level; lowerLevel++) {
            before.add(priorityLevel(lowerLevel));
        }
        result.put("before", before);
        return result;
    }

    private int precedence(String sign) {
        if ("^".equals(sign)) return 0;
        if ("*".equals(sign)) return 1;
        if ("/".equals(sign)) return 2;
        if ("+".equals(sign)) return 3;
        if ("-".equals(sign)) return 4;
        return 5;
    }

    private Object operatorKey(Object operator) {
        Object noun = LegacyReflection.field(operator, "m_name");
        if (noun == null) noun = accessor(operator, "node", "node");
        return noun == null ? null : LegacyReflection.field(noun, "key");
    }

    private String stableTypeName(Object info) {
        Class<?> type = classValue(value(info, "getType", "type"));
        return type == null ? "" : LegacyStableIds.stableName(type);
    }

    private String sign(Object operator) {
        Object raw = LegacyReflection.field(operator, "sign");
        return raw == null ? String.valueOf(operator) : String.valueOf(raw);
    }

    private Class<?> arithmeticClass() {
        return LegacyReflection.classOrNull(ARITHMETICS, classLoader);
    }

    private Class<?> operatorClass() {
        return LegacyReflection.classOrNull(OPERATOR, classLoader);
    }

    private Object value(Object target, String methodName, String fieldName) {
        Object result = LegacyReflection.invokeOrNull(target, methodName);
        return result != null ? result : LegacyReflection.field(target, fieldName);
    }

    private Object accessor(Object target, String methodName, String fieldName) {
        Object result = LegacyReflection.invokeOrNull(target, methodName);
        return result != null ? result : LegacyReflection.field(target, fieldName);
    }

    private String className(Class<?> type) {
        hierarchy.add(type);
        return LegacyStableIds.stableName(type);
    }

    private static Class<?> classValue(Object value) {
        return value instanceof Class<?> ? (Class<?>) value : null;
    }

    private static void put(Map<String, Object> data, String field, Object value) {
        if (value != null) data.put(field, value);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
