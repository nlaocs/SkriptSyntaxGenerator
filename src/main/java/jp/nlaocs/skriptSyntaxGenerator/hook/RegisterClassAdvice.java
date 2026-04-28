package jp.nlaocs.skriptSyntaxGenerator.hook;

import ch.njol.skript.classes.ClassInfo;
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.HookCallerResolver;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterClassCollector;
import net.bytebuddy.asm.Advice;
import org.bukkit.plugin.Plugin;

public final class RegisterClassAdvice {
    private RegisterClassAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static <T> void onEnter(@Advice.Argument(0) final ClassInfo<T> arg0) {
        Plugin plugin = HookCallerResolver.resolvePlugin();
        AddonInfo addon = plugin != null
                ? new AddonInfo(plugin.getName(), plugin.getDescription().getVersion())
                : null;
        RegisterClassCollector.getInstance().addFromHook(arg0, addon);
    }
}
