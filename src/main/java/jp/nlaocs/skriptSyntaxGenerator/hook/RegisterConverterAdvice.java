package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterConverterCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterConverterAdvice {
    private RegisterConverterAdvice() {
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
            @Advice.AllArguments final Object[] args,
            @Advice.Thrown final Throwable thrown
    ) {
        if (thrown == null) {
            RegisterConverterCollector.getInstance().addFromHook(args);
        }
    }
}
