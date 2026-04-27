package jp.nlaocs.skriptSyntaxGenerator.hook;

import ch.njol.skript.classes.ClassInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterClassCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterClassAdvice {
    private RegisterClassAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static <T> void onEnter(@Advice.Argument(0) final ClassInfo<T> arg0) {
        RegisterClassCollector.getInstance().add(arg0);
    }
}
