package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import ch.njol.skript.classes.ClassInfo;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisteredClassInfoCollector {

    private static final RegisteredClassInfoCollector INSTANCE = new RegisteredClassInfoCollector();

    private final Set<ClassInfo<?>> infos = ConcurrentHashMap.newKeySet();

    private RegisteredClassInfoCollector() {
    }

    public static RegisteredClassInfoCollector getInstance() {
        return INSTANCE;
    }

    public void add(ClassInfo<?> info) {
        if (info != null) {
            infos.add(info);
        }
    }

    public List<ClassInfo<?>> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(infos));
    }

    public void clear() {
        infos.clear();
    }
}
