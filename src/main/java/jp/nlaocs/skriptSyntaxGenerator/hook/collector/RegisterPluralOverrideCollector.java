package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegisterPluralOverrideCollector extends AbstractMapHookCollector<
    RegisterPluralOverrideCollector.Registration,
    Integer,
    RegisterPluralOverrideCollector.Registration
> {
    private static final RegisterPluralOverrideCollector INSTANCE = new RegisterPluralOverrideCollector();

    private final AtomicInteger registrationOrder = new AtomicInteger();

    private RegisterPluralOverrideCollector() {
    }

    public static RegisterPluralOverrideCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isValidArguments(Object... arguments) {
        return arguments != null
            && arguments.length == 2
            && arguments[0] instanceof String
            && arguments[1] instanceof String;
    }

    public void addFromHook(Object... arguments) {
        if (!isValidArguments(arguments)) return;

        Plugin plugin = HookCallerResolver.resolvePlugin();
        add(new Registration(
            (String) arguments[0],
            (String) arguments[1],
            registrationOrder.getAndIncrement(),
            plugin == null ? null : plugin.getName(),
            plugin == null ? null : plugin.getDescription().getVersion()
        ));
    }

    @Override
    protected Integer keyOf(Registration registration) {
        return registration.registrationOrder();
    }

    @Override
    protected Registration snapshotOf(Registration registration) {
        return registration;
    }

    public List<Registration> getOverrides() {
        List<Registration> result = new ArrayList<>(snapshotValues());
        result.sort(Comparator.comparingInt(Registration::registrationOrder));
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        registrationOrder.set(0);
    }

    public record Registration(
        String singular,
        String plural,
        int registrationOrder,
        String addonName,
        String addonVersion
    ) {
    }
}
