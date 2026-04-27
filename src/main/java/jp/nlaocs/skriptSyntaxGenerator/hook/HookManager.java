package jp.nlaocs.skriptSyntaxGenerator.hook;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HookManager {

    private static final HookManager INSTANCE = new HookManager();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final List<Hook> hooks = List.of(
            RegisterClassHook.INSTANCE,
            RegisterComparatorHook.INSTANCE,
            RegisterDifferenceHook.INSTANCE,
            RegisterConverterHook.INSTANCE
    );
    private Logger logger = Logger.getLogger("HookManager");

    private HookManager() {
    }

    public static HookManager getInstance() {
        return INSTANCE;
    }

    public void setLogger(Logger logger) {
        if (logger != null) this.logger = logger;
    }

    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            logger.fine("[HookManager] already initialized. skip.");
            return;
        }

        try {
            Instrumentation instrumentation = ByteBuddyAgent.install();
            for (Hook hook : hooks) {
                hook.install(instrumentation);
            }

            logger.info("[HookManager] hooks initialized.");
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[HookManager] failed to initialize hooks.", t);
        }
    }
}
