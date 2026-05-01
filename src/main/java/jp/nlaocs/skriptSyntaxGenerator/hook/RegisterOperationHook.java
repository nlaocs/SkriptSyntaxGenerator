package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterOperationHook extends AbstractRetransformHook {

    public static final RegisterOperationHook INSTANCE = new RegisterOperationHook();

    private RegisterOperationHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterOperationHook";
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
        return RegisterOperationAdvice.class;
    }
}

