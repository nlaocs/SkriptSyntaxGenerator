package jp.nlaocs.skriptSyntaxGenerator.bytecode

import net.bytebuddy.jar.asm.ClassReader
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.Handle
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.jar.asm.Type
import java.util.concurrent.ConcurrentHashMap

object ExpressionBytecodeAnalyzer {
    private const val ACCEPT_CHANGE_DESCRIPTOR =
        "(Lch/njol/skript/classes/Changer\$ChangeMode;)[Ljava/lang/Class;"
    private const val GET_RETURN_TYPE_DESCRIPTOR = "()Ljava/lang/Class;"
    private const val POSSIBLE_RETURN_TYPES_DESCRIPTOR = "()[Ljava/lang/Class;"
    private const val IS_SINGLE_DESCRIPTOR = "()Z"

    private const val SIMPLE_EXPRESSION = "ch/njol/skript/lang/util/SimpleExpression"
    private const val CHANGE_MODE = "ch/njol/skript/classes/Changer\$ChangeMode"
    private const val SKRIPT = "ch/njol/skript/Skript"
    private const val COLLECTION_UTILS = "ch/njol/util/coll/CollectionUtils"
    private const val KOTLIN_INTRINSICS = "kotlin/jvm/internal/Intrinsics"

    private val classCache = ConcurrentHashMap<Class<*>, BytecodeClass>()

    fun acceptChangeStrategy(expressionClass: Class<*>): AcceptChangeStrategy {
        val method = findMethod(expressionClass, "acceptChange", ACCEPT_CHANGE_DESCRIPTOR)
            ?: return AcceptChangeStrategy.UNRESOLVED

        if (method.ownerInternalName == SIMPLE_EXPRESSION) {
            return if (hasSafeGetReturnType(expressionClass)) {
                AcceptChangeStrategy.REGISTERED_RETURN_TYPE
            } else {
                AcceptChangeStrategy.UNRESOLVED
            }
        }

        return if (method.isSafeToCall()) {
            AcceptChangeStrategy.INSTANCE_CALL
        } else {
            AcceptChangeStrategy.UNRESOLVED
        }
    }

    fun isSingleAnalysis(expressionClass: Class<*>): IsSingleAnalysis =
        findMethod(expressionClass, "isSingle", IS_SINGLE_DESCRIPTOR)
            ?.isSingleAnalysis()
            ?: IsSingleAnalysis.UNRESOLVED

    fun returnTypeAnalysis(expressionClass: Class<*>): ReturnTypeAnalysis {
        val returnTypeMethod = findMethod(
            expressionClass,
            "getReturnType",
            GET_RETURN_TYPE_DESCRIPTOR
        ) ?: return ReturnTypeAnalysis(
            state = ReturnTypeState.UNRESOLVED,
            possibleReturnTypes = emptyList(),
            possibleReturnTypesState = PossibleReturnTypesState.UNRESOLVED
        )
        val possibleReturnTypesMethod = findMethod(
            expressionClass,
            "possibleReturnTypes",
            POSSIBLE_RETURN_TYPES_DESCRIPTOR
        )
        val state = returnTypeMethod.returnTypeState()
        val descriptors = linkedSetOf<String>().apply {
            addAll(returnTypeMethod.classLiteralDescriptors)
            possibleReturnTypesMethod?.let { addAll(it.classLiteralDescriptors) }
        }
        val possibleReturnTypes = descriptors.mapNotNull { descriptor ->
            resolveClassLiteral(expressionClass, descriptor)
        }
        val possibleReturnTypesState = when {
            state == ReturnTypeState.STATIC -> PossibleReturnTypesState.COMPLETE
            possibleReturnTypesMethod?.hasCompleteClassLiteralSet() == true ->
                PossibleReturnTypesState.COMPLETE
            possibleReturnTypes.isNotEmpty() -> PossibleReturnTypesState.PARTIAL
            else -> PossibleReturnTypesState.UNRESOLVED
        }
        return ReturnTypeAnalysis(state, possibleReturnTypes, possibleReturnTypesState)
    }

    private fun resolveClassLiteral(expressionClass: Class<*>, descriptor: String): Class<*>? =
        runCatching {
            val type = Type.getType(descriptor)
            when (type.sort) {
                Type.OBJECT -> Class.forName(type.className, false, expressionClass.classLoader)
                Type.ARRAY -> Class.forName(descriptor.replace('/', '.'), false, expressionClass.classLoader)
                else -> null
            }
        }.getOrNull()

    private fun hasSafeGetReturnType(expressionClass: Class<*>): Boolean =
        findMethod(expressionClass, "getReturnType", GET_RETURN_TYPE_DESCRIPTOR)
            ?.isSafeToCall() == true

