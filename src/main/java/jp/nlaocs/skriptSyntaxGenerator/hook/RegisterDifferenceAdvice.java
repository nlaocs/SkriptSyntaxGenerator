package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterDifferenceCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterDifferenceAdvice {
    private RegisterDifferenceAdvice() {
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
            @Advice.AllArguments final Object[] args,
            @Advice.Thrown final Throwable thrown
    ) {
        if (thrown == null) {
            RegisterDifferenceCollector.getInstance().addFromHook(args);
        }
    }
}
