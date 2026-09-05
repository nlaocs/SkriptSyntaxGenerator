package jp.nlaocs.skriptSyntaxGenerator.util

import ch.njol.skript.classes.Parser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.function.Supplier

class ExtensionsTest {
    @Test
    fun `empty collections become null without changing populated collections`() {
        assertNull((null as Collection<String>?).nullIfEmpty())
        assertNull(emptyList<String>().nullIfEmpty())
        assertEquals(listOf("value"), listOf("value").nullIfEmpty())

        assertNull((null as Array<String>?).toListOrNullIfEmpty())
        assertNull(emptyArray<String>().toListOrNullIfEmpty())
        assertEquals(listOf("value"), arrayOf("value").toListOrNullIfEmpty())
    }

    @Test
    fun `cleaning trims strings and drops blank values`() {
        assertNull((null as List<String?>?).cleaning())
        assertNull(listOf<String?>(null, " ").cleaning())
        assertEquals(listOf("first", "second"), listOf(" first ", null, "", "second").cleaning())
    }

    @Test
    fun `enum values are kept separate from usage`() {
        assertEquals(listOf("first value", "second"), ExampleValue::class.java.enumValues())
        assertEquals(emptyList<String>(), String::class.java.enumValues())
    }

    @Test
    fun `parser patterns and supplied literal values are collected independently`() {
        val parser = ExampleParser()
        val supplier = Supplier { listOf("first", "second", "first").iterator() }

        assertEquals(listOf("first", "second"), parserPatterns(parser))
        assertEquals(listOf("FIRST", "SECOND"), literalValues(parser, supplier))
        val literals = typeLiterals(parser, supplier).orEmpty()
        assertEquals(listOf("FIRST", "SECOND"), literals.map { it.text })
        assertEquals(listOf("FIRSTS", "SECONDS"), literals.map { it.pluralText })
        assertEquals(listOf("first", "second"), literals.map { it.variableName })
        assertEquals(listOf(String::class.java, String::class.java), literals.map { it.valueClass })
        assertEquals(listOf(null, null), literals.map { it.enumConstant })

        val represented = typeLiterals(
            RepresentedParser(),
            Supplier { listOf(RepresentedValue()).iterator() }
        ).orEmpty().single()
        assertEquals(Number::class.java, represented.representedClass)
    }

    @Test
    fun `registered type parser patterns retain runtime ownership`() {
        val registrations = registeredParserPatterns(ExampleRegistry::class.java).orEmpty()

        assertEquals(listOf("creeper", " powered creeper "), registrations.map { it.pattern })
        assertEquals(listOf(0, 0), registrations.map { it.registrationIndex })
        assertEquals(listOf(0, 1), registrations.map { it.patternIndex })
        assertEquals(listOf("normal", "powered"), registrations.map { it.sourceCodeName })
        assertEquals(listOf(ExampleValue::class.java, ExampleValue::class.java), registrations.map { it.dataClass })
        assertEquals(listOf(Number::class.java, Number::class.java), registrations.map { it.representedClass })
    }

    @Test
    fun `registered type parser patterns fail closed when a registration is incomplete`() {
        assertNull(registeredParserPatterns(IncompleteRegistry::class.java))
    }

    private enum class ExampleValue {
        FIRST_VALUE,
        SECOND
    }

    private class ExampleParser : Parser<String>() {
        fun getPatterns(): Array<String> = arrayOf("first", "second")

        override fun toString(value: String, flags: Int): String =
            value.uppercase() + if (flags and 1 != 0) "S" else ""

        override fun toVariableNameString(value: String): String = value

        override fun getDebugMessage(value: String): String = "debug:$value"
    }

    private class RepresentedValue {
        fun getType(): Class<*> = Number::class.java
    }

    private class RepresentedParser : Parser<RepresentedValue>() {
        override fun toString(value: RepresentedValue, flags: Int): String = "represented"

        override fun toVariableNameString(value: RepresentedValue): String = "represented"
    }

    private class ExampleRegistry {
        private companion object {
            @JvmField
            val infos = listOf(ExampleRegistration())
        }
    }

    private class ExampleRegistration {
        @Suppress("unused")
        private val codeName = "fallback"

        @Suppress("unused")
        private val entityClass = Number::class.java

        @Suppress("unused")
        fun getPatterns(): Array<String> = arrayOf("creeper", " powered creeper ")

        @Suppress("unused")
        fun getCodeNameFromPattern(index: Int): String = if (index == 0) "normal" else "powered"

        @Suppress("unused")
        fun getElementClass(): Class<*> = ExampleValue::class.java
    }

    private class IncompleteRegistry {
        private companion object {
            @JvmField
            val infos = listOf(ExampleRegistration(), IncompleteRegistration())
        }
    }

    private class IncompleteRegistration {
        @Suppress("unused")
        fun getPatterns(): Array<String> = arrayOf("missing entity class")

        @Suppress("unused")
        fun getElementClass(): Class<*> = ExampleValue::class.java
    }
}