    private fun findMethod(
        type: Class<*>,
        methodName: String,
        descriptor: String,
        visited: MutableSet<String> = HashSet()
    ): BytecodeMethod? {
        val bytecode = readClass(type) ?: return null
        if (!visited.add(bytecode.internalName)) {
            return null
        }

        bytecode.methods[MethodKey(methodName, descriptor)]?.let { return it }

        type.superclass?.let { superclass ->
            findMethod(superclass, methodName, descriptor, visited)?.let { return it }
        }

        for (interfaceClass in type.interfaces) {
            findMethod(interfaceClass, methodName, descriptor, visited)?.let { return it }
        }

        return null
    }

    private fun readClass(type: Class<*>): BytecodeClass? {
        classCache[type]?.let { return it }

        val resourceName = type.name.replace('.', '/') + ".class"
        val stream = type.classLoader?.getResourceAsStream(resourceName)
            ?: ClassLoader.getSystemResourceAsStream(resourceName)
            ?: return null

        return stream.use {
            val visitor = BytecodeClassVisitor()
            ClassReader(it).accept(visitor, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            visitor.toBytecodeClass()
        }.also {
            classCache[type] = it
        }
    }

    enum class AcceptChangeStrategy {
        INSTANCE_CALL,
        REGISTERED_RETURN_TYPE,
        UNRESOLVED
    }

    enum class IsSingleAnalysis {
        SINGLE,
        MULTIPLE,
        BOTH,
        UNRESOLVED
    }

    data class ReturnTypeAnalysis(
        val state: ReturnTypeState,
        val possibleReturnTypes: List<Class<*>>,
        val possibleReturnTypesState: PossibleReturnTypesState
    )

    enum class ReturnTypeState {
        STATIC,
        DYNAMIC,
        UNRESOLVED
    }

    enum class PossibleReturnTypesState {
        COMPLETE,
        PARTIAL,
        UNRESOLVED
    }

    private data class MethodKey(
        val name: String,
        val descriptor: String
    )

    private data class BytecodeClass(
        val internalName: String,
        val methods: Map<MethodKey, BytecodeMethod>
    )

    private data class MethodCall(
        val opcode: Int,
        val owner: String,
        val name: String,
        val descriptor: String
    )

    private class BytecodeClassVisitor : ClassVisitor(Opcodes.ASM9) {
        private var internalName: String? = null
        private val methods = LinkedHashMap<MethodKey, BytecodeMethod>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            internalName = name
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val method = BytecodeMethod(requireNotNull(internalName), access, name, descriptor)
            methods[MethodKey(name, descriptor)] = method
            return method
        }

        fun toBytecodeClass(): BytecodeClass =
            BytecodeClass(requireNotNull(internalName), methods.toMap())
    }

    private class BytecodeMethod(
        val ownerInternalName: String,
        private val access: Int,
        @Suppress("unused")
        val name: String,
        @Suppress("unused")
        val descriptor: String
    ) : MethodVisitor(Opcodes.ASM9) {
        private val opcodes = mutableListOf<Int>()
        private val methodCalls = mutableListOf<MethodCall>()
        val classLiteralDescriptors = linkedSetOf<String>()
        private var unsafeInstructionFound = false
        private var instanceStateAccessed = false
        private var controlFlowFound = false

        fun isSingleAnalysis(): IsSingleAnalysis {
            constantBooleanReturn()?.let {
                return if (it) IsSingleAnalysis.SINGLE else IsSingleAnalysis.MULTIPLE
            }

            if (methodCalls.any { it.name == "isSingle" && it.descriptor == IS_SINGLE_DESCRIPTOR }) {
                return IsSingleAnalysis.BOTH
            }

            return IsSingleAnalysis.UNRESOLVED
        }

        private fun constantBooleanReturn(): Boolean? {
            if (opcodes.size != 2 || opcodes[1] != Opcodes.IRETURN) {
                return null
            }

            return when (opcodes[0]) {
                Opcodes.ICONST_0 -> false
                Opcodes.ICONST_1 -> true
                else -> null
            }
        }

        fun returnTypeState(): ReturnTypeState {
            if (constantClassReturn() != null) {
                return ReturnTypeState.STATIC
            }
            if (!hasCode()) {
                return ReturnTypeState.UNRESOLVED
            }
            return if (
                instanceStateAccessed ||
                controlFlowFound ||
                opcodes.count { it == Opcodes.ARETURN } > 1 ||
                methodCalls.any { it.name in dynamicReturnTypeMethodNames }
            ) {
                ReturnTypeState.DYNAMIC
            } else {
                ReturnTypeState.UNRESOLVED
            }
        }

        fun hasCompleteClassLiteralSet(): Boolean =
            hasCode() &&
                classLiteralDescriptors.isNotEmpty() &&
                !instanceStateAccessed &&
                !controlFlowFound &&
                methodCalls.all {
                    it.owner == COLLECTION_UTILS && it.name == "array"
                }

        private fun constantClassReturn(): String? =
            classLiteralDescriptors.singleOrNull()
                ?.takeIf { opcodes == listOf(Opcodes.LDC, Opcodes.ARETURN) }

        fun isSafeToCall(): Boolean = hasCode() && !unsafeInstructionFound

        override fun visitInsn(opcode: Int) {
            opcodes += opcode
            when (opcode) {
                Opcodes.ATHROW,
                Opcodes.MONITORENTER,
                Opcodes.MONITOREXIT -> unsafeInstructionFound = true
            }
        }

        override fun visitIntInsn(opcode: Int, operand: Int) {
            opcodes += opcode
        }

        override fun visitVarInsn(opcode: Int, variable: Int) {
            opcodes += opcode
        }

        override fun visitTypeInsn(opcode: Int, type: String) {
            opcodes += opcode
            if (opcode == Opcodes.NEW) {
                unsafeInstructionFound = true
            }
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            opcodes += opcode
            if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                instanceStateAccessed = true
                unsafeInstructionFound = true
            }
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            opcodes += opcode
            methodCalls += MethodCall(opcode, owner, name, descriptor)
            if (!isSafeMethodInvocation(opcode, owner, name)) {
                unsafeInstructionFound = true
            }
        }

