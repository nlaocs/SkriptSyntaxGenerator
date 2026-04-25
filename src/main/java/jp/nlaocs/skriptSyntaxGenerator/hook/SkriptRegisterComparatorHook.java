package jp.nlaocs.skriptSyntaxGenerator.hook;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.*;

public final class SkriptRegisterComparatorHook {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private SkriptRegisterComparatorHook() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) return;

        Instrumentation inst = ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named("org.skriptlang.skript.lang.comparator.Comparators"))
                .transform((builder, td, cl, module, pd) ->
                        builder.visit(
                                Advice.to(SkriptRegisterComparatorAdvice.class)
                                        .on(named("registerComparator"))
                        )
                )
                .installOn(inst);

        try {
            inst.retransformClasses(
                    Class.forName("org.skriptlang.skript.lang.comparator.Comparators")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
