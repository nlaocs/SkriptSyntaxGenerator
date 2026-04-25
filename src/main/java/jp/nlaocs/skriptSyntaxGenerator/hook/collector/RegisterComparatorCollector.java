package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import kotlin.Pair;
import org.skriptlang.skript.lang.comparator.Comparator;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisterComparatorCollector {

    private static final RegisterComparatorCollector INSTANCE = new RegisterComparatorCollector();

    private final Map<Comparator<?, ?>, Snapshot> comparators = new ConcurrentHashMap<>();

    private RegisterComparatorCollector() {
    }

    public static RegisterComparatorCollector getInstance() {
        return INSTANCE;
    }

    public void add(Comparator<?, ?> comparator) {
        //comparators.put(new Pair<>(comparator.getFirstType(), comparator.getSecondType()), Snapshot.from(comparator));
        comparators.put(comparator, Snapshot.from(comparator));
    }

    public Map<Comparator<?, ?>, Snapshot> getComparators() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(comparators));
    }

    public Map<Comparator<?, ?>, Snapshot> snapshotMap() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(comparators));
    }

    public static record Snapshot(
            boolean supportsOrdering,
            boolean supportsInversion
    ) {
        static Snapshot from(Comparator<?, ?> comparator) {
            return new Snapshot(
                    comparator.supportsOrdering(),
                    comparator.supportsInversion()
            );
        }
    }

}
