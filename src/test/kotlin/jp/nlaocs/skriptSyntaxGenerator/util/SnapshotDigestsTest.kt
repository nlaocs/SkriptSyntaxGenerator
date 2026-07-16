package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SnapshotDigestsTest {
    private val server = ServerManifestData("Paper", "1", "1", "1.21", "21")
    private val plugin = PluginManifestData(
        0,
        "Skript",
        "2.14",
        "main",
        true,
        emptyList(),
        emptyList(),
        emptyList(),
        "abc"
    )

    @Test
    fun `content digest is independent from map insertion order`() {
        val first = linkedMapOf("B.json" to "[]", "A.json" to "{}")
        val second = linkedMapOf("A.json" to "{}", "B.json" to "[]")

        assertEquals(SnapshotDigests.contentDigest(first), SnapshotDigests.contentDigest(second))
        assertNotEquals(
            SnapshotDigests.contentDigest(first),
            SnapshotDigests.contentDigest(linkedMapOf("A.json" to "[]", "B.json" to "[]"))
        )
    }

    @Test
    fun `snapshot id sorts files but preserves plugin content`() {
        val first = SnapshotDigests.snapshotId(
            1,
            "digest",
            server,
            "english",
            listOf(plugin),
            listOf("B.json", "A.json")
        )
        val reorderedFiles = SnapshotDigests.snapshotId(
            1,
            "digest",
            server,
            "english",
            listOf(plugin),
            listOf("A.json", "B.json")
        )
        val changedPlugin = SnapshotDigests.snapshotId(
            1,
            "digest",
            server,
            "english",
            listOf(plugin.copy(version = "2.15")),
            listOf("A.json", "B.json")
        )

        assertEquals(first, reorderedFiles)
        assertNotEquals(first, changedPlugin)
    }
}
