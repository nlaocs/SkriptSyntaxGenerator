package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import ch.njol.skript.classes.ClassInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisteredClassInfoCollector {

    private static final RegisteredClassInfoCollector INSTANCE = new RegisteredClassInfoCollector();

    // key: codeName, value: Snapshot
    private final Map<String, Snapshot> infos = new ConcurrentHashMap<>();

    private RegisteredClassInfoCollector() {
    }

    public static RegisteredClassInfoCollector getInstance() {
        return INSTANCE;
    }

    public void add(ClassInfo<?> info) {
        if (info == null) return;

        final String codeName = info.getCodeName();
        if (codeName == null) return;
        infos.put(codeName, Snapshot.from(info));
    }

    public List<Snapshot> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(infos.values()));
    }

    public Map<String, Snapshot> snapshotMap() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(infos));
    }

    public void clear() {
        infos.clear();
    }

    public static record Snapshot(
            Set<String> before,
            Set<String> after
    ) {
        static Snapshot from(ClassInfo<?> info) {
            return new Snapshot(
                    Set.copyOf(info.before()),
                    Set.copyOf(info.after())
            );
        }
    }
}
