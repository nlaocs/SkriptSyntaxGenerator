package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData

object SnapshotDigests {
    fun contentDigest(serializedOutputs: Map<String, String>): String =
        StableIds.digest(
            serializedOutputs.toSortedMap().entries.joinToString("|") { (fileName, json) ->
                "${fileName.length}:$fileName${json.length}:$json"
            }
        )

    fun snapshotId(
        schemaVersion: Int,
        contentDigest: String,
        server: ServerManifestData,
        language: String,
        plugins: List<PluginManifestData>,
        files: Collection<String>
    ): String = StableIds.digest(
        fingerprint(
            listOf(
                schemaVersion.toString(),
                contentDigest,
                server.fingerprint(),
                language,
                fingerprint(plugins.map(PluginManifestData::fingerprint)),
                fingerprint(files.sorted())
            )
        )
    )

    private fun fingerprint(parts: Collection<String>): String =
        parts.joinToString("|") { part -> "${part.length}:$part" }
}
