package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.util.List;
import java.util.Map;

final class LegacyEventValueRecord {
    final Class<?> eventClass;
    final Class<?> valueClass;
    final List<Class<?>> excludes;
    final Map<String, Object> data;

    LegacyEventValueRecord(
        Class<?> eventClass,
        Class<?> valueClass,
        List<Class<?>> excludes,
        Map<String, Object> data
    ) {
        this.eventClass = eventClass;
        this.valueClass = valueClass;
        this.excludes = excludes;
        this.data = data;
    }

    boolean isAvailableFor(List<Class<?>> referenceEvents) {
        for (Class<?> referenceEvent : referenceEvents) {
            if (!eventClass.isAssignableFrom(referenceEvent)) continue;
            boolean excluded = false;
            for (Class<?> excludedType : excludes) {
                if (excludedType.isAssignableFrom(referenceEvent)) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) return true;
        }
        return false;
    }
}
