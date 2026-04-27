package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.lang.converter.Converter;

import java.util.Map;

public final class RegisterConverterCollector extends AbstractMapHookCollector<RegisterConverterCollector.Registration, RegisterConverterCollector.Key, RegisterConverterCollector.Snapshot> {

    private static final RegisterConverterCollector INSTANCE = new RegisterConverterCollector();

    private RegisterConverterCollector() {
    }

    public static RegisterConverterCollector getInstance() {
        return INSTANCE;
    }

    public void addFromHook(Object... args) {
        if (args == null || (args.length != 3 && args.length != 4)) {
            return;
        }

        Class<?> from = args[0] instanceof Class<?> clazz ? clazz : null;
        Class<?> to = args[1] instanceof Class<?> clazz ? clazz : null;
        if (from == null || to == null || !(args[2] instanceof Converter<?, ?> converter)) {
            return;
        }

        int flags = 0;
        if (args.length == 4) {
            if (!(args[3] instanceof Number number)) {
                return;
            }
            flags = number.intValue();
        }

        add(new Registration(from, to, converter, flags));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.from(), registration.to(), registration.flags());
    }

    @Override
    protected Snapshot snapshotOf(Registration registration) {
        return Snapshot.from(registration);
    }

    public Map<Key, Snapshot> getConverters() {
        return snapshotMap();
    }

    public static record Registration(
            Class<?> from,
            Class<?> to,
            Converter<?, ?> converter,
            int flags
    ) {
    }

    public static record Key(
            Class<?> from,
            Class<?> to,
            int flags
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
                    registration.converter().getClass(),
                    registration.from(),
                    registration.to()
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

