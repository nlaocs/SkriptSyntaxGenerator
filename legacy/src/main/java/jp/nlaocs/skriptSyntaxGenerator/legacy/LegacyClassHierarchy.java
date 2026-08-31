package jp.nlaocs.skriptSyntaxGenerator.legacy;

import jp.nlaocs.skriptSyntaxGenerator.data.ClassMethodData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class LegacyClassHierarchy {
    private static final String CONTAINER_TYPE_ANNOTATION =
        "ch.njol.skript.util.Container$ContainerType";
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
        add(containerElementType(type));
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
            item.put("methods", methods(type));
            Class<?> containerElementType = containerElementType(type);
            if (containerElementType != null) {
                item.put("containerElementType", LegacyStableIds.stableName(containerElementType));
            }
            Map<String, Object> provider = addonResolver.providerForClass(type);
            if (provider != null) item.put("provider", provider);
            result.add(item);
        }
        return result;
    }

    private List<ClassMethodData> methods(Class<?> type) {
        try {
            return reflectedMethods(type);
        } catch (RuntimeException | LinkageError reflectionFailure) {
            try {
                return LegacyClassFileMethodReader.read(type);
            } catch (RuntimeException | LinkageError classFileFailure) {
                reflectionFailure.addSuppressed(classFileFailure);
                throw new IllegalStateException(
                    "Cannot inspect declared methods of " + type.getName(),
                    reflectionFailure
                );
            }
        }
    }

    private List<ClassMethodData> reflectedMethods(Class<?> type) {
        Map<String, ClassMethodData> unique = new TreeMap<String, ClassMethodData>();
        for (Method method : type.getDeclaredMethods()) {
            List<String> parameterTypes = new ArrayList<String>();
            for (Class<?> parameterType : method.getParameterTypes()) {
                parameterTypes.add(LegacyStableIds.stableName(parameterType));
            }
            ClassMethodData data = new ClassMethodData(
                method.getName(),
                parameterTypes,
                LegacyStableIds.stableName(method.getReturnType()),
                Modifier.isStatic(method.getModifiers())
            );
            unique.put(data.signatureKey(), data);
        }
        return new ArrayList<ClassMethodData>(unique.values());
    }

    private Class<?> containerElementType(Class<?> type) {
        Annotation[] annotations;
        try {
            annotations = type.getDeclaredAnnotations();
        } catch (RuntimeException | LinkageError failure) {
            throw new IllegalStateException(
                "Cannot inspect @ContainerType on " + type.getName(),
                failure
            );
        }
        for (Annotation annotation : annotations) {
            if (!CONTAINER_TYPE_ANNOTATION.equals(annotation.annotationType().getName())) continue;
            try {
                Object value = annotation.annotationType().getMethod("value").invoke(annotation);
                return value instanceof Class<?> ? (Class<?>) value : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                throw new IllegalStateException(
                    "Cannot inspect @ContainerType on " + type.getName(),
                    failure
                );
            }
        }
        return null;
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
