package jp.nlaocs.skriptSyntaxGenerator.bytecode;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public final class CardinalitySourceFixtures {
    public static class Direct extends SimpleExpression<Number> {
        protected Expression<?> source;
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[1];
            return true;
        }
        @Override public boolean isSingle() { return source.isSingle(); }
        @Override protected Number[] get(Event event) { throw new UnsupportedOperationException(); }
        @Override public Class<? extends Number> getReturnType() { return Number.class; }
        @Override public String toString(Event event, boolean debug) { return "fixture"; }
    }

    public static class Forwarded extends Direct {
        private boolean flag;
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            flag = pattern == 3;
            return super.init(inputs, pattern, delayed, result);
        }
    }

    public static class GuardedSetter extends Direct {
        private void setInput(Expression<?> input) {
            com.google.common.base.Preconditions.checkNotNull(input, "input");
            source = input;
        }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            setInput(inputs[0]);
            return true;
        }
    }

    public static class NullFallback extends Direct {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            if (source == null) source = new OneToMany();
            return true;
        }
    }

    public static class Ambiguous extends Direct {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = pattern == 0 ? inputs[0] : inputs[1];
            return true;
        }
    }

    public static class OneToMany extends Direct {
        @Override public boolean isSingle() { return false; }
    }

    public static class Inverted extends Direct {
        @Override public boolean isSingle() { return !source.isSingle(); }
    }

    public static class Escaping extends Direct {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            mutate(this);
            return true;
        }
        private static void mutate(Direct value) { value.source = new OneToMany(); }
    }

    public static class Looping extends Direct {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            while (pattern-- > 0) source = inputs[0];
            return true;
        }
    }
}

