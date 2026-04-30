package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.skriptlang.skript.lang.comparator.Comparator;

import java.util.Map;

public final class RegisterComparatorCollector extends AbstractMapHookCollector<RegisterComparatorCollector.Registration, RegisterComparatorCollector.Key, RegisterComparatorCollector.Snapshot> {

    private static final RegisterComparatorCollector INSTANCE = new RegisterComparatorCollector();

    private RegisterComparatorCollector() {
    }

    public static RegisterComparatorCollector getInstance() {
        return INSTANCE;
    }

    public <T1, T2> void addFromHook(Class<T1> firstType, Class<T2> secondType, Comparator<T1, T2> comparator) {
        add(new Registration(firstType, secondType, comparator));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.firstType(), registration.secondType(), registration.comparator());
    }

    @Override
    protected Snapshot snapshotOf(Registration registration) {
        return Snapshot.from(registration);
    }

    public Map<Key, Snapshot> getComparators() {
        return snapshotMap();
    }

    public static record Registration(
            Class<?> firstType,
            Class<?> secondType,
            Comparator<?, ?> comparator
    ) {
    }


    public static record Snapshot(
            boolean supportsOrdering,
            boolean supportsInversion,
            String addonName,
            String addonVersion
    ) {
        static Snapshot from(Registration registration) {
            Comparator<?, ?> comparator = registration.comparator();
            var plugin = resolvePlugin();
            return new Snapshot(
                    comparator.supportsOrdering(),
                    comparator.supportsInversion(),
                    plugin != null ? plugin.getName() : null,
                    plugin != null ? plugin.getDescription().getVersion() : null
            );
        }

        private static org.bukkit.plugin.Plugin resolvePlugin() {
            return HookCallerResolver.resolvePlugin();
        }
    }

    public static record Key(
            Class<?> firstType,
            Class<?> secondType,
            Comparator<?, ?> comparator
    ) {
    }

}
