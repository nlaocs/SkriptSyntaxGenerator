package jp.nlaocs.skriptSyntaxGenerator.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SnapshotFormatTest {
    @Test
    fun `normalizes missing outputs to stable root shapes`() {
        val condition = mapOf("id" to "condition")

        val normalized = SnapshotFormat.normalize(
            mapOf("Conditions.json" to listOf(condition))
        )

        assertEquals(SnapshotFormat.getDataFiles(), normalized.keys.toList())
        assertEquals(listOf(condition), normalized.getValue("Conditions.json"))
        assertEquals(emptyMap<String, Any>(), normalized.getValue("Operations.json"))
        assertEquals(
            mapOf(
                "algorithm" to "unresolved",
                "pluralOverrideSupported" to false,
                "rules" to emptyList<Any>()
            ),
            normalized.getValue("PluralRules.json")
        )
        assertEquals(
            mapOf("aliases" to emptyMap<String, Any>(), "targets" to emptyList<Any>()),
            normalized.getValue("Aliases.json")
        )
        assertEquals(emptyList<Any>(), normalized.getValue("Types.json"))
    }

    @Test
    fun `rejects outputs outside the snapshot contract`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            SnapshotFormat.normalize(mapOf("Unknown.json" to emptyList<Any>()))
        }

        assertEquals("Unexpected snapshot outputs: [Unknown.json]", exception.message)
    }

    @Test
    fun `manifest is part of all files but not normalized data outputs`() {
        assertEquals(19, SnapshotFormat.getAllFiles().size)
        assertEquals(SnapshotFormat.getAllFiles().sorted(), SnapshotFormat.getAllFiles())
        assertEquals(true, SnapshotFormat.getAllFiles().contains(SnapshotFormat.MANIFEST_FILE))
        assertEquals(false, SnapshotFormat.getDataFiles().contains(SnapshotFormat.MANIFEST_FILE))
    }
}
