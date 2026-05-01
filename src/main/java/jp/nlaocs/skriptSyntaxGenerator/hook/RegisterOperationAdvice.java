package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterOperationCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterOperationAdvice {
    private RegisterOperationAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.AllArguments final Object[] args) {
        RegisterOperationCollector.getInstance().addFromHook(args);
    }
}


