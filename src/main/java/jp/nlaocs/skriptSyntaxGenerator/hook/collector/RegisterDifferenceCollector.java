package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.lang.arithmetic.Operation;

import java.util.Map;

public final class RegisterDifferenceCollector extends AbstractMapHookCollector<RegisterDifferenceCollector.Registration, RegisterDifferenceCollector.Key, RegisterDifferenceCollector.Snapshot> {

    private static final RegisterDifferenceCollector INSTANCE = new RegisterDifferenceCollector();

    private RegisterDifferenceCollector() {
    }

    public static RegisterDifferenceCollector getInstance() {
        return INSTANCE;
    }

    public void addFromHook(Object... args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            return;
        }

        Class<?> type = args[0] instanceof Class<?> clazz ? clazz : null;
        if (type == null) {
            return;
        }

        if (args.length == 2) {
            if (!(args[1] instanceof Operation<?, ?, ?> operation)) {
                return;
            }
            add(new Registration(type, type, operation));
            return;
        }

        Class<?> returnType = args[1] instanceof Class<?> clazz ? clazz : null;
        if (returnType == null || !(args[2] instanceof Operation<?, ?, ?> operation)) {
            return;
        }

        add(new Registration(type, returnType, operation));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.type(), registration.returnType());
    }

    @Override
    protected Snapshot snapshotOf(Registration registration) {
        return Snapshot.from(registration);
    }

    public Map<Key, Snapshot> getDifferences() {
        return snapshotMap();
    }

    public static record Registration(
            Class<?> type,
            Class<?> returnType,
            Operation<?, ?, ?> operation
    ) {
    }

    public static record Key(
            Class<?> type,
            Class<?> returnType
    ) {
    }

    public static record Snapshot(
            String addonName,
            String addonVersion
    ) {
        static Snapshot from(Registration registration) {
            Plugin plugin = resolvePlugin(registration);
            return new Snapshot(
                    plugin != null ? plugin.getName() : null,
                    plugin != null ? plugin.getDescription().getVersion() : null
            );
        }

        private static Plugin resolvePlugin(Registration registration) {
            Class<?>[] candidates = new Class<?>[]{
                    registration.operation().getClass(),
                    registration.type(),
                    registration.returnType()
            };

            for (Class<?> candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                try {
                    return JavaPlugin.getProvidingPlugin(candidate);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
    }
}
