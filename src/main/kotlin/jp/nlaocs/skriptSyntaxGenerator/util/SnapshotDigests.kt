package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotCapabilitiesData

object SnapshotDigests {
    fun contentDigest(serializedOutputs: Map<String, String>): String =
        StableIds.digest { writer ->
            serializedOutputs.toSortedMap().entries.forEachIndexed { index, (fileName, json) ->
                if (index > 0) writer.append('|')
                writer.append(fileName.length.toString())
                    .append(':')
                    .append(fileName)
                    .append(json.length.toString())
                    .append(':')
                    .append(json)
            }
        }

    fun snapshotId(
        schemaVersion: Int,
        contentDigest: String,
        server: ServerManifestData,
        language: String,
        plugins: List<PluginManifestData>,
        capabilities: SnapshotCapabilitiesData,
        files: Collection<String>
    ): String = StableIds.digest(
        fingerprint(
            listOf(
                schemaVersion.toString(),
                contentDigest,
                server.fingerprint(),
                language,
                fingerprint(plugins.map(PluginManifestData::fingerprint)),
                capabilities.fingerprint(),
                fingerprint(files.sorted())
            )
        )
    )

    private fun fingerprint(parts: Collection<String>): String =
        parts.joinToString("|") { part -> "${part.length}:$part" }
}
