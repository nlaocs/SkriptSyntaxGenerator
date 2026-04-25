package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterComparatorCollector;
import net.bytebuddy.asm.Advice;
import org.skriptlang.skript.lang.comparator.Comparator;

public class SkriptRegisterComparatorAdvice {
    private SkriptRegisterComparatorAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static <T1, T2> void onEnter(
            @Advice.Argument(0) final Class<T1> firstType,
            @Advice.Argument(1) final Class<T2> secondType,
            @Advice.Argument(2) final Comparator<T1, T2> comparator
    ) {
        RegisterComparatorCollector.getInstance().add(comparator);

    }
}
