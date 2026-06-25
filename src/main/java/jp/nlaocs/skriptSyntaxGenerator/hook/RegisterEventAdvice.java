package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.HookCallerResolver;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventCollector;
import net.bytebuddy.asm.Advice;
import org.bukkit.plugin.Plugin;

public final class RegisterEventAdvice {
    private RegisterEventAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.AllArguments final Object[] args) {
        Plugin plugin = HookCallerResolver.resolvePlugin();
        AddonInfo addon = plugin != null
                ? new AddonInfo(plugin.getName(), plugin.getDescription().getVersion())
                : null;
        RegisterEventCollector.getInstance().addFromHook(args, addon);
    }
}
