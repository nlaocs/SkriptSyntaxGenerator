package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterConverterHook extends AbstractRetransformHook {

    public static final RegisterConverterHook INSTANCE = new RegisterConverterHook();

    private RegisterConverterHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterConverterHook";
    }

    @Override
    protected String targetClassName() {
        return "org.skriptlang.skript.lang.converter.Converters";
    }

    @Override
    protected String targetMethodName() {
        return "registerConverter";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterConverterAdvice.class;
    }
}