        override fun visitInvokeDynamicInsn(
            name: String,
            descriptor: String,
            bootstrapMethodHandle: Handle,
            vararg bootstrapMethodArguments: Any?
        ) {
            opcodes += Opcodes.INVOKEDYNAMIC
            unsafeInstructionFound = true
        }

        override fun visitJumpInsn(opcode: Int, label: Label) {
            opcodes += opcode
            controlFlowFound = true
        }

        override fun visitLdcInsn(value: Any?) {
            opcodes += Opcodes.LDC
            if (value is Type && value.sort in setOf(Type.OBJECT, Type.ARRAY)) {
                classLiteralDescriptors += value.descriptor
            }
        }

        override fun visitIincInsn(variable: Int, increment: Int) {
            opcodes += Opcodes.IINC
        }

        override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
            opcodes += Opcodes.TABLESWITCH
            controlFlowFound = true
        }

        override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<out Label>) {
            opcodes += Opcodes.LOOKUPSWITCH
            controlFlowFound = true
        }

        override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) {
            opcodes += Opcodes.MULTIANEWARRAY
        }

        override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
            unsafeInstructionFound = true
        }

        private fun hasCode(): Boolean =
            (access and Opcodes.ACC_ABSTRACT) == 0 && (access and Opcodes.ACC_NATIVE) == 0

        private fun isSafeMethodInvocation(opcode: Int, owner: String, name: String): Boolean {
            if (owner == SKRIPT && name in setOf("error", "warning", "exception")) {
                return false
            }

            if (name in riskyExpressionMethodNames) {
                return false
            }

            if (owner.startsWith("ch/njol/skript/classes/Changer\$ChangerUtils")) {
                return false
            }

            if (owner.contains("ParserInstance") || owner.contains("SkriptParser") || owner.contains("EventValues")) {
                return false
            }

            if (owner == COLLECTION_UTILS && name == "array") {
                return true
            }

            if (owner == CHANGE_MODE && name == "ordinal") {
                return true
            }

            if (owner == "java/lang/Enum" && name == "ordinal") {
                return true
            }

            if (owner == "java/lang/Class" && name in safeClassMethodNames) {
                return true
            }

            if (owner == KOTLIN_INTRINSICS && name.startsWith("check")) {
                return true
            }

            return opcode == Opcodes.INVOKESPECIAL && name == "<init>" && owner.startsWith("java/")
        }
    }

    private val riskyExpressionMethodNames = setOf(
        "acceptChange",
        "getAcceptedChangeModes",
        "getExpr",
        "getParser",
        "canReturn",
        "canReturnAnyOf",
        "possibleReturnTypes",
        "getReturnType",
        "isSingle",
        "canBeSingle",
        "getAnd",
        "isDefault",
        "getSingle",
        "getArray",
        "getAll",
        "stream",
        "streamAll"
    )

    private val safeClassMethodNames = setOf(
        "isAssignableFrom",
        "isArray",
        "getComponentType"
    )

    private val dynamicReturnTypeMethodNames = setOf(
        "getExpr",
        "getReturnType",
        "possibleReturnTypes",
        "canReturn",
        "canReturnAnyOf",
        "isSingle",
        "getSingle",
        "getArray",
        "getAll"
    )
}
