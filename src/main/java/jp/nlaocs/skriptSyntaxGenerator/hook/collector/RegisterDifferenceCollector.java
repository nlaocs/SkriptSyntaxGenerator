package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.arithmetic.Operation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegisterDifferenceCollector extends AbstractMapHookCollector<RegisterDifferenceCollector.Registration, RegisterDifferenceCollector.Key, RegisterDifferenceCollector.Snapshot> {

    private static final RegisterDifferenceCollector INSTANCE = new RegisterDifferenceCollector();

    private final AtomicInteger registrationOrder = new AtomicInteger();

    private RegisterDifferenceCollector() {
    }

    public static RegisterDifferenceCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... args) {
        // The two-argument overload delegates to this canonical overload.
        if (args == null || args.length != 3) {
            return false;
        }
        return args[0] instanceof Class<?>
                && args[1] instanceof Class<?>
                && args[2] instanceof Operation<?, ?, ?>;
    }

    public void addFromHook(Object... args) {
        if (!isValidArguments(args)) {
            return;
        }

        Class<?> type = (Class<?>) args[0];
        Class<?> returnType = (Class<?>) args[1];
        Operation<?, ?, ?> operation = (Operation<?, ?, ?>) args[2];
        add(new Registration(type, returnType, operation));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.type(), registration.returnType());
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

    public Map<Key, Snapshot> getDifferences() {
        return snapshotMap();
    }

    @Override
    public void clear() {
        super.clear();
        registrationOrder.set(0);
    }

    public record Registration(
            Class<?> type,
            Class<?> returnType,
            Operation<?, ?, ?> operation
    ) {
    }

    public record Key(
            Class<?> type,
            Class<?> returnType
    ) {
    }

    public record Snapshot(
            String addonName,
            String addonVersion,
            int registrationOrder
    ) {
    }
}
