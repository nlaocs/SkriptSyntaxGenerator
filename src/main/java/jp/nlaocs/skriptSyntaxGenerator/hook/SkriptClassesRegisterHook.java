package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class SkriptClassesRegisterHook extends AbstractRetransformHook {

    public static final SkriptClassesRegisterHook INSTANCE = new SkriptClassesRegisterHook();

    private SkriptClassesRegisterHook() {
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
        return SkriptRegisterClassAdvice.class;
    }
}
