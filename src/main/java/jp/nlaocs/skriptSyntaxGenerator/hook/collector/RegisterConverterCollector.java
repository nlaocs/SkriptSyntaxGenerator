package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.converter.Converter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegisterConverterCollector extends AbstractMapHookCollector<RegisterConverterCollector.Registration, RegisterConverterCollector.Key, RegisterConverterCollector.Snapshot> {

    private static final RegisterConverterCollector INSTANCE = new RegisterConverterCollector();

    private final AtomicInteger registrationOrder = new AtomicInteger();

    private RegisterConverterCollector() {
    }

    public static RegisterConverterCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || args.length != 4) {
            return false;
        }
        return args[0] instanceof Class<?>
                && args[1] instanceof Class<?>
                && args[2] instanceof Converter<?, ?>
                && args[3] instanceof Number;
    }

    public void addFromHook(Object... args) {
        if (!isValidArguments(args)) {
            return;
        }

        add(new Registration(
                (Class<?>) args[0],
                (Class<?>) args[1],
                (Converter<?, ?>) args[2],
                ((Number) args[3]).intValue()
        ));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.from(), registration.to(), registration.flags());
    }

    @Override
    protected Snapshot snapshotOf(Registration registration) {
        Plugin plugin = HookCallerResolver.resolvePlugin();
        return new Snapshot(
                plugin != null ? plugin.getName() : null,
                plugin != null ? plugin.getDescription().getVersion() : null,
                registrationOrder.getAndIncrement()
        );
    }

    public Map<Key, Snapshot> getConverters() {
        return snapshotMap();
    }

    @Override
    public void clear() {
        super.clear();
        registrationOrder.set(0);
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
            String addonVersion,
            int registrationOrder
    ) {
    }
}