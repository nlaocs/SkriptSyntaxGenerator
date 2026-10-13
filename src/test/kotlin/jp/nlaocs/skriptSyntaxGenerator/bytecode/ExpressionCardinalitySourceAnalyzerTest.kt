package jp.nlaocs.skriptSyntaxGenerator.bytecode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import ch.njol.skript.lang.Expression
import ch.njol.util.Kleenean
import org.junit.jupiter.api.Test
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory

class ExpressionCardinalitySourceAnalyzerTest {
    @Test
    fun `static field reads cannot hide a class initializer mutation`() {
        val instance = CardinalitySourceFixtures.InitGlobalRead()
        assertEquals(0, CardinalitySourceFixtures.StaticCarrier.initialized)
        assertNull(ExpressionCardinalitySourceAnalyzer.analyze(instance.javaClass))
        assertEquals(0, CardinalitySourceFixtures.StaticCarrier.initialized)
        val input = CardinalitySourceFixtures.Single()
        assertTrue(instance.init(arrayOf<Expression<*>>(input), 0, Kleenean.FALSE, null))
        assertTrue(input.isSingle)
        assertFalse(instance.isSingle)
        assertEquals(1, CardinalitySourceFixtures.StaticCarrier.initialized)
    }

    @Test
    fun `ancestor special owner is unknown while inherited caller super forwarding stays proven`() {
        val changed = CardinalitySourceFixtures.ancestorSpecialFixture()
        val input = CardinalitySourceFixtures.Single()
        assertNull(ExpressionCardinalitySourceAnalyzer.analyze(changed.javaClass))
        assertTrue(changed.init(arrayOf<Expression<*>>(input), 0, Kleenean.FALSE, null))
        assertTrue(input.isSingle)
        assertFalse(changed.isSingle)
        val inherited = CardinalitySourceFixtures.InheritedForwarded()
        assertEquals(1, ExpressionCardinalitySourceAnalyzer.analyze(inherited.javaClass)?.expressionIndex)
        assertTrue(inherited.init(arrayOf<Expression<*>>(CardinalitySourceFixtures.OneToMany(), input), 0, Kleenean.FALSE, null))
        assertTrue(inherited.isSingle)
    }

    @Test
    fun `constructor aliases cannot prove a source after writes or external escape`() {
        val input = CardinalitySourceFixtures.Single()
        for (instance in listOf(CardinalitySourceFixtures.ConstructorAliasWrite(), CardinalitySourceFixtures.ConstructorAliasEscape())) {
            assertTrue(instance.init(arrayOf<Expression<*>>(input), 0, Kleenean.FALSE, null))
            assertTrue(input.isSingle)
            assertFalse(instance.isSingle)
            assertNull(ExpressionCardinalitySourceAnalyzer.analyze(instance.javaClass))
        }
    }

    @Test
    fun `private helper dispatch uses the symbolic owner rather than a child shadow`() {
        val input = CardinalitySourceFixtures.Single()
        val changed = CardinalitySourceFixtures.PrivateShadow()
        assertTrue(changed.init(arrayOf<Expression<*>>(input), 0, Kleenean.FALSE, null))
        assertFalse(changed.isSingle)
        assertNull(ExpressionCardinalitySourceAnalyzer.analyze(changed.javaClass))
        val preserved = CardinalitySourceFixtures.PrivateInputShadow()
        assertTrue(preserved.init(arrayOf<Expression<*>>(input), 0, Kleenean.FALSE, null))
        assertTrue(preserved.isSingle)
        assertEquals(0, ExpressionCardinalitySourceAnalyzer.analyze(preserved.javaClass)?.expressionIndex)
    }

    @Test
    fun `ordinary virtual overrides and explicit super calls retain their dispatch`() {
        for (instance in listOf(CardinalitySourceFixtures.VirtualInput(), CardinalitySourceFixtures.SuperForwardedVirtualInput())) {
            val inputs = arrayOf<Expression<*>>(CardinalitySourceFixtures.OneToMany(), CardinalitySourceFixtures.Single())
            assertTrue(instance.init(inputs, 0, Kleenean.FALSE, null))
            assertTrue(instance.isSingle)
            assertEquals(1, ExpressionCardinalitySourceAnalyzer.analyze(instance.javaClass)?.expressionIndex)
        }
        val explicit = CardinalitySourceFixtures.ExplicitSuperHelper()
        assertTrue(explicit.init(arrayOf<Expression<*>>(CardinalitySourceFixtures.Single(), CardinalitySourceFixtures.Single()), 0, Kleenean.FALSE, null))
        assertFalse(explicit.isSingle)
        assertNull(ExpressionCardinalitySourceAnalyzer.analyze(explicit.javaClass))
    }

    @Test
    fun `does not lose an input escape through an untracked field alias`() {
        assertNull(ExpressionCardinalitySourceAnalyzer.analyze(CardinalitySourceFixtures.AliasedEscape::class.java))
    }

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
