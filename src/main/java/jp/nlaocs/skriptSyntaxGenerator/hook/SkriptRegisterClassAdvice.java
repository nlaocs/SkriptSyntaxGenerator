package jp.nlaocs.skriptSyntaxGenerator.hook;

import ch.njol.skript.classes.ClassInfo;
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisteredClassInfoCollector;
import net.bytebuddy.asm.Advice;

public final class SkriptRegisterClassAdvice {
    private SkriptRegisterClassAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static <T> void onEnter(@Advice.Argument(0) final ClassInfo<T> arg0) {
        RegisteredClassInfoCollector.getInstance().add(arg0);
    }
}
