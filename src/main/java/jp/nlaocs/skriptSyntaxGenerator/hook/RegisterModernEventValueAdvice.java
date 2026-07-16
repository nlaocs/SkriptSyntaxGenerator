package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.HookCallerResolver;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterModernEventValueCollector;
import net.bytebuddy.asm.Advice;
import org.bukkit.plugin.Plugin;

public final class RegisterModernEventValueAdvice {
    private RegisterModernEventValueAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Argument(0) Object eventValue) {
        Plugin plugin = HookCallerResolver.resolvePlugin();
        AddonInfo addon = plugin != null
                ? new AddonInfo(plugin.getName(), plugin.getDescription().getVersion())
                : null;
        RegisterModernEventValueCollector.getInstance().addFromHook(eventValue, addon);
    }
}
