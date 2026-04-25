package jp.nlaocs.skriptSyntaxGenerator.hook;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.named;

public abstract class AbstractRetransformHook implements Hook {

    private final AtomicBoolean installed = new AtomicBoolean(false);

    @Override
    public final void install(Instrumentation instrumentation) throws Exception {
        if (!installed.compareAndSet(false, true)) return;

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named(targetClassName()))
                .transform((builder, td, cl, module, pd) ->
                        builder.visit(
                                Advice.to(adviceClass())
                                        .on(named(targetMethodName()))
                        )
                )
                .installOn(instrumentation);

        instrumentation.retransformClasses(Class.forName(targetClassName()));
    }

    protected abstract String targetClassName();

    protected abstract String targetMethodName();

    protected abstract Class<?> adviceClass();
}

