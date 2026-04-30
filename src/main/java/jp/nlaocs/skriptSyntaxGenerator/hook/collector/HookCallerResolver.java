package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class HookCallerResolver {
    private static final Logger LOGGER = Bukkit.getLogger();

    private static final String OWN_PACKAGE_PREFIX = "jp.nlaocs.skriptSyntaxGenerator.";
    private static final String HOOK_PACKAGE_PREFIX = "jp.nlaocs.skriptSyntaxGenerator.hook.";
    private static final String SKRIPT_CORE_PACKAGE_PREFIX = "ch.njol.skript.";
    private static final String SKRIPT_LANG_PACKAGE_PREFIX = "org.skriptlang.skript.";
    private static final String[] IGNORED_PREFIXES = {
            OWN_PACKAGE_PREFIX,
            HOOK_PACKAGE_PREFIX,
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "com.sun.",
            "kotlin.",
            "org.jetbrains.",
            "net.bytebuddy.",
            "org.objectweb.",
            "org.bukkit."
    };

    private HookCallerResolver() {
    }

    public static Plugin resolvePlugin() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Plugin skriptPlugin = null;

        LOGGER.info("[HookCallerResolver] ===== Stack Trace Analysis Start =====");
        LOGGER.info("[HookCallerResolver] Total frames: " + stackTrace.length);

        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement frame = stackTrace[i];
            String className = frame.getClassName();

            LOGGER.info("[HookCallerResolver] [" + i + "] " + className + "." + frame.getMethodName());

            if (shouldSkip(className)) {
                LOGGER.info("[HookCallerResolver]   -> SKIPPED");
                continue;
            }

            LOGGER.info("[HookCallerResolver]   -> Processing...");

            Class<?> candidate = resolveClass(className);
            if (candidate == null) {
                LOGGER.info("[HookCallerResolver]   -> Failed to resolve class");
                continue;
            }

            Plugin plugin = tryResolvePlugin(candidate);
            if (plugin == null) {
                LOGGER.info("[HookCallerResolver]   -> No plugin providing this class");
                continue;
            }

            LOGGER.info("[HookCallerResolver]   -> Found plugin: " + plugin.getName());

            if (!isSkriptInternal(className)) {
                LOGGER.info("[HookCallerResolver] ===== RESULT: " + plugin.getName() + " (Non-Skript) =====");
                return plugin;
            }

            if (skriptPlugin == null) {
                LOGGER.info("[HookCallerResolver]   -> Storing as fallback Skript plugin");
                skriptPlugin = plugin;
            }
        }

        LOGGER.info("[HookCallerResolver] ===== RESULT: " + (skriptPlugin != null ? skriptPlugin.getName() : "null") + " (Skript) =====");
        return skriptPlugin;
    }

    private static boolean shouldSkip(String className) {
        if (className == null || className.isEmpty()) {
            return true;
        }

        for (String ignoredPrefix : IGNORED_PREFIXES) {
            if (className.startsWith(ignoredPrefix)) {
                return true;
            }
        }
        return "java.lang.Thread".equals(className)
                || className.startsWith("net.bytebuddy.")
                || className.startsWith("org.objectweb.");
    }

    private static boolean isSkriptInternal(String className) {
        return className.startsWith(SKRIPT_CORE_PACKAGE_PREFIX)
                || className.startsWith(SKRIPT_LANG_PACKAGE_PREFIX);
    }

    private static Class<?> resolveClass(String className) {
        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                return Class.forName(className, false, contextLoader);
            }
        } catch (ClassNotFoundException ignored) {
        }

        try {
            ClassLoader ownLoader = HookCallerResolver.class.getClassLoader();
            if (ownLoader != null) {
                return Class.forName(className, false, ownLoader);
            }
        } catch (ClassNotFoundException ignored) {
        }

        return null;
    }

    private static Plugin tryResolvePlugin(Class<?> candidate) {
        try {
            return JavaPlugin.getProvidingPlugin(candidate);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
