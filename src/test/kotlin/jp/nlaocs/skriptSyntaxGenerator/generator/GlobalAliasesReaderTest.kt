package jp.nlaocs.skriptSyntaxGenerator.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.IdentityHashMap

class GlobalAliasesReaderTest {
    @Test
    fun unserializableValuesUseDeterministicMarker() {
        val normalized = normalize(Any())

        assertEquals(
            mapOf("type" to "java.lang.Object", "state" to "unresolved"),
            normalized,
        )
        assertFalse(normalized.toString().contains(Regex("@[0-9a-fA-F]+")))
    }

    @Test
    fun cyclesUseDeterministicMarker() {
        val cyclic = linkedMapOf<String, Any>()
        cyclic["self"] = cyclic

        assertEquals(
            mapOf(
                "self" to mapOf(
                    "type" to "java.util.LinkedHashMap",
                    "state" to "cycle",
                ),
            ),
            normalize(cyclic),
        )
    }

    private fun normalize(value: Any): Any {
        val method = GlobalAliasesReader::class.java.getDeclaredMethod(
            "normalize",
            Any::class.java,
            IdentityHashMap::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, value, IdentityHashMap<Any, Boolean>())
    }
}