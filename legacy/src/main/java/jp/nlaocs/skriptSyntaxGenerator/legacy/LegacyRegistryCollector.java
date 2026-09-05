package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class LegacyRegistryCollector {
    private final ClassLoader classLoader;
    private final LegacyAddonResolver addonResolver;
    private final LegacyClassHierarchy hierarchy;

    LegacyRegistryCollector(
        ClassLoader classLoader,
        LegacyAddonResolver addonResolver,
        LegacyClassHierarchy hierarchy
    ) {
        this.classLoader = classLoader;
        this.addonResolver = addonResolver;
        this.hierarchy = hierarchy;
    }

    List<Map<String, Object>> collectTypes() {
        Class<?> classesClass = LegacyReflection.classOrNull(
            "ch.njol.skript.registrations.Classes", classLoader
        );
        if (classesClass == null || !LegacyReflection.hasMethod(classesClass, "getClassInfos", 0)) {
            return Collections.emptyList();
        }
        List<Object> infos = LegacyReflection.list(
            LegacyReflection.invokeStatic(classesClass, "getClassInfos")
        );
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < infos.size(); index++) {
            Object info = infos.get(index);
            Class<?> originalClass = classValue(value(info, "getC", "c"));
            if (originalClass == null) continue;
            String codeName = String.valueOf(value(info, "getCodeName", "codeName"));
            Object parser = value(info, "getParser", "parser");
            Object serializer = value(info, "getSerializer", "serializer");
            Object changer = value(info, "getChanger", "changer");
            Object defaultExpression = value(info, "getDefaultExpression", "defaultExpression");
            Map<String, Object> addon = addonResolver.addonForCandidates(
                parser, serializer, changer, defaultExpression, originalClass
            );

            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("typeParseOrder", index);
            put(data, "name", LegacyReflection.invokeOrNull(info, "getDocName"));
            putList(data, "description", LegacyReflection.strings(LegacyReflection.invokeOrNull(info, "getDescription")));
            Object since = LegacyReflection.invokeOrNull(info, "getSince");
            if (since != null) data.put("since", Collections.singletonList(String.valueOf(since)));
            putList(data, "examples", LegacyReflection.strings(LegacyReflection.invokeOrNull(info, "getExamples")));
            putList(data, "requires", LegacyReflection.strings(LegacyReflection.field(info, "requiredPlugins")));
            data.put("addon", addon);
            String definitionId = LegacyStableIds.record(
                "type", addon, codeName, LegacyStableIds.stableName(originalClass)
            );
            data.put("definitionId", definitionId);
            data.put("registrationId", definitionId);
            put(data, "documentationId", LegacyReflection.invokeOrNull(info, "getDocumentationID"));
            Object docName = LegacyReflection.invokeOrNull(info, "getDocName");
            data.put("hasDocs", docName != null && !String.valueOf(docName).isEmpty());
            addChanger(data, changer);
            data.put("originalClass", className(originalClass));
            data.put("classType", classType(originalClass));
            data.put("codeName", codeName);
            if (originalClass.getSuperclass() != null) {
                data.put("superClass", className(originalClass.getSuperclass()));
            }
            data.put("interfaces", classNames(asList(originalClass.getInterfaces())));
            data.put("assignableTo", assignableTo(infos, info, originalClass));
            putList(data, "userInputPatterns", patterns(value(info, "getUserInputPatterns", "userInputPatterns")));
            data.put("noun", nounData(value(info, "getName", "name")));
            Class<?> serializeAs = classValue(value(info, "getSerializeAs", "serializeAs"));
            if (serializeAs != null) data.put("serializeAs", className(serializeAs));
            putList(data, "usage", LegacyReflection.strings(LegacyReflection.invokeOrNull(info, "getUsage")));
            putList(data, "enumValues", enumValues(originalClass));
            putList(data, "parserPatterns", parserPatterns(parser));
            putList(data, "registeredParserPatterns", registeredParserPatterns(originalClass));
            Object supplier = LegacyReflection.invokeOrNull(info, "getSupplier");
            putList(data, "literalValues", literalValues(parser, supplier));
            putList(data, "typeLiterals", typeLiterals(parser, supplier));
            if (parser != null) data.put("parserClass", className(parser.getClass()));
            putList(data, "parseContexts", parseContexts(parser));
            if (defaultExpression != null) {
                data.put("defaultExpressionClass", className(defaultExpression.getClass()));
            }
            data.put("hasParser", parser != null);
            data.put("hasSerializer", serializer != null);
            data.put("hasSupplier", supplier != null);
            data.put("properties", Collections.emptyList());
            putList(data, "before", sortedStrings(LegacyReflection.invokeOrNull(info, "before")));
            putList(data, "after", sortedStrings(LegacyReflection.invokeOrNull(info, "after")));
            result.add(data);
        }
        return result;
    }

    List<Map<String, Object>> collectFunctions() {
        Class<?> functionsClass = LegacyReflection.classOrNull(
            "ch.njol.skript.lang.function.Functions", classLoader
        );
        if (functionsClass == null || !LegacyReflection.hasMethod(functionsClass, "getJavaFunctions", 0)) {
            return Collections.emptyList();
        }
        List<Object> functions = LegacyReflection.list(
            LegacyReflection.invokeStatic(functionsClass, "getJavaFunctions")
        );
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < functions.size(); index++) {
            Object function = functions.get(index);
            String name = String.valueOf(LegacyReflection.invoke(function, "getName"));
            Object returnInfo = LegacyReflection.invokeOrNull(function, "getReturnType");
            Class<?> returnType = classValue(value(returnInfo, "getC", "c"));
            boolean returnSingle = Boolean.TRUE.equals(LegacyReflection.invokeOrNull(function, "isSingle"));
            List<Map<String, Object>> parameters = new ArrayList<Map<String, Object>>();
            List<String> parameterFingerprints = new ArrayList<String>();
            for (Object parameter : LegacyReflection.list(LegacyReflection.invoke(function, "getParameters"))) {
                String parameterName = String.valueOf(LegacyReflection.invoke(parameter, "getName"));
                Object typeInfo = LegacyReflection.invoke(parameter, "getType");
                Class<?> parameterType = classValue(value(typeInfo, "getC", "c"));
                if (parameterType == null) continue;
                boolean single = Boolean.TRUE.equals(LegacyReflection.invoke(parameter, "isSingleValue"));
                List<Map<String, Object>> modifiers = new ArrayList<Map<String, Object>>();
                Object defaultExpression = LegacyReflection.invokeOrNull(parameter, "getDefaultExpression");
                if (defaultExpression != null) {
                    Map<String, Object> optional = new LinkedHashMap<String, Object>();
                    optional.put("type", "optional");
                    modifiers.add(optional);
                }
                Map<String, Object> parameterData = new LinkedHashMap<String, Object>();
                parameterData.put("name", parameterName);
                parameterData.put("type", className(parameterType));
                parameterData.put("modifiers", modifiers);
                parameterData.put("single", single);
                parameters.add(parameterData);
                parameterFingerprints.add(
                    parameterName + ":" + LegacyStableIds.stableName(parameterType) + ":" + single + ":" +
                        (modifiers.isEmpty() ? "" : "optional")
                );
            }
            Map<String, Object> addon = addonResolver.addonForCandidates(function);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("registrationOrder", index);
            data.put("name", name);
            putList(data, "description", LegacyReflection.strings(LegacyReflection.invokeOrNull(function, "getDescription")));
            Object since = LegacyReflection.invokeOrNull(function, "getSince");
            if (since != null) data.put("since", Collections.singletonList(String.valueOf(since)));
            putList(data, "examples", LegacyReflection.strings(LegacyReflection.invokeOrNull(function, "getExamples")));
            if (returnType != null) data.put("returnType", className(returnType));
            data.put("returnTypeIsSingle", returnSingle);
            data.put("parameters", parameters);
            data.put("addon", addon);
            data.put(
                "registrationId",
                LegacyStableIds.record(
                    "function", addon, name, join(parameterFingerprints, ";"),
                    returnType == null ? "" : LegacyStableIds.stableName(returnType),
                    String.valueOf(returnSingle)
                )
            );
            data.put("definitionId", LegacyStableIds.record("function", addon, name));
            result.add(data);
        }
        return result;
    }

    List<Map<String, Object>> collectConverters() {
        Class<?> convertersClass = LegacyReflection.classOrNull(
            "org.skriptlang.skript.lang.converter.Converters", classLoader
        );
        String getter = "getConverterInfos";
        if (convertersClass == null || !LegacyReflection.hasMethod(convertersClass, getter, 0)) {
            convertersClass = LegacyReflection.classOrNull(
                "ch.njol.skript.registrations.Converters", classLoader
            );
            getter = "getConverters";
        }
        if (convertersClass == null || !LegacyReflection.hasMethod(convertersClass, getter, 0)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object info : LegacyReflection.list(LegacyReflection.invokeStatic(convertersClass, getter))) {
            Object converter = value(info, "getConverter", "converter");
            if (converter != null && converter.getClass().getName().endsWith(".ChainedConverter")) {
                continue;
            }
            Class<?> from = classValue(value(info, "getFrom", "from"));
            Class<?> to = classValue(value(info, "getTo", "to"));
            if (from == null || to == null) continue;
            Object rawFlags = value(info, "getFlags", "flags");
            if (!(rawFlags instanceof Number)) rawFlags = LegacyReflection.field(info, "options");
            int flags = rawFlags instanceof Number ? ((Number) rawFlags).intValue() : 0;
            Map<String, Object> addon = addonResolver.addonForCandidates(converter, from, to);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("from", className(from));
            data.put("to", className(to));
            data.put("flags", flags);
            data.put("registrationOrder", result.size());
            data.put("addon", addon);
            data.put(
                "registrationId",
                LegacyStableIds.record(
                    "converter", addon, className(from), className(to), String.valueOf(flags)
                )
            );
            result.add(data);
        }
        return result;
    }

    List<Map<String, Object>> collectComparators() {
        Class<?> comparatorsClass = LegacyReflection.classOrNull(
            "org.skriptlang.skript.lang.comparator.Comparators", classLoader
        );
        List<Object> infos;
        boolean supportsInversionMetadata;
        if (comparatorsClass != null && LegacyReflection.hasMethod(comparatorsClass, "getComparatorInfos", 0)) {
            infos = LegacyReflection.list(
                LegacyReflection.invokeStatic(comparatorsClass, "getComparatorInfos")
            );
            supportsInversionMetadata = true;
        } else {
            comparatorsClass = LegacyReflection.classOrNull(
                "ch.njol.skript.registrations.Comparators", classLoader
            );
            if (comparatorsClass == null) return Collections.emptyList();
            infos = LegacyReflection.list(LegacyReflection.field(comparatorsClass, "comparators"));
            supportsInversionMetadata = false;
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object info : infos) {
            Class<?> firstType = classValue(value(info, "getFirstType", "firstType"));
            if (firstType == null) firstType = classValue(LegacyReflection.field(info, "c1"));
            Class<?> secondType = classValue(value(info, "getSecondType", "secondType"));
            if (secondType == null) secondType = classValue(LegacyReflection.field(info, "c2"));
            Object comparator = value(info, "getComparator", "comparator");
            if (comparator == null) comparator = LegacyReflection.field(info, "c");
            if (firstType == null || secondType == null) continue;
            Map<String, Object> addon = addonResolver.addonForCandidates(comparator, firstType, secondType);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("registrationOrder", result.size());
            data.put("firstType", className(firstType));
            data.put("secondType", className(secondType));
            Object ordering = LegacyReflection.invokeOrNull(comparator, "supportsOrdering");
            if (ordering instanceof Boolean) data.put("supportsOrdering", ordering);
            Object inversion = supportsInversionMetadata
                ? LegacyReflection.invokeOrNull(comparator, "supportsInversion")
                : Boolean.TRUE;
            if (inversion instanceof Boolean) data.put("supportsInversion", inversion);
            data.put("addon", addon);
            data.put(
                "registrationId",
                LegacyStableIds.record(
                    "comparator", addon, className(firstType), className(secondType)
                )
            );
            result.add(data);
        }
        return result;
    }
    private void addChanger(Map<String, Object> data, Object changer) {
        if (changer == null) return;
        Class<?> changeMode = LegacyReflection.classOrNull(
            "ch.njol.skript.classes.Changer$ChangeMode", classLoader
        );
        if (changeMode == null || !changeMode.isEnum()) return;
        Map<String, Object> accepted = new LinkedHashMap<String, Object>();
        for (Object mode : changeMode.getEnumConstants()) {
            Object acceptedTypes = LegacyReflection.invokeOrNull(changer, "acceptChange", mode);
            if (acceptedTypes == null) continue;
            List<Class<?>> classes = LegacyReflection.classes(acceptedTypes);
            accepted.put(String.valueOf(mode), classNames(classes));
        }
        if (!accepted.isEmpty()) data.put("changer", accepted);
    }

    private List<String> assignableTo(List<Object> infos, Object currentInfo, Class<?> currentClass) {
        List<String> result = new ArrayList<String>();
        for (Object info : infos) {
            if (info == currentInfo) continue;
            Class<?> candidate = classValue(value(info, "getC", "c"));
            if (candidate != null && candidate.isAssignableFrom(currentClass)) {
                result.add(String.valueOf(value(info, "getCodeName", "codeName")));
            }
        }
        return result;
    }

    private Map<String, Object> nounData(Object noun) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if (noun == null) return result;
        put(result, "key", LegacyReflection.field(noun, "key"));
        put(result, "value", LegacyReflection.invokeOrNull(noun, "getValue"));
        put(result, "singular", LegacyReflection.invokeOrNull(noun, "getSingular"));
        put(result, "plural", LegacyReflection.invokeOrNull(noun, "getPlural"));
        Object gender = LegacyReflection.invokeOrNull(noun, "getGender");
        if (gender != null) {
            result.put("gender", gender);
            Object genderId = LegacyReflection.invokeStaticOrNull(noun.getClass(), "getGenderID", gender);
            put(result, "genderId", genderId);
        }
        return result;
    }

    private List<String> enumValues(Class<?> originalClass) {
        List<String> result = new ArrayList<String>();
        if (!originalClass.isEnum()) return result;
        Object constants = originalClass.getEnumConstants();
        for (int index = 0; index < Array.getLength(constants); index++) {
            Enum<?> constant = (Enum<?>) Array.get(constants, index);
            result.add(constant.name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
        }
        return result;
    }

    private List<String> parserPatterns(Object parser) {
        return LegacyReflection.strings(LegacyReflection.invokeOrNull(parser, "getPatterns"));
    }

    private List<Map<String, Object>> registeredParserPatterns(Class<?> owner) {
        if (!owner.getName().equals("ch.njol.skript.entity.EntityData")) {
            return Collections.emptyList();
        }
        Object rawInfos = LegacyReflection.field(owner, "infos");
        if (rawInfos == null) return Collections.emptyList();

        List<Map<String, Object>> registrations = new ArrayList<Map<String, Object>>();
        List<Object> infos = LegacyReflection.list(rawInfos);
        for (int registrationIndex = 0; registrationIndex < infos.size(); registrationIndex++) {
            Object info = infos.get(registrationIndex);
            Object rawPatterns = LegacyReflection.invokeOrNull(info, "getPatterns");
            Class<?> dataClass = classValue(LegacyReflection.invokeOrNull(info, "getElementClass"));
            Class<?> representedClass = classValue(LegacyReflection.field(info, "entityClass"));
            if (rawPatterns == null || dataClass == null || representedClass == null) {
                return Collections.emptyList();
            }

            List<Object> patterns = LegacyReflection.list(rawPatterns);
            for (int patternIndex = 0; patternIndex < patterns.size(); patternIndex++) {
                Object rawPattern = patterns.get(patternIndex);
                if (!(rawPattern instanceof String)) return Collections.emptyList();

                Map<String, Object> registration = new LinkedHashMap<String, Object>();
                registration.put("pattern", rawPattern);
                registration.put("registrationIndex", registrationIndex);
                registration.put("patternIndex", patternIndex);
                Object codeName = LegacyReflection.invokeOrNull(info, "getCodeNameFromPattern", patternIndex);
                if (codeName == null) {
                    List<Object> codeNames = LegacyReflection.list(LegacyReflection.field(info, "codeNames"));
                    if (patternIndex < codeNames.size()) codeName = codeNames.get(patternIndex);
                }
                if (codeName == null) codeName = LegacyReflection.field(info, "codeName");
                put(registration, "sourceCodeName", codeName);
                registration.put("dataClass", className(dataClass));
                registration.put(
                    "representedClass",
                    className(representedClassForPattern(dataClass, codeName, representedClass))
                );
                registrations.add(registration);
            }
        }
        return registrations;
    }

    private Class<?> representedClassForPattern(
        Class<?> dataClass,
        Object sourceCodeName,
        Class<?> fallback
    ) {
        if (!dataClass.getName().equals("ch.njol.skript.entity.SimpleEntityData") || sourceCodeName == null) {
            return fallback;
        }
        for (Object simpleInfo : LegacyReflection.list(LegacyReflection.field(dataClass, "types"))) {
            if (!sourceCodeName.equals(LegacyReflection.field(simpleInfo, "codeName"))) continue;
            Class<?> representedClass = classValue(LegacyReflection.field(simpleInfo, "c"));
            if (representedClass != null) return representedClass;
        }
        return fallback;
    }

    private List<String> literalValues(Object parser, Object supplier) {
        if (parser == null || supplier == null) return Collections.emptyList();
        Object iterator = LegacyReflection.invokeOrNull(supplier, "get");
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Object value : LegacyReflection.list(iterator)) {
            Object rendered = LegacyReflection.invokeOrNull(parser, "toString", value, 0);
            if (rendered == null) continue;
            String text = String.valueOf(rendered).trim();
            if (!text.isEmpty()) result.add(text);
        }
        return new ArrayList<String>(result);
    }

    private List<Map<String, Object>> typeLiterals(Object parser, Object supplier) {
        if (parser == null || supplier == null) return Collections.emptyList();
        Object iterator = LegacyReflection.invokeOrNull(supplier, "get");
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object value : LegacyReflection.list(iterator)) {
            if (value == null) continue;
            String text = rendered(parser, "toString", value, 0);
            if (text == null) continue;
            Map<String, Object> literal = new LinkedHashMap<String, Object>();
            literal.put("text", text);
            String plural = rendered(parser, "toString", value, 1);
            if (plural != null && !plural.equals(text)) literal.put("pluralText", plural);
            put(literal, "variableName", rendered(parser, "toVariableNameString", value));
            String debug = rendered(parser, "getDebugMessage", value);
            if (debug != null && !debug.equals(text)) literal.put("debugText", debug);
            literal.put("valueClass", className(value.getClass()));
            Object representedClass = LegacyReflection.invokeOrNull(value, "getType");
            if (representedClass instanceof Class<?>) {
                literal.put("representedClass", className((Class<?>) representedClass));
            }
            if (value instanceof Enum<?>) literal.put("enumConstant", ((Enum<?>) value).name());
            if (!result.contains(literal)) result.add(literal);
        }
        return result;
    }

    private List<String> parseContexts(Object parser) {
        if (parser == null) return Collections.emptyList();
        Class<?> contextClass = LegacyReflection.classOrNull("ch.njol.skript.lang.ParseContext", classLoader);
        if (contextClass == null || !contextClass.isEnum()) return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        for (Object context : contextClass.getEnumConstants()) {
            if (Boolean.TRUE.equals(LegacyReflection.invokeOrNull(parser, "canParse", context))) {
                result.add(((Enum<?>) context).name());
            }
        }
        return result;
    }

    private String rendered(Object parser, String method, Object value, Object... extra) {
        Object[] arguments = new Object[extra.length + 1];
        arguments[0] = value;
        System.arraycopy(extra, 0, arguments, 1, extra.length);
        Object rendered = LegacyReflection.invokeOrNull(parser, method, arguments);
        if (rendered == null) return null;
        String text = String.valueOf(rendered).trim();
        return text.isEmpty() ? null : text;
    }

    private List<String> patterns(Object rawPatterns) {
        List<String> result = new ArrayList<String>();
        for (Object rawPattern : LegacyReflection.list(rawPatterns)) {
            if (rawPattern instanceof Pattern) result.add(((Pattern) rawPattern).pattern());
            else if (rawPattern != null) result.add(String.valueOf(rawPattern));
        }
        return result;
    }

    private List<String> sortedStrings(Object value) {
        List<String> result = LegacyReflection.strings(value);
        Collections.sort(result);
        return result;
    }

    private String classType(Class<?> type) {
        if (type.isAnnotation()) return "Annotation";
        if (type.isEnum()) return "Enum";
        if (type.isInterface()) return "Interface";
        if (type.isArray()) return "Array";
        if (type.isPrimitive()) return "Primitive";
        if (type.isSynthetic()) return "Synthetic";
        if (type.isMemberClass()) return "MemberClass";
        if (type.isLocalClass()) return "LocalClass";
        if (type.isAnonymousClass()) return "AnonymousClass";
        return "Class";
    }

    private Object value(Object target, String methodName, String fieldName) {
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

    private static List<Class<?>> asList(Class<?>[] values) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        Collections.addAll(result, values);
        return result;
    }

    private static Class<?> classValue(Object value) {
        return value instanceof Class<?> ? (Class<?>) value : null;
    }

    private static void put(Map<String, Object> data, String field, Object value) {
        if (value != null) data.put(field, value);
    }

    private static void putList(Map<String, Object> data, String field, List<?> value) {
        if (value != null && !value.isEmpty()) data.put(field, value);
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(delimiter);
            result.append(value);
        }
        return result.toString();
    }
}
