package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import java.util.List;

public final class HookCollectorRegistry {

    private static final List<HookCollector<?, ?, ?>> COLLECTORS = List.of(
            RegisterClassCollector.getInstance(),
            RegisterComparatorCollector.getInstance()
    );

    private HookCollectorRegistry() {
    }

    public static List<HookCollector<?, ?, ?>> collectors() {
        return COLLECTORS;
    }

    public static void clearAll() {
        COLLECTORS.forEach(collector -> collector.clear());
    }
}

