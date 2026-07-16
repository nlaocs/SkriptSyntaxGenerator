package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterEventValueHook extends AbstractRetransformHook {

    public static final RegisterEventValueHook INSTANCE = new RegisterEventValueHook();

    private RegisterEventValueHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterEventValueHook";
    }

    @Override
    protected String targetClassName() {
        return "ch.njol.skript.registrations.EventValues";
    }

    @Override
    protected String targetMethodName() {
        return "registerEventValue";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterEventValueAdvice.class;
    }
}