package jp.nlaocs.skriptSyntaxGenerator.bytecode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory

class ExpressionCardinalitySourceAnalyzerTest {
    @Test
    fun `records the input index without calling expression methods`() {
        val source = ExpressionCardinalitySourceAnalyzer.analyze(CardinalitySourceFixtures.Direct::class.java)!!
        assertEquals(1, source.expressionIndex)
        assertTrue(source.requiresExplicitInput)
        assertTrue(source.requiresParsedInput)
        assertEquals(CardinalitySourceFixtures.Direct::class.java.name, source.isSingleMethodDeclaringClass)
        val wire = JacksonFactory.create().valueToTree<com.fasterxml.jackson.databind.JsonNode>(source)
        assertEquals(1, wire["expressionIndex"].intValue())
        assertEquals(source.isSingleMethodDeclaringClass, wire["isSingleMethodDeclaringClass"].textValue())
    }

    @Test
    fun `follows unchanged super init inputs and skips a null-only fallback`() {
        assertEquals(1, ExpressionCardinalitySourceAnalyzer.analyze(CardinalitySourceFixtures.Forwarded::class.java)?.expressionIndex)
        assertEquals(0, ExpressionCardinalitySourceAnalyzer.analyze(CardinalitySourceFixtures.NullFallback::class.java)?.expressionIndex)
        assertEquals(0, ExpressionCardinalitySourceAnalyzer.analyze(CardinalitySourceFixtures.GuardedSetter::class.java)?.expressionIndex)
    }

    @Test
    fun `never assumes the first input for branches inversion one-to-many escape or loops`() {
        for (type in listOf(CardinalitySourceFixtures.Ambiguous::class.java,
            CardinalitySourceFixtures.Inverted::class.java, CardinalitySourceFixtures.OneToMany::class.java,
            CardinalitySourceFixtures.Escaping::class.java, CardinalitySourceFixtures.Looping::class.java)) {
            assertNull(ExpressionCardinalitySourceAnalyzer.analyze(type), type.name)
        }
    }
}

