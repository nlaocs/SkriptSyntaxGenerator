package jp.nlaocs.skriptSyntaxGenerator.bytecode

import net.bytebuddy.jar.asm.ClassReader
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.jar.asm.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * Optional evidence of a direct isSingle() dependency on an init Expression[] input.
 * No Skript code is invoked. Unknown instructions, loops, escaping expression inputs,
 * and non-delegating isSingle implementations leave the source unknown.
 */
object ExpressionCardinalitySourceAnalyzer {
    private const val INIT = "([Lch/njol/skript/lang/Expression;ILch/njol/util/Kleenean;Lch/njol/skript/lang/SkriptParser\$ParseResult;)Z"
    private const val MAX_STATES = 256
    private const val MAX_DEPTH = 12
    private val classes = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    data class Source(
        val expressionIndex: Int,
        val requiresExplicitInput: Boolean = true,
        val requiresParsedInput: Boolean = true,
        @get:com.fasterxml.jackson.annotation.JsonProperty("isSingleMethodDeclaringClass")
        val isSingleMethodDeclaringClass: String,
        val initMethodDeclaringClass: String
    )

    fun analyze(type: Class<*>): Source? = runCatching {
        val single = find(type, "isSingle", "()Z") ?: return null
        val code = single.code.filter { it.op >= 0 }
        if (code.size != 4 || code[0] != Insn(Opcodes.ALOAD, number = 0) ||
            code[1].op != Opcodes.GETFIELD || code[2].op !in setOf(Opcodes.INVOKEINTERFACE, Opcodes.INVOKEVIRTUAL) ||
            code[2].name != "isSingle" || code[2].desc != "()Z" || code[3].op != Opcodes.IRETURN
        ) return null
        val expression = Class.forName("ch.njol.skript.lang.Expression", false, type.classLoader)
        val delegateOwner = Class.forName(code[2].owner.replace('/', '.'), false, type.classLoader)
        val delegate = find(delegateOwner, "isSingle", "()Z") ?: return null
        if (!expression.isAssignableFrom(delegateOwner) || delegate.access and Opcodes.ACC_PUBLIC == 0 ||
            delegate.access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC) != 0) return null
        val field = canonicalField(type, code[1])
        val init = find(type, "init", INIT) ?: return null
        // A finite positive analysis; higher input indices and unsupported shapes remain unknown.
        for (index in 0 until 16) {
            val outcomes = execute(type, init, listOf(Value.ArrayInput, Value.Unknown, Value.Unknown, Value.ParseMetadata),
                Value.Unknown, field, index, 0) ?: return null
            val accepted = outcomes.filter { it.value != Value.Integer(0) }
            if (accepted.isNotEmpty() && accepted.all { it.source == Value.Capture(index) }) {
                return Source(index, isSingleMethodDeclaringClass = single.owner.replace('/', '.'),
                    initMethodDeclaringClass = init.owner.replace('/', '.'))
            }
        }
        null
    }.getOrNull()

    private data class Field(val owner: String, val name: String, val desc: String)

    private fun canonicalField(root: Class<*>, instruction: Insn): Field {
        var owner: Class<*>? = Class.forName(instruction.owner.replace('/', '.'), false, root.classLoader)
        while (owner != null) {
            val current = owner
            val declared = runCatching { current.getDeclaredField(instruction.name) }.getOrNull()
            if (declared != null) {
                require(Type.getDescriptor(declared.type) == instruction.desc)
                return Field(declared.declaringClass.name.replace('.', '/'), instruction.name, instruction.desc)
            }
            owner = current.superclass
        }
        error("unavailable field declaration")
    }
    private data class Insn(val op: Int, val number: Int = 0, val owner: String = "",
        val name: String = "", val desc: String = "", val label: Label? = null)
    private data class Method(val owner: String, val declaringClass: Class<*>, val desc: String,
                              val access: Int, val code: List<Insn>, val unsafe: Boolean)
    private sealed interface Value {
        data object This : Value
        data object ArrayInput : Value
        data object Unknown : Value
        data object ParseMetadata : Value
        data object Null : Value
        data class Capture(val index: Int) : Value
        data class Integer(val value: Int) : Value
    }
    private data class Frame(var pc: Int, val stack: MutableList<Value>, val locals: MutableMap<Int, Value>,
        var source: Value, val visited: MutableSet<Int>) {
        fun copyAt(next: Int) = Frame(next, stack.toMutableList(), locals.toMutableMap(), source, visited.toMutableSet())
    }
    private data class Outcome(val value: Value, val source: Value)

    private fun execute(root: Class<*>, method: Method, args: List<Value>, initial: Value,
                        field: Field, assumed: Int, depth: Int): List<Outcome>? {
        if (depth >= MAX_DEPTH || method.unsafe || method.code.isEmpty()) return null
        val labels = method.code.withIndex().filter { it.value.op == -1 }.associate { it.value.label to it.index }
        val locals = mutableMapOf<Int, Value>(0 to Value.This)
        var slot = 1
        Type.getArgumentTypes(method.desc).forEachIndexed { index, type ->
            locals[slot] = args.getOrElse(index) { Value.Unknown }; slot += type.size
        }
        val pending = ArrayDeque<Frame>()
        pending.add(Frame(0, mutableListOf(), locals, initial, mutableSetOf()))
        val outcomes = mutableListOf<Outcome>()
        var states = 0
        while (pending.isNotEmpty()) {
            if (++states > MAX_STATES) return null
            val frame = pending.removeFirst()
            while (frame.pc < method.code.size) {
                if (!frame.visited.add(frame.pc)) return null // loops are not a proven dependency
                val i = method.code[frame.pc++]
                fun pop(): Value = frame.stack.removeLastOrNull() ?: error("invalid stack")
                fun push(v: Value) { frame.stack.add(v) }
                fun jump(take: Boolean?) {
                    val target = labels[i.label] ?: error("unknown label")
                    if (target < frame.pc - 1) error("loop")
                    if (take == true) frame.pc = target
                    else if (take == null) pending.add(frame.copyAt(target))
                }
                when (i.op) {
                    -1, Opcodes.NOP -> {}
                    Opcodes.ACONST_NULL -> push(Value.Null)
                    in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> push(Value.Integer(i.op - Opcodes.ICONST_0))
                    Opcodes.BIPUSH, Opcodes.SIPUSH -> push(Value.Integer(i.number))
                    Opcodes.LDC, Opcodes.NEW, Opcodes.GETSTATIC -> push(Value.Unknown)
                    Opcodes.ALOAD, Opcodes.ILOAD -> push(frame.locals[i.number] ?: Value.Unknown)
                    Opcodes.ASTORE, Opcodes.ISTORE -> frame.locals[i.number] = pop()
                    Opcodes.AALOAD -> {
                        val index = pop(); val array = pop()
                        push(if (array == Value.ArrayInput && index is Value.Integer && index.value >= 0)
                            Value.Capture(index.value) else Value.Unknown)
                    }
                    Opcodes.CHECKCAST -> {}
                    Opcodes.DUP -> push(frame.stack.last())
                    Opcodes.POP -> pop()
                    Opcodes.GETFIELD -> {
                        val receiver = pop()
                        push(if (receiver == Value.This && canonicalField(root, i) == field)
                            frame.source else Value.Unknown)
                    }
                    Opcodes.PUTFIELD -> {
                        val value = pop(); val receiver = pop()
                        // Constructors/heap aliases are not modeled. An untracked receiver
                        // may be this object, even when the stored value is also unknown.
                        if (receiver != Value.This) return null
                        if (receiver == Value.This && canonicalField(root, i) == field) frame.source = value
                        else if (value == Value.This || value is Value.Capture || value == Value.ArrayInput) return null
                        // Other fields are not tracked: storing an input/self alias there could
                        // hide a later mutation behind a GETFIELD that becomes Unknown.
                    }
                    Opcodes.PUTSTATIC, Opcodes.AASTORE -> return null // possible input/self escape
                    Opcodes.IFNULL, Opcodes.IFNONNULL -> {
                        val value = pop()
                        val isNull = when (value) {
                            Value.Null -> true
                            Value.This, Value.ArrayInput -> false
                            Value.Capture(assumed) -> false
                            else -> null
                        }
                        jump(isNull?.let { if (i.op == Opcodes.IFNULL) it else !it })
                    }
                    Opcodes.IFEQ, Opcodes.IFNE -> {
                        val value = pop()
                        jump((value as? Value.Integer)?.value?.let { if (i.op == Opcodes.IFEQ) it == 0 else it != 0 })
                    }
                    Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE -> { pop(); jump(null) }
                    in Opcodes.IF_ICMPEQ..Opcodes.IF_ACMPNE -> { pop(); pop(); jump(null) }
                    Opcodes.GOTO -> jump(true)
                    Opcodes.INSTANCEOF -> { pop(); push(Value.Unknown) }
                    Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> {
                        val argTypes = Type.getArgumentTypes(i.desc)
                        val callArgs = argTypes.indices.map { pop() }.reversed()
                        val receiver = if (i.op == Opcodes.INVOKESTATIC) Value.Unknown else pop()
                        val resultType = Type.getReturnType(i.desc)
                        val identityGuard = i.op == Opcodes.INVOKESTATIC && (
                            (i.owner == "com/google/common/base/Preconditions" && i.name == "checkNotNull") ||
                            (i.owner == "java/util/Objects" && i.name == "requireNonNull") ||
                            (i.owner == "kotlin/jvm/internal/Intrinsics" && i.name in setOf("checkNotNull", "checkNotNullParameter")))
                        if (identityGuard && callArgs.isNotEmpty()) {
                            // These guards preserve object identity on every normally returning path.
                            if (callArgs.first() == Value.Null) break
                            if (resultType.sort != Type.VOID) push(callArgs.first())
                        } else if (i.op == Opcodes.INVOKESTATIC &&
                            i.owner == "ch/njol/skript/util/LiteralUtils" && i.name == "hasUnparsedLiteral" &&
                            callArgs == listOf(Value.Capture(assumed))) {
                            // Conditional fact: the caller must verify a fully parsed explicit input.
                            push(Value.Integer(0))
                        } else if (receiver == Value.ParseMetadata && i.op == Opcodes.INVOKEVIRTUAL &&
                            i.owner == "ch/njol/skript/lang/SkriptParser\$ParseResult" &&
                            i.name == "hasTag" && i.desc == "(Ljava/lang/String;)Z") {
                            // Skript's init ParseResult tag query; unrelated references/helpers
                            // are never treated as this trusted metadata input.
                            push(Value.Unknown)
                        } else if (receiver == Value.This) {
                            val called = resolveThisCall(root, i) ?: return null
                            val returned = execute(root, called, callArgs, frame.source, field, assumed, depth + 1)
                                ?: return null
                            if (returned.isEmpty()) break
                            for (extra in returned.drop(1)) {
                                val fork = frame.copyAt(frame.pc); fork.source = extra.source
                                if (resultType.sort != Type.VOID) fork.stack.add(extra.value)
                                pending.add(fork)
                            }
                            frame.source = returned.first().source
                            if (resultType.sort != Type.VOID) push(returned.first().value)
                        } else {
                            // Unknown references may be constructor-created aliases to this.
                            // Even an external call with no obvious input can mutate it through
                            // an alias/global; only the explicit contracts above are accepted.
                            return null
                        }
                    }
                    Opcodes.IRETURN, Opcodes.ARETURN -> { outcomes.add(Outcome(pop(), frame.source)); break }
                    Opcodes.RETURN -> { outcomes.add(Outcome(Value.Unknown, frame.source)); break }
                    Opcodes.ATHROW -> break // exceptional paths do not initialize an accepted Expression
                    else -> return null
                }
            }
        }
        return outcomes
    }

    private fun resolveThisCall(root: Class<*>, instruction: Insn): Method? {
        val owner = Class.forName(instruction.owner.replace('/', '.'), false, root.classLoader)
        if (!owner.isAssignableFrom(root) || owner.isInterface || instruction.op == Opcodes.INVOKEINTERFACE)
            return null // unsupported default-interface dispatch stays unknown
        val resolved = find(owner, instruction.name, instruction.desc) ?: return null
        if (resolved.access and Opcodes.ACC_STATIC != 0) return null
        if (instruction.op == Opcodes.INVOKESPECIAL ||
            resolved.access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL) != 0) return resolved

        var type: Class<*>? = root
        while (type != null) {
            val candidate = read(type)[instruction.name + instruction.desc]
            if (candidate != null && candidate.access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC) == 0) {
                if (candidate.declaringClass == resolved.declaringClass) return resolved
                val publicOverride = resolved.access and Opcodes.ACC_PUBLIC != 0 && candidate.access and Opcodes.ACC_PUBLIC != 0
                val protectedOverride = resolved.access and Opcodes.ACC_PROTECTED != 0 &&
                    candidate.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) != 0
                val packageOverride = resolved.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED) == 0 &&
                    candidate.declaringClass.packageName == resolved.declaringClass.packageName &&
                    candidate.declaringClass.classLoader == resolved.declaringClass.classLoader
                return if (publicOverride || protectedOverride || packageOverride) candidate else null
            }
            type = type.superclass
        }
        return null
    }

    private fun find(type: Class<*>, name: String, desc: String, seen: MutableSet<Class<*>> = mutableSetOf()): Method? {
        if (!seen.add(type)) return null
        read(type)[name + desc]?.let { return it }
        type.superclass?.let { find(it, name, desc, seen)?.let { method -> return method } }
        for (parent in type.interfaces) find(parent, name, desc, seen)?.let { return it }
        return null
    }

    private fun read(type: Class<*>): Map<String, Method> = classes.computeIfAbsent(type) {
        val name = type.name.replace('.', '/') + ".class"
        val input = type.classLoader?.getResourceAsStream(name) ?: ClassLoader.getSystemResourceAsStream(name)
            ?: return@computeIfAbsent emptyMap()
        val methods = mutableMapOf<String, Method>()
        input.use { stream ->
            ClassReader(stream).accept(object : ClassVisitor(Opcodes.ASM9) {
                private var owner = ""
                override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) { owner = name }
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
                    val code = mutableListOf<Insn>()
                    var unsafe = access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_NATIVE or Opcodes.ACC_STATIC) != 0
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitInsn(opcode: Int) { code.add(Insn(opcode)) }
                        override fun visitVarInsn(opcode: Int, variable: Int) { code.add(Insn(opcode, number = variable)) }
                        override fun visitIntInsn(opcode: Int, operand: Int) { code.add(Insn(opcode, number = operand)) }
                        override fun visitTypeInsn(opcode: Int, type: String) { code.add(Insn(opcode, owner = type)) }
                        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) { code.add(Insn(opcode, owner = owner, name = name, desc = descriptor)) }
                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) { code.add(Insn(opcode, owner = owner, name = name, desc = descriptor)) }
                        override fun visitJumpInsn(opcode: Int, label: Label) { code.add(Insn(opcode, label = label)) }
                        override fun visitLabel(label: Label) { code.add(Insn(-1, label = label)) }
                        override fun visitLdcInsn(value: Any) { code.add(if (value is Int) Insn(Opcodes.BIPUSH, number = value) else Insn(Opcodes.LDC)) }
                        override fun visitIincInsn(variable: Int, increment: Int) { unsafe = true }
                        override fun visitInvokeDynamicInsn(name: String, descriptor: String, handle: net.bytebuddy.jar.asm.Handle, vararg args: Any) { unsafe = true }
                        override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) { unsafe = true }
                        override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<out Label>) { unsafe = true }
                        override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) { unsafe = true }
                        override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) { unsafe = true }
                        override fun visitEnd() { methods[name + desc] = Method(owner, type, desc, access, code, unsafe) }
                    }
                }
            }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
        methods
    }
}
