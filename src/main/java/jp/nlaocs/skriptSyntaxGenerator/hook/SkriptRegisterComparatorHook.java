package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class SkriptRegisterComparatorHook extends AbstractRetransformHook {

    public static final SkriptRegisterComparatorHook INSTANCE = new SkriptRegisterComparatorHook();

    private SkriptRegisterComparatorHook() {
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
        return SkriptRegisterComparatorAdvice.class;
    }
}
