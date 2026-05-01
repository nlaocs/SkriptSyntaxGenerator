package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.arithmetic.Operator;

import java.util.Map;

public final class RegisterOperatorCollector extends AbstractMapHookCollector<Operator, RegisterOperatorCollector.Key, RegisterOperatorCollector.Snapshot> {

    private static final RegisterOperatorCollector INSTANCE = new RegisterOperatorCollector();

    private RegisterOperatorCollector() {
    }

    public static RegisterOperatorCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || args.length == 0) {
            return false;
        }
        // Valid if any argument is an Operator
        for (Object arg : args) {
            if (arg instanceof Operator) {
                return true;
            }
        }
        return false;
    }

    public void addFromHook(Object... args) {
        if (!isValidArguments(args)) {
            return;
        }

        for (Object arg : args) {
            if (arg instanceof Operator operator) {
                add(operator);
                return;
            }
        }
    }

    @Override
    protected Key keyOf(Operator operator) {
        return new Key(operator.sign(), resolveNodeKey(operator));
    }

    @Override
    protected Snapshot snapshotOf(Operator operator) {
        return Snapshot.from();
    }

    public Map<Key, Snapshot> getOperators() {
        return snapshotMap();
    }

    private static String resolveNodeKey(Operator operator) {
        return operator.node() != null ? operator.node().key : null;
    }

    public record Key(
            String sign,
            String key
    ) {
    }

    public record Snapshot(
            String addonName,
            String addonVersion
    ) {
        static Snapshot from() {
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


