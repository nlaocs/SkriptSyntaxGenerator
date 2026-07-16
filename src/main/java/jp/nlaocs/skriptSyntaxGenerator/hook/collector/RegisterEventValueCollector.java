package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegisterEventValueCollector implements HookCollector<RegisterEventValueCollector.Registration, RegisterEventValueCollector.Key, RegisterEventValueCollector.Snapshot> {

    private static final RegisterEventValueCollector INSTANCE = new RegisterEventValueCollector();

    private final ConcurrentMap<Key, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final AtomicInteger registrationOrder = new AtomicInteger();

    private RegisterEventValueCollector() {
    }

    public static RegisterEventValueCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(Registration registration) {
        if (registration == null) {
            return;
        }

        Key key = new Key(registration.eventClass(), registration.valueClass(), registration.time());
        snapshots.computeIfAbsent(
                key,
                ignored -> new Snapshot(registration.addon(), registrationOrder.getAndIncrement())
        );
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || args.length != 6) {
            return false;
        }
        return args[0] instanceof Class<?> eventClass
                && Event.class.isAssignableFrom(eventClass)
                && args[1] instanceof Class<?>
                && args[2] instanceof Converter<?, ?>
                && args[3] instanceof Number
                && (args[4] == null || args[4] instanceof String)
                && (args[5] == null || args[5] instanceof Class<?>[]);
    }

    public void addFromHook(Object[] args, AddonInfo addon) {
        if (!isValidArguments(args)) {
            return;
        }
        add(new Registration(
                (Class<?>) args[0],
                (Class<?>) args[1],
                ((Number) args[3]).intValue(),
                addon
        ));
    }

    public Snapshot snapshotFor(Class<?> eventClass, Class<?> valueClass, int time) {
        return snapshots.get(new Key(eventClass, valueClass, time));
    }

    @Override
    public Map<Key, Snapshot> snapshotMap() {
        return Map.copyOf(snapshots);
    }

    @Override
    public void clear() {
        snapshots.clear();
        registrationOrder.set(0);
    }

    public record Registration(
            Class<?> eventClass,
            Class<?> valueClass,
            int time,
            AddonInfo addon
    ) {
    }

    public record Key(
            Class<?> eventClass,
            Class<?> valueClass,
            int time
    ) {
    }

    public record Snapshot(
            AddonInfo addon,
            int registrationOrder
    ) {
    }
}
