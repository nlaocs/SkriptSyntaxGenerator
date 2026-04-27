package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import ch.njol.skript.classes.ClassInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class RegisterClassCollector extends AbstractMapHookCollector<ClassInfo<?>, String, RegisterClassCollector.Snapshot> {

    private static final RegisterClassCollector INSTANCE = new RegisterClassCollector();

    private RegisterClassCollector() {
    }

    public static RegisterClassCollector getInstance() {
        return INSTANCE;
    }

    @Override
    protected String keyOf(ClassInfo<?> info) {
        return info.getCodeName();
    }

    @Override
    protected Snapshot snapshotOf(ClassInfo<?> info) {
        return Snapshot.from(info);
    }

    public List<Snapshot> snapshot() {
        return List.copyOf(snapshotValues());
    }

    public static record Snapshot(
            Set<String> before,
            Set<String> after
    ) {
        static Snapshot from(ClassInfo<?> info) {
            return new Snapshot(
                    safeCopy(info.before()),
                    safeCopy(info.after())
            );
        }

        private static Set<String> safeCopy(Collection<String> values) {
            if (values == null || values.isEmpty()) {
                return Set.of();
            }
            return Set.copyOf(values);
        }
    } // TODO: replace record if support for legacy Java versions is required.
}
