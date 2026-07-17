package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class LegacyClassHierarchy {
    private final Map<String, Class<?>> classes = new TreeMap<String, Class<?>>();
    private final LegacyAddonResolver addonResolver;

    LegacyClassHierarchy(LegacyAddonResolver addonResolver) {
        this.addonResolver = addonResolver;
    }

    void add(Class<?> type) {
        if (type == null || classes.put(LegacyStableIds.stableName(type), type) != null) return;
        add(type.getSuperclass());
        for (Class<?> interfaceType : type.getInterfaces()) add(interfaceType);
        add(type.getComponentType());
    }

    void addAll(Iterable<Class<?>> values) {
        for (Class<?> value : values) add(value);
    }

    List<Map<String, Object>> toData() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Class<?> type : classes.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", LegacyStableIds.stableName(type));
            item.put("binaryName", type.getName());
            item.put("kind", kind(type));
            if (type.getSuperclass() != null) {
                item.put("superClass", LegacyStableIds.stableName(type.getSuperclass()));
            }
            List<String> interfaces = new ArrayList<String>();
            for (Class<?> interfaceType : type.getInterfaces()) {
                interfaces.add(LegacyStableIds.stableName(interfaceType));
            }
            Collections.sort(interfaces);
            item.put("interfaces", interfaces);
            if (type.getComponentType() != null) {
                item.put("componentType", LegacyStableIds.stableName(type.getComponentType()));
            }
            Map<String, Object> provider = addonResolver.providerForClass(type);
            if (provider != null) item.put("provider", provider);
            result.add(item);
        }
        return result;
    }

    private String kind(Class<?> type) {
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
}
