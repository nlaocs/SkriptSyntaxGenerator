package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HookCollector<S, K, V> {
    void add(S source);

    /**
     * Determines if the given arguments are valid for this collector.
     * Used when distinguishing between overloads in Advice with @Advice.AllArguments.
     *
     * @param args Arguments passed from Advice.AllArguments
     * @return true if this collector should process these arguments
     */
    default boolean isValidArguments(Object... args) {
        return true;
    }

    Map<K, V> snapshotMap();

    default Collection<V> snapshot() {
        return List.copyOf(snapshotMap().values());
    }

    void clear();

    default int size() {
        return snapshotMap().size();
    }

    default boolean isEmpty() {
        return size() == 0;
    }
}

