package jp.nlaocs.skriptSyntaxGenerator.bytecode;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

public final class CardinalitySourceFixtures {
    public static class DynamicCarrier {
        public static Direct current;
        public static int bootstrapCalls;
    }
    public static class DynamicLiteralFixture extends Direct {
        public DynamicLiteralFixture() { DynamicCarrier.current = this; }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            Object ignored = "ordinary-trigger";
            int number = 1000000;
            Class<?> type = Single.class;
            return true;
        }
        @Override public boolean isSingle() { return source.isSingle(); }
    }
    public static Object constantBootstrap(java.lang.invoke.MethodHandles.Lookup lookup, String name, Class<?> type) {
        DynamicCarrier.bootstrapCalls++;
        DynamicCarrier.current.source = new OneToMany();
        return new Object();
    }
    public static java.lang.invoke.CallSite invocationBootstrap(java.lang.invoke.MethodHandles.Lookup lookup, String name, java.lang.invoke.MethodType type) {
        DynamicCarrier.bootstrapCalls++;
        DynamicCarrier.current.source = new OneToMany();
        return new java.lang.invoke.ConstantCallSite(java.lang.invoke.MethodHandles.empty(type));
    }
    public static Direct dynamicLiteralFixture(boolean getterInvocation) throws Exception {
        String name = DynamicLiteralFixture.class.getName();
        String resource = name.replace('.', '/') + ".class";
        byte[] original;
        try (java.io.InputStream input = CardinalitySourceFixtures.class.getClassLoader().getResourceAsStream(resource)) {
            original = input.readAllBytes();
        }
        net.bytebuddy.jar.asm.ClassWriter writer = new net.bytebuddy.jar.asm.ClassWriter(0);
        String owner = CardinalitySourceFixtures.class.getName().replace('.', '/');
        new net.bytebuddy.jar.asm.ClassReader(original).accept(new net.bytebuddy.jar.asm.ClassVisitor(net.bytebuddy.jar.asm.Opcodes.ASM9, writer) {
            @Override public net.bytebuddy.jar.asm.MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                return new net.bytebuddy.jar.asm.MethodVisitor(net.bytebuddy.jar.asm.Opcodes.ASM9, super.visitMethod(access, method, descriptor, signature, exceptions)) {
                    @Override public void visitCode() {
                        super.visitCode();
                        if (getterInvocation && method.equals("isSingle")) super.visitInvokeDynamicInsn("mutate", "()V", new net.bytebuddy.jar.asm.Handle(
                            net.bytebuddy.jar.asm.Opcodes.H_INVOKESTATIC, owner, "invocationBootstrap",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false));
                    }
                    @Override public void visitLdcInsn(Object value) {
                        if (!getterInvocation && method.equals("init") && "ordinary-trigger".equals(value)) value = new net.bytebuddy.jar.asm.ConstantDynamic(
                            "mutate", "Ljava/lang/Object;", new net.bytebuddy.jar.asm.Handle(net.bytebuddy.jar.asm.Opcodes.H_INVOKESTATIC, owner, "constantBootstrap",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false));
                        super.visitLdcInsn(value);
                    }
                };
            }
        }, 0);
        byte[] changed = writer.toByteArray();
        Class<?> type = new ClassLoader(CardinalitySourceFixtures.class.getClassLoader()) {
            Class<?> fixture() { return defineClass(name, changed, 0, changed.length); }
            @Override public java.io.InputStream getResourceAsStream(String path) {
                return path.equals(resource) ? new java.io.ByteArrayInputStream(changed) : super.getResourceAsStream(path);
            }
        }.fixture();
        return (Direct) type.getConstructor().newInstance();
    }

    public static class StaticCarrier {
        public static Direct current;
        public static int initialized;
    }
    public static class LazyMutator {
        public static final Object TRIGGER = mutate();
        private static Object mutate() {
            StaticCarrier.initialized++;
            StaticCarrier.current.source = new OneToMany();
            return new Object();
        }
    }
    public static class InitGlobalRead extends Direct {
        public InitGlobalRead() { StaticCarrier.current = this; }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            Object ignored = LazyMutator.TRIGGER;
            return true;
        }
    }
    public static class AncestorSpecialBase extends Direct {
        protected void setInput(Expression<?> input) { source = input; }
    }
    public static class AncestorSpecialMiddle extends AncestorSpecialBase {
        @Override protected void setInput(Expression<?> input) { source = new OneToMany(); }
    }
    public static class AncestorSpecialChild extends AncestorSpecialMiddle {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            super.setInput(inputs[0]);
            return true;
        }
    }
    public static class InheritedForwarded extends Forwarded {}

    public static Direct ancestorSpecialFixture() throws Exception {
        String name = AncestorSpecialChild.class.getName();
        String resource = name.replace('.', '/') + ".class";
        byte[] original;
        try (java.io.InputStream input = CardinalitySourceFixtures.class.getClassLoader().getResourceAsStream(resource)) {
            original = input.readAllBytes();
        }
        net.bytebuddy.jar.asm.ClassWriter writer = new net.bytebuddy.jar.asm.ClassWriter(0);
        new net.bytebuddy.jar.asm.ClassReader(original).accept(new net.bytebuddy.jar.asm.ClassVisitor(net.bytebuddy.jar.asm.Opcodes.ASM9, writer) {
            @Override public net.bytebuddy.jar.asm.MethodVisitor visitMethod(int access, String method, String descriptor, String signature, String[] exceptions) {
                return new net.bytebuddy.jar.asm.MethodVisitor(net.bytebuddy.jar.asm.Opcodes.ASM9, super.visitMethod(access, method, descriptor, signature, exceptions)) {
                    @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean isInterface) {
                        if (opcode == net.bytebuddy.jar.asm.Opcodes.INVOKESPECIAL && method.equals("setInput") && owner.endsWith("$AncestorSpecialMiddle"))
                            owner = owner.replace("$AncestorSpecialMiddle", "$AncestorSpecialBase");
                        super.visitMethodInsn(opcode, owner, method, descriptor, isInterface);
                    }
                };
            }
        }, 0);
        byte[] changed = writer.toByteArray();
        Class<?> type = new ClassLoader(CardinalitySourceFixtures.class.getClassLoader()) {
            Class<?> fixture() { return defineClass(name, changed, 0, changed.length); }
            @Override public java.io.InputStream getResourceAsStream(String path) {
                return path.equals(resource) ? new java.io.ByteArrayInputStream(changed) : super.getResourceAsStream(path);
            }
        }.fixture();
        return (Direct) type.getConstructor().newInstance();
    }

    public static class Single extends Direct {
        @Override public boolean isSingle() { return true; }
    }

    public static class ConstructorAliasWrite extends Direct {
        private static final Expression<?> MANY = new OneToMany();
        private final ConstructorAliasWrite alias = this;
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            alias.source = MANY;
            return true;
        }
    }

    public static class ConstructorAliasEscape extends Direct {
        private final Direct alias = this;
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            replace(alias);
            return true;
        }
        private static void replace(Direct value) { value.source = new OneToMany(); }
    }

    public static class PrivateBase extends Direct {
        private void setInput(Expression<?> value) { source = new OneToMany(); }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            setInput(inputs[0]);
            return true;
        }
    }
    public static class PrivateShadow extends PrivateBase {
        private void setInput(Expression<?> value) { source = value; }
    }
    public static class PrivateInputBase extends Direct {
        private void setInput(Expression<?> value) { source = value; }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            setInput(inputs[0]);
            return true;
        }
    }
    public static class PrivateInputShadow extends PrivateInputBase {
        private void setInput(Expression<?> value) { source = new OneToMany(); }
    }

    public static class VirtualBase extends Direct {
        protected void setInputs(Expression<?>[] inputs) { source = new OneToMany(); }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            setInputs(inputs);
            return true;
        }
    }
    public static class VirtualInput extends VirtualBase {
        @Override protected void setInputs(Expression<?>[] inputs) { source = inputs[1]; }
    }
    public static class SuperForwardedVirtualInput extends VirtualInput {
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            return super.init(inputs, pattern, delayed, result);
        }
    }
    public static class ExplicitSuperHelper extends VirtualBase {
        @Override protected void setInputs(Expression<?>[] inputs) { source = inputs[1]; }
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            super.setInputs(inputs);
            return true;
        }
    }

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

    public static class AliasedEscape extends Direct {
        private Expression<?> alias;
        @Override public boolean init(Expression<?>[] inputs, int pattern, Kleenean delayed, ParseResult result) {
            source = inputs[0];
            alias = source;
            mutate(alias);
            return true;
        }
        private static void mutate(Expression<?> value) {
            if (value instanceof Direct) ((Direct) value).source = new OneToMany();
        }
    }
}
