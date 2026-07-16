package jp.nlaocs.skriptSyntaxGenerator.hook;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.named;

public abstract class AbstractRetransformHook implements Hook {

    private final AtomicBoolean installed = new AtomicBoolean(false);

    @Override
    public final void install(Instrumentation instrumentation) throws Exception {
        if (!installed.compareAndSet(false, true)) return;

        Set<Class<?>> targetClasses = new LinkedHashSet<>();
        try {
            targetClasses.add(Class.forName(targetClassName(), false, getClass().getClassLoader()));
        } catch (ClassNotFoundException ignored) {
        }

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named(targetClassName()))
                .transform((builder, td, cl, module, pd) ->
                        builder.visit(
                                Advice.to(adviceClass())
                                        .on(named(targetMethodName()))
                        )
                )
                .installOn(instrumentation);

        Arrays.stream(instrumentation.getAllLoadedClasses())
                .filter(type -> type.getName().equals(targetClassName()))
                .filter(instrumentation::isModifiableClass)
                .forEach(targetClasses::add);

        if (targetClasses.isEmpty() && !optionalTarget()) {
            throw new ClassNotFoundException(targetClassName());
        }
        if (!targetClasses.isEmpty()) {
            instrumentation.retransformClasses(targetClasses.toArray(Class<?>[]::new));
        }
    }

    protected abstract String targetClassName();

    protected abstract String targetMethodName();

    protected abstract Class<?> adviceClass();

    protected boolean optionalTarget() {
        return false;
    }
}
