package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import ch.njol.skript.lang.SkriptEvent;
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import org.bukkit.event.Event;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RegisterEventCollector implements HookCollector<RegisterEventCollector.Registration, RegisterEventCollector.Key, List<RegisterEventCollector.Snapshot>> {

    private static final RegisterEventCollector INSTANCE = new RegisterEventCollector();

    private final ConcurrentMap<Key, CopyOnWriteArrayList<Snapshot>> snapshots = new ConcurrentHashMap<>();

    private RegisterEventCollector() {
    }

    public static RegisterEventCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(Registration registration) {
        if (registration == null) {
            return;
        }

        Key key = keyOf(registration);
        if (key == null) {
            return;
        }

        Snapshot snapshot = snapshotOf(registration);
        if (snapshot == null) {
            return;
        }

        snapshots.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(snapshot);
    }

    @Override
    public boolean isValidArguments(Object... args) {
        if (args == null || args.length != 4) {
            return false;
        }
        if (!(args[0] instanceof String)) {
            return false;
        }
        if (!(args[1] instanceof Class<?> eventClass) || !SkriptEvent.class.isAssignableFrom(eventClass)) {
            return false;
        }
        if (!(args[2] instanceof Class<?>[] events)) {
            return false;
        }
        for (Class<?> event : events) {
            if (event == null || !Event.class.isAssignableFrom(event)) {
                return false;
            }
        }
        return args[3] instanceof String[];
    }

    public void addFromHook(Object[] args, AddonInfo addon) {
        if (!isValidArguments(args)) {
            return;
        }

        String name = (String) args[0];
        Class<?> eventClass = (Class<?>) args[1];
        Class<?>[] eventArray = (Class<?>[]) args[2];
        String[] patterns = (String[]) args[3];

        add(new Registration(eventClass, name, List.of(patterns), List.of(eventArray), addon));
    }

    protected Key keyOf(Registration registration) {
        return keyOf(registration.eventClass(), registration.name(), registration.patterns(), registration.events());
    }

    protected Snapshot snapshotOf(Registration registration) {
        return new Snapshot(registration.addon());
    }

    public Snapshot snapshotFor(Key key, int occurrence) {
        if (occurrence < 0) {
            return null;
        }

        List<Snapshot> values = snapshots.get(key);
        if (values == null || occurrence >= values.size()) {
            return null;
        }
        return values.get(occurrence);
    }

    public static Key keyOf(
            Class<?> eventClass,
            String name,
            Collection<String> patterns,
            Collection<? extends Class<?>> events
    ) {
        if (eventClass == null || name == null || patterns == null || events == null) {
            return null;
        }
        return new Key(
                eventClass,
                normalizeName(name),
                normalizePatterns(patterns),
                normalizeEvents(events)
        );
    }

    @Override
    public Map<Key, List<Snapshot>> snapshotMap() {
        Map<Key, List<Snapshot>> copy = new HashMap<>();
        snapshots.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    @Override
    public void clear() {
        snapshots.clear();
    }

    @Override
    public int size() {
        int size = 0;
        for (List<Snapshot> value : snapshots.values()) {
            size += value.size();
        }
        return size;
    }

    private static String normalizeName(String name) {
        String normalized = name;
        if (normalized.startsWith("*")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("On ")) {
            normalized = normalized.substring(3);
        }
        return normalized;
    }

    private static List<String> normalizePatterns(Collection<String> patterns) {
        List<String> normalized = new ArrayList<>(patterns.size());
        for (String pattern : patterns) {
            if (pattern != null) {
                normalized.add(BukkitSyntaxInfos.fixPattern(pattern));
            }
        }
        return List.copyOf(normalized);
    }

    private static List<Class<?>> normalizeEvents(Collection<? extends Class<?>> events) {
        List<Class<?>> normalized = new ArrayList<>(events.size());
        for (Class<?> event : events) {
            if (event != null) {
                normalized.add(event);
            }
        }
        return List.copyOf(normalized);
    }

    public record Registration(
            Class<?> eventClass,
            String name,
            List<String> patterns,
            List<Class<?>> events,
            AddonInfo addon
    ) {
    }

    public record Key(
            Class<?> eventClass,
            String normalizedName,
            List<String> patterns,
            List<Class<?>> events
    ) {
    }

    public record Snapshot(
            AddonInfo addon
    ) {
    }
}
