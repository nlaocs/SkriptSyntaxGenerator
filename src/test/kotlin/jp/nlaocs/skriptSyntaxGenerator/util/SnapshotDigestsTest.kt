package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.AliasesCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.EventValueApi
import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxApi
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxKindCapabilitiesData
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
    private val capabilities = SnapshotCapabilitiesData(
        SyntaxApi.REGISTRY,
        EventValueApi.LEGACY,
        SyntaxKindCapabilitiesData.modern(),
        AliasesCapabilitiesData(true, true)
    )

    @Test
    fun `content digest is independent from map insertion order`() {
        val first = linkedMapOf("B.json" to "[]", "A.json" to "{}")
        val second = linkedMapOf("A.json" to "{}", "B.json" to "[]")

        assertEquals(SnapshotDigests.contentDigest(first), SnapshotDigests.contentDigest(second))
        assertEquals(
            StableIds.digest("6:A.json2:{}|6:B.json2:[]"),
            SnapshotDigests.contentDigest(first)
        )
        assertNotEquals(
            SnapshotDigests.contentDigest(first),
            SnapshotDigests.contentDigest(linkedMapOf("A.json" to "[]", "B.json" to "[]"))
        )
    }

    @Test
    fun `snapshot id sorts files and preserves environment capabilities`() {
        val first = snapshotId(capabilities, listOf(plugin), listOf("B.json", "A.json"))
        val reorderedFiles = snapshotId(capabilities, listOf(plugin), listOf("A.json", "B.json"))
        val changedPlugin = snapshotId(
            capabilities,
            listOf(plugin.copy(version = "2.15")),
            listOf("A.json", "B.json")
        )
        val changedCapabilities = snapshotId(
            SnapshotCapabilitiesData(
                SyntaxApi.REGISTRY,
                EventValueApi.MODERN_2_15,
                SyntaxKindCapabilitiesData.modern(),
                AliasesCapabilitiesData(true, true)
            ),
            listOf(plugin),
            listOf("A.json", "B.json")
        )

        assertEquals(first, reorderedFiles)
        assertNotEquals(first, changedPlugin)
        assertNotEquals(first, changedCapabilities)
    }

    private fun snapshotId(
        snapshotCapabilities: SnapshotCapabilitiesData,
        plugins: List<PluginManifestData>,
        files: List<String>
    ): String = SnapshotDigests.snapshotId(
        schemaVersion = 2,
        contentDigest = "digest",
        server = server,
        language = "english",
        plugins = plugins,
        capabilities = snapshotCapabilities,
        files = files
    )
}
