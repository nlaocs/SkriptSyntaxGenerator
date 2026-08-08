package jp.nlaocs.skriptSyntaxGenerator.bytecode

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer.ChangeMode
import ch.njol.skript.lang.util.SimpleExpression
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.AcceptChangeStrategy
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.IsSingleAnalysis
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.PossibleReturnTypesState
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.ReturnTypeState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpressionBytecodeAnalyzerTest {
    @Test
    fun `safe acceptChange implementation can be called on an instance`() {
        assertEquals(
            AcceptChangeStrategy.INSTANCE_CALL,
            ExpressionBytecodeAnalyzer.acceptChangeStrategy(SafeAcceptChangeFixture::class.java)
        )
    }

    @Test
    fun `stateful and error reporting acceptChange implementations remain unresolved`() {
        assertEquals(
            AcceptChangeStrategy.UNRESOLVED,
            ExpressionBytecodeAnalyzer.acceptChangeStrategy(StatefulAcceptChangeFixture::class.java)
        )
        assertEquals(
            AcceptChangeStrategy.UNRESOLVED,
            ExpressionBytecodeAnalyzer.acceptChangeStrategy(ErrorAcceptChangeFixture::class.java)
        )
    }

    @Test
    fun `SimpleExpression delegation uses registered return type only when getReturnType is safe`() {
        assertEquals(
            AcceptChangeStrategy.REGISTERED_RETURN_TYPE,
            ExpressionBytecodeAnalyzer.acceptChangeStrategy(ConstantReturnTypeFixture::class.java)
        )
        assertEquals(
            AcceptChangeStrategy.UNRESOLVED,
            ExpressionBytecodeAnalyzer.acceptChangeStrategy(StatefulReturnTypeFixture::class.java)
        )
    }

    @Test
    fun `isSingle analysis distinguishes constants delegation and dynamic state`() {
        assertEquals(IsSingleAnalysis.SINGLE, ExpressionBytecodeAnalyzer.isSingleAnalysis(SingleFixture::class.java))
        assertEquals(IsSingleAnalysis.MULTIPLE, ExpressionBytecodeAnalyzer.isSingleAnalysis(MultipleFixture::class.java))
        assertEquals(IsSingleAnalysis.BOTH, ExpressionBytecodeAnalyzer.isSingleAnalysis(DelegatingFixture::class.java))
        assertEquals(IsSingleAnalysis.UNRESOLVED, ExpressionBytecodeAnalyzer.isSingleAnalysis(StatefulSingleFixture::class.java))
    }

    @Test
    fun `return type analysis distinguishes static dynamic and known alternatives`() {
        val constant = ExpressionBytecodeAnalyzer.returnTypeAnalysis(ConstantReturnTypeFixture::class.java)
        assertEquals(ReturnTypeState.STATIC, constant.state)
        assertEquals(listOf(String::class.java), constant.possibleReturnTypes)
        assertEquals(PossibleReturnTypesState.COMPLETE, constant.possibleReturnTypesState)

        val stateful = ExpressionBytecodeAnalyzer.returnTypeAnalysis(StatefulReturnTypeFixture::class.java)
        assertEquals(ReturnTypeState.DYNAMIC, stateful.state)
        assertEquals(PossibleReturnTypesState.UNRESOLVED, stateful.possibleReturnTypesState)

        val alternatives = ExpressionBytecodeAnalyzer.returnTypeAnalysis(DynamicReturnTypeFixture::class.java)
        assertEquals(ReturnTypeState.DYNAMIC, alternatives.state)
        assertEquals(
            setOf(String::class.java, Long::class.javaObjectType),
            alternatives.possibleReturnTypes.toSet()
        )
        assertEquals(PossibleReturnTypesState.COMPLETE, alternatives.possibleReturnTypesState)

        val indirect = ExpressionBytecodeAnalyzer.returnTypeAnalysis(
            IndirectPossibleReturnTypeFixture::class.java
        )
        assertEquals(ReturnTypeState.DYNAMIC, indirect.state)
        assertEquals(listOf(String::class.java), indirect.possibleReturnTypes)
        assertEquals(PossibleReturnTypesState.PARTIAL, indirect.possibleReturnTypesState)
    }

    @Test
    fun `Skript context dependent return types remain dynamic`() {
        val sizeExpression = listOf(
            "org.skriptlang.skript.common.properties.elements.expressions.PropExprSize",
            "org.skriptlang.skript.common.properties.expressions.PropExprSize"
        ).firstNotNullOf { name ->
            runCatching { Class.forName(name, false, javaClass.classLoader) }.getOrNull()
        }
        val size = ExpressionBytecodeAnalyzer.returnTypeAnalysis(sizeExpression)
        assertEquals(ReturnTypeState.DYNAMIC, size.state)
        assertEquals(PossibleReturnTypesState.PARTIAL, size.possibleReturnTypesState)
        assertEquals(setOf(Long::class.javaObjectType), size.possibleReturnTypes.toSet())

        val parse = ExpressionBytecodeAnalyzer.returnTypeAnalysis(
            Class.forName("ch.njol.skript.expressions.ExprParse", false, javaClass.classLoader)
        )
        assertEquals(ReturnTypeState.DYNAMIC, parse.state)
        assertEquals(PossibleReturnTypesState.PARTIAL, parse.possibleReturnTypesState)
    }
}

private class SafeAcceptChangeFixture {
    fun acceptChange(mode: ChangeMode): Array<Class<*>>? =
        if (mode == ChangeMode.SET) arrayOf(String::class.java) else null
}

private class StatefulAcceptChangeFixture {
    private val enabled = true

    fun acceptChange(mode: ChangeMode): Array<Class<*>>? =
        if (enabled && mode == ChangeMode.SET) arrayOf(String::class.java) else null
}

private class ErrorAcceptChangeFixture {
    fun acceptChange(mode: ChangeMode): Array<Class<*>>? {
        Skript.error("fixture error: $mode")
        return null
    }
}

private abstract class ConstantReturnTypeFixture : SimpleExpression<String>() {
    override fun getReturnType() = String::class.java
}

private abstract class StatefulReturnTypeFixture : SimpleExpression<Any>() {
    private val dynamicReturnType: Class<out Any> = Any::class.java

    override fun getReturnType(): Class<out Any> = dynamicReturnType
}

private class DynamicReturnTypeFixture(private val text: Boolean) {
    fun getReturnType(): Class<*> =
        if (text) String::class.java else Long::class.javaObjectType

    fun possibleReturnTypes(): Array<Class<*>> =
        arrayOf(String::class.java, Long::class.javaObjectType)
}

private class IndirectPossibleReturnTypeFixture {
    private val returnType: Class<*> = Any::class.java

    fun getReturnType(): Class<*> = returnType

    fun possibleReturnTypes(): Array<Class<*>> = expandPossibleReturnTypes(String::class.java)
}

private fun expandPossibleReturnTypes(type: Class<*>): Array<Class<*>> =
    arrayOf(type, Long::class.javaObjectType)

private class SingleFixture {
    fun isSingle(): Boolean = true
}

private class MultipleFixture {
    fun isSingle(): Boolean = false
}

private fun interface MultiplicityDelegate {
    fun isSingle(): Boolean
}

private class DelegatingFixture(private val delegate: MultiplicityDelegate) {
    fun isSingle(): Boolean = delegate.isSingle()
}

private class StatefulSingleFixture(private val single: Boolean) {
    fun isSingle(): Boolean = single
}
