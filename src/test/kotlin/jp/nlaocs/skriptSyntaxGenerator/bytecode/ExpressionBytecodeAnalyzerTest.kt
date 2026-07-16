package jp.nlaocs.skriptSyntaxGenerator.bytecode

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer.ChangeMode
import ch.njol.skript.lang.util.SimpleExpression
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.AcceptChangeStrategy
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer.IsSingleAnalysis
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
