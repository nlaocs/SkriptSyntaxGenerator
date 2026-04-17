package jp.nlaocs.skriptSyntaxGenerator.hook;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.bukkit.Bukkit;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.*;

public final class SkriptClassesRegisterHook {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private SkriptClassesRegisterHook() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) return;

        Instrumentation inst = ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named("ch.njol.skript.registrations.Classes"))
                .transform((builder, td, cl, module, pd) ->
                        builder.visit(
                                Advice.to(SkriptRegisterClassAdvice.class)
                                        .on(named("registerClass"))
                        )
                )
                .installOn(inst);

        try {
            inst.retransformClasses(
                    Class.forName("ch.njol.skript.registrations.Classes")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
