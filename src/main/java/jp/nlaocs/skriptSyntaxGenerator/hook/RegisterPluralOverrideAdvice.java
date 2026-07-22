package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterPluralOverrideCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterPluralOverrideAdvice {
    private RegisterPluralOverrideAdvice() {
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.AllArguments final Object[] arguments,
        @Advice.Thrown final Throwable thrown
    ) {
        if (thrown == null) {
            RegisterPluralOverrideCollector.getInstance().addFromHook(arguments);
        }
    }
}
