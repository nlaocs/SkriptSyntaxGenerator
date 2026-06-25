package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterEventHook extends AbstractRetransformHook {

    public static final RegisterEventHook INSTANCE = new RegisterEventHook();

    private RegisterEventHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterEventHook";
    }

    @Override
    protected String targetClassName() {
        return "ch.njol.skript.Skript";
    }

    @Override
    protected String targetMethodName() {
        return "registerEvent";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterEventAdvice.class;
    }
}
