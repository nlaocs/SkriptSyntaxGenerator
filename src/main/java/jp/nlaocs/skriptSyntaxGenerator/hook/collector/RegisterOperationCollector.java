package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.arithmetic.Operator;

import java.util.Map;

public final class RegisterOperationCollector extends AbstractMapHookCollector<RegisterOperationCollector.Registration, RegisterOperationCollector.Key, RegisterOperationCollector.Snapshot> {

    private static final RegisterOperationCollector INSTANCE = new RegisterOperationCollector();

    private RegisterOperationCollector() {
    }

    public static RegisterOperationCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || args.length != 5) {
            return false;
        }
        // Check: Operator, Class, Class, Class, Operation
        if (!(args[0] instanceof org.skriptlang.skript.lang.arithmetic.Operator)) {
            return false;
        }
        if (!(args[1] instanceof Class<?>)) {
            return false;
        }
        if (!(args[2] instanceof Class<?>)) {
            return false;
        }
        if (!(args[3] instanceof Class<?>)) {
            return false;
        }
        // args[4] is Operation - type check omitted for now
        return true;
    }

    public void addFromHook(Object... args) {
        if (!isValidArguments(args)) {
            return;
        }

        if (!(args[0] instanceof Operator operator)) {
            return;
        }

        Class<?> leftClass = args[1] instanceof Class<?> clazz ? clazz : null;
        Class<?> rightClass = args[2] instanceof Class<?> clazz ? clazz : null;
        Class<?> returnType = args[3] instanceof Class<?> clazz ? clazz : null;
        if (leftClass == null || rightClass == null || returnType == null) {
            return;
        }

        add(new Registration(operator, leftClass, rightClass, returnType));
    }

    @Override
    protected Key keyOf(Registration registration) {
        return new Key(registration.operator().sign(), registration.leftType(), registration.rightType(), registration.returnType());
    }

    @Override
    protected Snapshot snapshotOf(Registration registration) {
        return Snapshot.from(registration);
    }

    public Map<Key, Snapshot> getOperations() {
        return snapshotMap();
    }

    public record Registration(
            Operator operator,
            Class<?> leftType,
            Class<?> rightType,
            Class<?> returnType
    ) {
    }

    public record Key(
            String operatorSign,
            Class<?> leftType,
            Class<?> rightType,
            Class<?> returnType
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



