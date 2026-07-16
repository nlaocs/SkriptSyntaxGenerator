package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.HookCallerResolver;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventValueCollector;
import net.bytebuddy.asm.Advice;
import org.bukkit.plugin.Plugin;

public final class RegisterEventValueAdvice {
    private RegisterEventValueAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.AllArguments final Object[] args) {
        Plugin plugin = HookCallerResolver.resolvePlugin();
        AddonInfo addon = plugin != null
                ? new AddonInfo(plugin.getName(), plugin.getDescription().getVersion())
                : null;
        RegisterEventValueCollector.getInstance().addFromHook(args, addon);
    }
}