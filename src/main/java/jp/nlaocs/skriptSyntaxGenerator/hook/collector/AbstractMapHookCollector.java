package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractMapHookCollector<S, K, V> implements HookCollector<S, K, V> {

    private final ConcurrentMap<K, V> snapshots = new ConcurrentHashMap<>();

    @Override
    public final void add(S source) {
        if (!shouldCollect(source)) {
            return;
        }

        K key = keyOf(source);
        if (key == null) {
            return;
        }

        V snapshot = snapshotOf(source);
        if (snapshot == null) {
            return;
        }

        snapshots.put(key, snapshot);
    }

    protected boolean shouldCollect(S source) {
        return source != null;
    }

    protected abstract K keyOf(S source);

    protected abstract V snapshotOf(S source);

    protected final Collection<V> snapshotValues() {
        return List.copyOf(snapshots.values());
    }

    @Override
    public Map<K, V> snapshotMap() {
        return Map.copyOf(snapshots);
    }

    @Override
    public void clear() {
        snapshots.clear();
    }
}

