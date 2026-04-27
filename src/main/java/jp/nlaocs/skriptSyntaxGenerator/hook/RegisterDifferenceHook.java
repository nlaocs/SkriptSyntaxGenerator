package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterDifferenceHook extends AbstractRetransformHook {

    public static final RegisterDifferenceHook INSTANCE = new RegisterDifferenceHook();

    private RegisterDifferenceHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterDifferenceHook";
    }

    @Override
    protected String targetClassName() {
        return "org.skriptlang.skript.lang.arithmetic.Arithmetics";
    }

    @Override
    protected String targetMethodName() {
        return "registerDifference";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterDifferenceAdvice.class;
    }
}
