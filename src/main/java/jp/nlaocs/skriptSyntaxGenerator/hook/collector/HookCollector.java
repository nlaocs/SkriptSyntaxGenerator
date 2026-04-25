package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface HookCollector<S, K, V> {
    void add(S source);

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

