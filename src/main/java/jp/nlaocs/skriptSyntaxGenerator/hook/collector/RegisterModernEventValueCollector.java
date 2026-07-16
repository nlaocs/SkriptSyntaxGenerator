package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegisterModernEventValueCollector implements HookCollector<RegisterModernEventValueCollector.Registration, RegisterModernEventValueCollector.IdentityKey, RegisterModernEventValueCollector.Snapshot> {

    private static final RegisterModernEventValueCollector INSTANCE = new RegisterModernEventValueCollector();

    private final ConcurrentMap<IdentityKey, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final AtomicInteger registrationOrder = new AtomicInteger();

    private RegisterModernEventValueCollector() {
    }

    public static RegisterModernEventValueCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(Registration registration) {
        if (registration == null || registration.eventValue() == null) {
            return;
        }

        snapshots.computeIfAbsent(
                new IdentityKey(registration.eventValue()),
                ignored -> new Snapshot(registration.addon(), registrationOrder.getAndIncrement())
        );
    }

    public void addFromHook(Object eventValue, AddonInfo addon) {
        add(new Registration(eventValue, addon));
    }

    public Snapshot snapshotFor(Object eventValue) {
        return eventValue == null ? null : snapshots.get(new IdentityKey(eventValue));
    }

    @Override
    public Map<IdentityKey, Snapshot> snapshotMap() {
        return Map.copyOf(snapshots);
    }

    @Override
    public void clear() {
        snapshots.clear();
        registrationOrder.set(0);
    }

    public record Registration(Object eventValue, AddonInfo addon) {
    }

    public static final class IdentityKey {
        private final Object value;

        public IdentityKey(Object value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof IdentityKey key && value == key.value;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(value);
        }
    }

    public record Snapshot(AddonInfo addon, int registrationOrder) {
    }
}
