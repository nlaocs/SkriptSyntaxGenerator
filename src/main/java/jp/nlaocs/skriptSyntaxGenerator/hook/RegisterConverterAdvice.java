package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterConverterCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterConverterAdvice {
    private RegisterConverterAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.AllArguments final Object[] args) {
        RegisterConverterCollector.getInstance().addFromHook(args);
    }
}

