package jp.nlaocs.skriptSyntaxGenerator.legacy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

final class LegacyAddonResolver {
    private final ClassLoader classLoader;

    LegacyAddonResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Map<String, Object> syntaxAddon(String originClassPath, Class<?> elementClass) {
        Class<?> origin = originClassPath == null ? null : LegacyReflection.classOrNull(originClassPath, classLoader);
        return addonForCandidates(origin, elementClass);
    }

    Map<String, Object> addonForCandidates(Object... candidates) {
        for (Object candidate : candidates) {
            Class<?> type = candidate instanceof Class<?> ? (Class<?>) candidate
                : candidate == null ? null : candidate.getClass();
            Plugin plugin = providingPlugin(type);
            if (plugin != null) return toData(plugin);
        }
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        if (skript == null) {
            throw new IllegalStateException("Skript plugin is not available for addon fallback");
        }
        return toData(skript);
    }

    Map<String, Object> providerForClass(Class<?> type) {
        Plugin plugin = providingPlugin(type);
        return plugin == null ? null : toData(plugin);
    }

    private Plugin providingPlugin(Class<?> type) {
        if (type == null || type.isPrimitive()) return null;
        Class<?> candidate = type.isArray() ? type.getComponentType() : type;
        try {
            return JavaPlugin.getProvidingPlugin(candidate);
        } catch (IllegalArgumentException ignored) {
            if (candidate.getName().startsWith("ch.njol.skript.") ||
                candidate.getName().startsWith("org.skriptlang.skript.")) {
                return Bukkit.getPluginManager().getPlugin("Skript");
            }
            return null;
        }
    }

    static Map<String, Object> toData(Plugin plugin) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("name", plugin.getName());
        data.put("version", plugin.getDescription().getVersion());
        return data;
    }
}
