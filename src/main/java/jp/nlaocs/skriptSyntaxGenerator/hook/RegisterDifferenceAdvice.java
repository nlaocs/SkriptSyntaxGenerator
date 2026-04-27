package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterDifferenceCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterDifferenceAdvice {
    private RegisterDifferenceAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.AllArguments final Object[] args) {
        RegisterDifferenceCollector.getInstance().addFromHook(args);
    }
}
