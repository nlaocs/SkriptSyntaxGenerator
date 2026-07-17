package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.Skript
import ch.njol.skript.localization.Language
import jp.nlaocs.skriptSyntaxGenerator.generator.SnapshotFormat
import jp.nlaocs.skriptSyntaxGenerator.util.SnapshotDigests
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant

data class SnapshotManifestData(
    val schemaVersion: Int,
    val snapshotId: String,
    val contentDigest: String,
    val generatedAt: String,
    val server: ServerManifestData,
    val language: String,
    val plugins: List<PluginManifestData>,
    val capabilities: SnapshotCapabilitiesData,
    val files: List<String>
) {
    companion object {
        fun create(
            files: Collection<String>,
            contentDigest: String,
            capabilities: SnapshotCapabilitiesData
        ): SnapshotManifestData {
            val server = ServerManifestData(
                name = Bukkit.getName(),
                version = Bukkit.getVersion(),
                bukkitVersion = Bukkit.getBukkitVersion(),
                minecraftVersion = Skript.getMinecraftVersion().toString(),
                javaVersion = System.getProperty("java.version")
            )
            val language = Language.getName()
            val plugins = Bukkit.getPluginManager().plugins
                .mapIndexed { index, plugin -> PluginManifestData.from(plugin, index) }
            val orderedFiles = files.sorted()
            val snapshotId = SnapshotDigests.snapshotId(
                schemaVersion = SnapshotFormat.SCHEMA_VERSION,
                contentDigest = contentDigest,
                server = server,
                language = language,
                plugins = plugins,
                capabilities = capabilities,
                files = orderedFiles
            )

            return SnapshotManifestData(
                schemaVersion = SnapshotFormat.SCHEMA_VERSION,
                snapshotId = snapshotId,
                contentDigest = contentDigest,
                generatedAt = Instant.now().toString(),
                server = server,
                language = language,
                plugins = plugins,
                capabilities = capabilities,
                files = orderedFiles
            )
        }
    }
}

data class ServerManifestData(
    val name: String,
    val version: String,
    val bukkitVersion: String,
    val minecraftVersion: String,
    val javaVersion: String
) {
    fun fingerprint(): String =
        listOf(name, version, bukkitVersion, minecraftVersion, javaVersion)
            .joinToString("|") { part -> "${part.length}:$part" }
}

data class PluginManifestData(
    val loadOrder: Int,
    val name: String,
    val version: String,
    val main: String,
    val enabled: Boolean,
    val depend: List<String>,
    val softDepend: List<String>,
    val loadBefore: List<String>,
    val jarSha256: String?
) {
    fun fingerprint(): String = listOf(
        loadOrder.toString(),
        name,
        version,
        main,
        enabled.toString(),
        depend.joinToString(","),
        softDepend.joinToString(","),
        loadBefore.joinToString(","),
        jarSha256.orEmpty()
    ).joinToString("|") { part -> "${part.length}:$part" }

    companion object {
        fun from(plugin: Plugin, loadOrder: Int): PluginManifestData {
            val description = plugin.description
            return PluginManifestData(
                loadOrder = loadOrder,
                name = plugin.name,
                version = description.version,
                main = description.main,
                enabled = plugin.isEnabled,
                depend = description.depend.toList(),
                softDepend = description.softDepend.toList(),
                loadBefore = description.loadBefore.toList(),
                jarSha256 = hashPluginJar(plugin)
            )
        }

        private fun hashPluginJar(plugin: Plugin): String? = runCatching {
            val location = plugin.javaClass.protectionDomain?.codeSource?.location
                ?: return@runCatching null
            val path = Paths.get(location.toURI())
            if (!Files.isRegularFile(path)) {
                return@runCatching null
            }

            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(path).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
        }.getOrNull()
    }
}
