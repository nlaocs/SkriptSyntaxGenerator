package jp.nlaocs.skriptSyntaxGenerator.hook;

import ch.njol.skript.classes.ClassInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisteredClassInfoCollector;
import net.bytebuddy.asm.Advice;
import org.bukkit.Bukkit;

public final class SkriptRegisterClassAdvice {
    private SkriptRegisterClassAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static <T> void onEnter(@Advice.Argument(0) final ClassInfo<T> arg0) {
        RegisteredClassInfoCollector.getInstance().add(arg0);

        // System.out.println("[Hook] registerClass arg0=" + (arg0 == null ? "null" : arg0.getClass().getName()));
        // Bukkit.getLogger().info("[Hook] registerClass arg0=" + (arg0 == null ? "null" : arg0));
        // Bukkit.getLogger().info("[Hook] before " + (arg0 == null ? "null" : arg0.before()));
        // Bukkit.getLogger().info("[Hook] after " + (arg0 == null ? "null" : arg0.after()));
    }
}
