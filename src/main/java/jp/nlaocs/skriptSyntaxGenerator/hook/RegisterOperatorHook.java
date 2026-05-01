package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterOperatorHook extends AbstractRetransformHook {

    public static final RegisterOperatorHook INSTANCE = new RegisterOperatorHook();

    private RegisterOperatorHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterOperatorHook";
    }

    @Override
    protected String targetClassName() {
        return "org.skriptlang.skript.lang.arithmetic.Arithmetics";
    }

    @Override
    protected String targetMethodName() {
        return "registerOperation";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterOperatorAdvice.class;
    }
}

