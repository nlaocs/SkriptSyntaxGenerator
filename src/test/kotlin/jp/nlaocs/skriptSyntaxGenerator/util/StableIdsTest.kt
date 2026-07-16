package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StableIdsTest {
    private val addon = AddonInfo("Dummy Addon", "1.0")

    @Test
    fun `digest uses sha-256`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            StableIds.digest("abc")
        )
    }

    @Test
    fun `record ids are normalized deterministic and length delimited`() {
        val first = StableIds.record("Event Value", addon, "ab", "c")
        val repeated = StableIds.record("Event Value", addon, "ab", "c")
        val differentBoundary = StableIds.record("Event Value", addon, "a", "bc")

        assertTrue(first.startsWith("event-value:dummy-addon:"))
        assertEquals(first, repeated)
        assertNotEquals(first, differentBoundary)
    }

    @Test
    fun `addon version does not change stable ids`() {
        val oldVersion = StableIds.record("type", AddonInfo("Skript", "2.14"), "number")
        val newVersion = StableIds.record("type", AddonInfo("Skript", "2.15"), "number")

        assertEquals(oldVersion, newVersion)
    }

    @Test
    fun `registration ids distinguish patterns and occurrences`() {
        val definition = StableIds.definition("expression", addon, String::class.java)
        val first = StableIds.registration(definition, listOf("%string%"), 0)

        assertNotEquals(first, StableIds.registration(definition, listOf("%strings%"), 0))
        assertNotEquals(first, StableIds.registration(definition, listOf("%string%"), 1))
    }

    @Test
    fun `stable class names preserve readable array shapes`() {
        assertEquals("java.lang.String", String::class.java.stableName())
        assertEquals("java.lang.String[]", Array<String>::class.java.stableName())
        assertEquals("java.lang.String[][]", Array<Array<String>>::class.java.stableName())
        assertEquals("int[]", IntArray::class.java.stableName())
    }
}
