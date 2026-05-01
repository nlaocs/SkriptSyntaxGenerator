package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.converter.Converter;

import java.util.Map;

public final class RegisterConverterCollector extends AbstractMapHookCollector<RegisterConverterCollector.Registration, RegisterConverterCollector.Key, RegisterConverterCollector.Snapshot> {

    private static final RegisterConverterCollector INSTANCE = new RegisterConverterCollector();

    private RegisterConverterCollector() {
    }

    public static RegisterConverterCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || (args.length != 3 && args.length != 4)) {
            return false;
        }
        // args[0] = Class<?> from, args[1] = Class<?> to, args[2] = Converter<?, ?>
        if (!(args[0] instanceof Class<?>)) {
            return false;
        }
        if (!(args[1] instanceof Class<?>)) {
            return false;
        }
        if (!(args[2] instanceof Converter<?, ?>)) {
            return false;
        }
        // 4-arg: optional int flags
        if (args.length == 4) {
            return args[3] instanceof Number;
        }
        return true;
    }

    public void addFromHook(Object... args) {
        if (!isValidArguments(args)) {
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

    public record Registration(
            Class<?> from,
            Class<?> to,
            Converter<?, ?> converter,
            int flags
    ) {
    }

    public record Key(
            Class<?> from,
            Class<?> to,
            int flags
    ) {
    }

    public record Snapshot(
            String addonName,
            String addonVersion
    ) {
        static Snapshot from(Registration registration) {
            Plugin plugin = resolvePlugin();
            return new Snapshot(
                    plugin != null ? plugin.getName() : null,
                    plugin != null ? plugin.getDescription().getVersion() : null
            );
        }

        private static Plugin resolvePlugin() {
            return HookCallerResolver.resolvePlugin();
        }
    }
}

