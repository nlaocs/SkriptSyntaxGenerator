package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterClassHook extends AbstractRetransformHook {

    public static final RegisterClassHook INSTANCE = new RegisterClassHook();

    private RegisterClassHook() {
    }

    @Override
    public String name() {
        return "SkriptClassesRegisterHook";
    }

    @Override
    protected String targetClassName() {
        return "ch.njol.skript.registrations.Classes";
    }

    @Override
    protected String targetMethodName() {
        return "registerClass";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterClassAdvice.class;
    }
}
