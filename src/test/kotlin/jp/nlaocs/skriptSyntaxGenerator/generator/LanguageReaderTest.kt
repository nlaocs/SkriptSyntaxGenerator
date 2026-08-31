package jp.nlaocs.skriptSyntaxGenerator.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LanguageReaderTest {
    @Test
    fun `effective values use default language precedence`() {
        val values = LanguageReader.effectiveValues(
            mapOf(
                "shared" to "default",
                "default-only" to "value",
            ),
            mapOf(
                "shared" to "localized",
                "localized-only" to "value",
            ),
        )

        assertEquals(
            mapOf(
                "default-only" to "value",
                "localized-only" to "value",
                "shared" to "default",
            ),
            values,
        )
    }

    @Test
    fun `effective values reject non-string registry entries`() {
        assertThrows<IllegalStateException> {
            LanguageReader.effectiveValues(mapOf(42 to "invalid"), emptyMap<String, String>())
        }
        assertThrows<IllegalStateException> {
            LanguageReader.effectiveValues(mapOf("invalid" to null), emptyMap<String, String>())
        }
    }

    @Test
    fun `missing language class fails instead of imitating an empty registry`() {
        val loader = object : ClassLoader(null) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> =
                throw ClassNotFoundException(name)
        }

        assertThrows<IllegalStateException> { LanguageReader.read(loader) }
    }

    @Test
    fun `reads the available runtime language class`() {
        val values = LanguageReader.read(ch.njol.skript.localization.Language::class.java.classLoader)

        assertEquals(values.keys.sorted(), values.keys.toList())
    }
}
