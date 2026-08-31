package jp.nlaocs.skriptSyntaxGenerator.legacy;

import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockCanBuildEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LegacySyntaxCollector {
    private final Class<?> skriptClass;
    private final ClassLoader classLoader;
    private final LegacyAddonResolver addonResolver;
    private final LegacyClassHierarchy hierarchy;

    LegacySyntaxCollector(
        Class<?> skriptClass,
        ClassLoader classLoader,
        LegacyAddonResolver addonResolver,
        LegacyClassHierarchy hierarchy
    ) {
        this.skriptClass = skriptClass;
        this.classLoader = classLoader;
        this.addonResolver = addonResolver;
        this.hierarchy = hierarchy;
    }

    List<Map<String, Object>> collectBasic(String methodName, String kind) {
        if (!LegacyReflection.hasMethod(skriptClass, methodName, 0)) return Collections.emptyList();
        List<Object> infos = LegacyReflection.list(LegacyReflection.invokeStatic(skriptClass, methodName));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Integer> occurrences = new HashMap<String, Integer>();
        for (int index = 0; index < infos.size(); index++) {
            Object info = infos.get(index);
            Class<?> elementClass = elementClass(info);
            List<String> patterns = LegacyReflection.strings(value(info, "getPatterns", "patterns"));
            int occurrence = nextOccurrence(occurrences, elementClass, patterns);
            Map<String, Object> data = commonData(info, kind, index, occurrence, elementClass, patterns);

            if ("expression".equals(kind)) addExpressionData(data, info, elementClass);
            if ("section".equals(kind)) addSectionData(data, elementClass);
            result.add(data);
        }
        return result;
    }

    List<Map<String, Object>> collectEvents(List<LegacyEventValueRecord> eventValues) {
        if (!LegacyReflection.hasMethod(skriptClass, "getEvents", 0)) return Collections.emptyList();
        List<Object> infos = LegacyReflection.list(LegacyReflection.invokeStatic(skriptClass, "getEvents"));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Integer> occurrences = new HashMap<String, Integer>();
        for (int index = 0; index < infos.size(); index++) {
            Object info = infos.get(index);
            Class<?> elementClass = elementClass(info);
            List<String> patterns = LegacyReflection.strings(value(info, "getPatterns", "patterns"));
            int occurrence = nextOccurrence(occurrences, elementClass, patterns);
            Map<String, Object> data = commonEventData(
                info, index, occurrence, elementClass, patterns
            );
            List<Class<?>> referenceEvents = LegacyReflection.classes(
                value(info, "getEvents", "events")
            );
            data.put("referenceEvents", classNames(referenceEvents));

            List<Map<String, Object>> availableValues = new ArrayList<Map<String, Object>>();
            for (LegacyEventValueRecord eventValue : eventValues) {
                if (eventValue.isAvailableFor(referenceEvents)) availableValues.add(eventValue.data);
            }
            data.put("eventValues", availableValues);

            boolean cancellable = false;
            for (Class<?> referenceEvent : referenceEvents) {
                if (Cancellable.class.isAssignableFrom(referenceEvent)
                    || BlockCanBuildEvent.class.isAssignableFrom(referenceEvent)) {
                    cancellable = true;
                    break;
                }
            }
            data.put("cancellable", cancellable);
            Boolean prioritySupported = eventPrioritySupported(elementClass);
            if (prioritySupported != null) data.put("prioritySupported", prioritySupported);
            Object name = data.get("name");
            data.put("hasOnPrefix", name != null && String.valueOf(name).startsWith("On "));
            result.add(data);
        }
        return result;
    }

    private Boolean eventPrioritySupported(Class<?> elementClass) {
        try {
            Object event = elementClass.getDeclaredConstructor().newInstance();
            Object supported = LegacyReflection.invokeOrNull(event, "isEventPrioritySupported");
            return supported instanceof Boolean ? (Boolean) supported : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    List<LegacyEventValueRecord> collectEventValues() {
        Class<?> eventValuesClass = LegacyReflection.classOrNull(
            "ch.njol.skript.registrations.EventValues", classLoader
        );
        if (eventValuesClass == null) return Collections.emptyList();

        List<LegacyEventValueRecord> result = new ArrayList<LegacyEventValueRecord>();
        int resolutionOrder = 0;
        int[] times = {-1, 0, 1};
        for (int time : times) {
            List<Object> infos = eventValueInfos(eventValuesClass, time);
            for (Object info : infos) {
                Class<?> eventClass = classValue(value(info, "getEventClass", "event"));
                Class<?> valueClass = classValue(value(info, "getValueClass", "c"));
                if (eventClass == null || valueClass == null) continue;
                List<Class<?>> excludes = LegacyReflection.classes(
                    value(info, "getExcludes", "excludes")
                );
                Object getter = LegacyReflection.field(info, "getter");
                if (getter == null) getter = value(info, "getConverter", "converter");
                Map<String, Object> addon = addonResolver.addonForCandidates(getter, eventClass, valueClass);
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("eventClass", className(eventClass));
                data.put("valueClass", className(valueClass));
                data.put("time", time);
                Object errorMessage = value(info, "getExcludeErrorMessage", "excludeErrorMessage");
                if (errorMessage != null) data.put("excludeErrorMessage", String.valueOf(errorMessage));
                if (!excludes.isEmpty()) data.put("excludes", classNames(excludes));
                data.put("resolutionOrder", resolutionOrder++);
                data.put("addon", addon);
                data.put(
                    "registrationId",
                    LegacyStableIds.record(
                        "event-value", addon, className(eventClass), className(valueClass), String.valueOf(time)
                    )
                );
                hierarchy.add(eventClass);
                hierarchy.add(valueClass);
                hierarchy.addAll(excludes);
                result.add(new LegacyEventValueRecord(eventClass, valueClass, excludes, data));
            }
        }
        return result;
    }

    private Map<String, Object> commonData(
        Object info,
        String kind,
        int registrationOrder,
        int occurrence,
        Class<?> elementClass,
        List<String> patterns
    ) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("kind", kind);
        data.put("registrationOrder", registrationOrder);
        put(data, "name", LegacyDocumentation.stringValue(elementClass, "Name"));
        put(data, "documentationId", LegacyDocumentation.stringValue(elementClass, "DocumentationId"));
        addElementIdentity(data, elementClass);
        putList(data, "since", LegacyDocumentation.stringValues(elementClass, "Since"));
        putList(data, "description", LegacyDocumentation.stringValues(elementClass, "Description"));
        putList(data, "examples", LegacyDocumentation.stringValues(elementClass, "Examples"));
        putList(data, "requires", LegacyDocumentation.stringValues(elementClass, "RequiredPlugins"));
        data.put("noDoc", LegacyDocumentation.present(elementClass, "NoDoc"));
        putList(data, "events", LegacyDocumentation.stringValues(elementClass, "Events"));
        data.put("deprecated", elementClass.isAnnotationPresent(Deprecated.class));
        addRegistrationIdentity(data, info, kind, occurrence, elementClass, patterns);
        return data;
    }

    private Map<String, Object> commonEventData(
        Object info,
        int registrationOrder,
        int occurrence,
        Class<?> elementClass,
        List<String> patterns
    ) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("kind", "event");
        data.put("registrationOrder", registrationOrder);
        put(data, "name", LegacyReflection.invokeOrNull(info, "getName"));
        put(data, "id", LegacyReflection.invokeOrNull(info, "getId"));
        put(data, "documentationId", LegacyReflection.invokeOrNull(info, "getDocumentationID"));
        addElementIdentity(data, elementClass);
        Object since = LegacyReflection.invokeOrNull(info, "getSince");
        if (since != null) data.put("since", Collections.singletonList(String.valueOf(since)));
        Object rawDescription = LegacyReflection.invokeOrNull(info, "getDescription");
        putList(data, "description", LegacyReflection.strings(rawDescription));
        putList(data, "examples", LegacyReflection.strings(LegacyReflection.invokeOrNull(info, "getExamples")));
        putList(data, "requires", LegacyReflection.strings(LegacyReflection.invokeOrNull(info, "getRequiredPlugins")));
        data.put(
            "noDoc",
            LegacyDocumentation.present(elementClass, "NoDoc") ||
                (rawDescription != null && LegacyReflection.list(rawDescription).isEmpty())
        );
        data.put("deprecated", elementClass.isAnnotationPresent(Deprecated.class));
        addRegistrationIdentity(data, info, "event", occurrence, elementClass, patterns);
        return data;
    }

    private void addRegistrationIdentity(
        Map<String, Object> data,
        Object info,
        String kind,
        int occurrence,
        Class<?> elementClass,
        List<String> patterns
    ) {
        data.put("patterns", patterns);
        String origin = stringValue(value(info, "getOriginClassPath", "originClassPath"));
        Map<String, Object> addon = addonResolver.syntaxAddon(origin, elementClass);
        data.put("addon", addon);
        String definitionId = LegacyStableIds.definition(kind, addon, elementClass);
        data.put("definitionId", definitionId);
        data.put("registrationId", LegacyStableIds.registration(definitionId, patterns, occurrence));
    }

    private void addElementIdentity(Map<String, Object> data, Class<?> elementClass) {
        data.put("elementClass", className(elementClass));
        if (elementClass.getSuperclass() != null) {
            data.put("superClass", className(elementClass.getSuperclass()));
        }
        hierarchy.add(elementClass);
    }

    private void addExpressionData(Map<String, Object> data, Object info, Class<?> elementClass) {
        Class<?> returnType = classValue(value(info, "getReturnType", "returnType"));
        if (returnType != null) {
            data.put("returnType", className(returnType));
            hierarchy.add(returnType);
        }
        data.put("returnTypeState", "unresolved");
        data.put("possibleReturnTypesState", "unresolved");
        Object expressionType = LegacyReflection.invokeOrNull(info, "getExpressionType");
        if (expressionType != null) {
            String priority = expressionPriority(String.valueOf(expressionType));
            if (priority != null) {
                data.put("priorityStr", priority);
                Map<String, Object> priorityData = new LinkedHashMap<String, Object>();
                priorityData.put("after", Collections.emptyList());
                priorityData.put("before", Collections.emptyList());
                data.put("priority", priorityData);
            }
        }
        data.put("returnTypeMultiplicityState", "unresolved");
        data.put("acceptedChangersState", "unresolved");
        Class<?> sectionExpression = LegacyReflection.classOrNull(
            "ch.njol.skript.expressions.base.SectionExpression", classLoader
        );
        data.put("sectionExpression", sectionExpression != null && sectionExpression.isAssignableFrom(elementClass));
    }

    private void addSectionData(Map<String, Object> data, Class<?> elementClass) {
        Class<?> loopSection = LegacyReflection.classOrNull("ch.njol.skript.lang.LoopSection", classLoader);
        Class<?> effectSection = LegacyReflection.classOrNull("ch.njol.skript.lang.EffectSection", classLoader);
        data.put("loopSection", loopSection != null && loopSection.isAssignableFrom(elementClass));
        data.put("effectSection", effectSection != null && effectSection.isAssignableFrom(elementClass));
    }

    private List<Object> eventValueInfos(Class<?> eventValuesClass, int time) {
        if (LegacyReflection.hasMethod(eventValuesClass, "getEventValuesListForTime", 1)) {
            return LegacyReflection.list(
                LegacyReflection.invokeStatic(eventValuesClass, "getEventValuesListForTime", time)
            );
        }
        String fieldName = time < 0 ? "pastEventValues" : time > 0 ? "futureEventValues" : "defaultEventValues";
        return LegacyReflection.list(LegacyReflection.field(eventValuesClass, fieldName));
    }

    private Class<?> elementClass(Object info) {
        Class<?> result = classValue(value(info, "getElementClass", "c"));
        if (result == null) throw new IllegalStateException("Syntax info has no element class: " + info);
        return result;
    }

    private Object value(Object target, String methodName, String fieldName) {
        Object result = LegacyReflection.invokeOrNull(target, methodName);
        if (result == null && methodName.startsWith("get") && methodName.length() > 3) {
            String accessor = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            result = LegacyReflection.invokeOrNull(target, accessor);
        }
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

    private int nextOccurrence(
        Map<String, Integer> occurrences,
        Class<?> elementClass,
        List<String> patterns
    ) {
        String key = LegacyStableIds.stableName(elementClass) + "\u0000" + patterns.toString();
        Integer current = occurrences.get(key);
        int occurrence = current == null ? 0 : current;
        occurrences.put(key, occurrence + 1);
        return occurrence;
    }

    private String expressionPriority(String expressionType) {
        if ("SIMPLE".equals(expressionType)) return "SyntaxInfos.SIMPLE";
        if ("COMBINED".equals(expressionType)) return "SyntaxInfos.COMBINED";
        if ("PATTERN_MATCHES_EVERYTHING".equals(expressionType)) {
            return "SyntaxInfos.PATTERN_MATCHES_EVERYTHING";
        }
        if ("EVENT".equals(expressionType)) return "EventValueExpression.DEFAULT_PRIORITY";
        if ("PROPERTY".equals(expressionType)) return "PropertyExpression.DEFAULT_PRIORITY";
        return null;
    }

    private static Class<?> classValue(Object value) {
        return value instanceof Class<?> ? (Class<?>) value : null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void put(Map<String, Object> data, String field, Object value) {
        if (value != null) data.put(field, value);
    }

    private static void putList(Map<String, Object> data, String field, List<?> value) {
        if (value != null && !value.isEmpty()) data.put(field, value);
    }
}
