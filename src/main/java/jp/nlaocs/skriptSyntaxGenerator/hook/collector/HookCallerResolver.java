package jp.nlaocs.skriptSyntaxGenerator.hook.collector;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

import jp.nlaocs.skriptSyntaxGenerator.hook.HookLogOptions;

public final class HookCallerResolver {
    private static final Logger LOGGER = Bukkit.getLogger();
    private static final boolean HOOK_LOG_ENABLED = HookLogOptions.isEnabled();

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

        log("[HookCallerResolver] ===== Stack Trace Analysis Start =====");
        log("[HookCallerResolver] Total frames: " + stackTrace.length);

        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement frame = stackTrace[i];
            String className = frame.getClassName();

            log("[HookCallerResolver] [" + i + "] " + className + "." + frame.getMethodName());

            if (shouldSkip(className)) {
                log("[HookCallerResolver]   -> SKIPPED");
                continue;
            }

            log("[HookCallerResolver]   -> Processing...");

            Class<?> candidate = resolveClass(className);
            if (candidate == null) {
                log("[HookCallerResolver]   -> Failed to resolve class");
                continue;
            }

            Plugin plugin = tryResolvePlugin(candidate);
            if (plugin == null) {
                log("[HookCallerResolver]   -> No plugin providing this class");
                continue;
            }

            log("[HookCallerResolver]   -> Found plugin: " + plugin.getName());

            if (!isSkriptInternal(className)) {
                log("[HookCallerResolver] ===== RESULT: " + plugin.getName() + " (Non-Skript) =====");
                return plugin;
            }

            if (skriptPlugin == null) {
                log("[HookCallerResolver]   -> Storing as fallback Skript plugin");
                skriptPlugin = plugin;
            }
        }

        log("[HookCallerResolver] ===== RESULT: " + (skriptPlugin != null ? skriptPlugin.getName() : "null") + " (Skript) =====");
        return skriptPlugin;
    }

    private static void log(String message) {
        if (HOOK_LOG_ENABLED) {
            LOGGER.info(message);
        }
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
