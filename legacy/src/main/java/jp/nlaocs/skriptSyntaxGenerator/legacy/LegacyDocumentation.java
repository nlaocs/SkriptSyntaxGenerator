package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.lang.annotation.Annotation;
import java.util.List;

final class LegacyDocumentation {
    private static final String PREFIX = "ch.njol.skript.doc.";

    private LegacyDocumentation() {
    }

    static String stringValue(Class<?> elementClass, String annotationName) {
        Object value = value(elementClass, annotationName);
        return value instanceof String ? (String) value : null;
    }

    static List<String> stringValues(Class<?> elementClass, String annotationName) {
        return LegacyReflection.strings(value(elementClass, annotationName));
    }

    static boolean present(Class<?> elementClass, String annotationName) {
        return annotation(elementClass, annotationName) != null;
    }

    private static Object value(Class<?> elementClass, String annotationName) {
        Annotation annotation = annotation(elementClass, annotationName);
        return annotation == null ? null : LegacyReflection.invokeOrNull(annotation, "value");
    }

    @SuppressWarnings("unchecked")
    private static Annotation annotation(Class<?> elementClass, String annotationName) {
        ClassLoader loader = elementClass.getClassLoader();
        Class<?> rawType = LegacyReflection.classOrNull(PREFIX + annotationName, loader);
        if (rawType == null || !Annotation.class.isAssignableFrom(rawType)) return null;
        return elementClass.getAnnotation((Class<? extends Annotation>) rawType);
    }
}
