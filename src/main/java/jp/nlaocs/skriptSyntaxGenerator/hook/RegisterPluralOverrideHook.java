package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterPluralOverrideHook extends AbstractRetransformHook {

    public static final RegisterPluralOverrideHook INSTANCE = new RegisterPluralOverrideHook();

    private RegisterPluralOverrideHook() {
    }

    @Override
    public String name() {
        return "SkriptPluralOverrideHook";
    }

    @Override
    protected String targetClassName() {
        return "ch.njol.skript.util.Utils";
    }

    @Override
    protected String targetMethodName() {
        return "addPluralOverride";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterPluralOverrideAdvice.class;
    }

    @Override
    protected boolean optionalTarget() {
        return true;
    }
}
