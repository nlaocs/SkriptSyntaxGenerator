package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterModernEventValueHook extends AbstractRetransformHook {

    public static final RegisterModernEventValueHook INSTANCE = new RegisterModernEventValueHook();

    private RegisterModernEventValueHook() {
    }

    @Override
    public String name() {
        return "SkriptModernEventValueHook";
    }

    @Override
    protected String targetClassName() {
        return "org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistryImpl";
    }

    @Override
    protected String targetMethodName() {
        return "register";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterModernEventValueAdvice.class;
    }

    @Override
    protected boolean optionalTarget() {
        return true;
    }
}
