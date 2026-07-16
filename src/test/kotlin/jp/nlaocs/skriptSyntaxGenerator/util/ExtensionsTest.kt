package jp.nlaocs.skriptSyntaxGenerator.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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
    fun `enum values become lowercase usage strings`() {
        assertEquals(listOf("first value", "second"), ExampleValue::class.java.toStringListSafe())
        assertEquals(emptyList<String>(), String::class.java.toStringListSafe())
    }

    private enum class ExampleValue {
        FIRST_VALUE,
        SECOND
    }
}
