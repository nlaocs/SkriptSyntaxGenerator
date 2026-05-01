package jp.nlaocs.skriptSyntaxGenerator.hook;

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterOperatorCollector;
import net.bytebuddy.asm.Advice;

public final class RegisterOperatorAdvice {
	private RegisterOperatorAdvice() {
	}

	@Advice.OnMethodEnter(suppress = Throwable.class)
	public static void onEnter(@Advice.AllArguments final Object[] args) {
		RegisterOperatorCollector.getInstance().addFromHook(args);
	}
}

