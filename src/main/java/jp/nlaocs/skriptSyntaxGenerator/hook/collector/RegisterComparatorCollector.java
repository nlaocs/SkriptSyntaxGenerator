package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.skriptlang.skript.lang.comparator.Comparator;

import java.util.Map;

public final class RegisterComparatorCollector extends AbstractMapHookCollector<Comparator<?, ?>, Comparator<?, ?>, RegisterComparatorCollector.Snapshot> {

    private static final RegisterComparatorCollector INSTANCE = new RegisterComparatorCollector();

    private RegisterComparatorCollector() {
    }

    public static RegisterComparatorCollector getInstance() {
        return INSTANCE;
    }

    @Override
    protected Comparator<?, ?> keyOf(Comparator<?, ?> comparator) {
        return comparator;
    }

    @Override
    protected Snapshot snapshotOf(Comparator<?, ?> comparator) {
        return Snapshot.from(comparator);
    }

    public Map<Comparator<?, ?>, Snapshot> getComparators() {
        return snapshotMap();
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
