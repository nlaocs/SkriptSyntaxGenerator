package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class RegisterComparatorHook extends AbstractRetransformHook {

    public static final RegisterComparatorHook INSTANCE = new RegisterComparatorHook();

    private RegisterComparatorHook() {
    }

    @Override
    public String name() {
        return "SkriptRegisterComparatorHook";
    }

    @Override
    protected String targetClassName() {
        return "org.skriptlang.skript.lang.comparator.Comparators";
    }

    @Override
    protected String targetMethodName() {
        return "registerComparator";
    }

    @Override
    protected Class<?> adviceClass() {
        return RegisterComparatorAdvice.class;
    }
}
