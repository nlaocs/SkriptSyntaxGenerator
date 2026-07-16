package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.ObjectMapper
import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import jp.nlaocs.skriptSyntaxGenerator.util.SnapshotDigests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SnapshotValidatorTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun `accepts a minimal internally consistent snapshot`() {
        writeSnapshot()

        val report = SnapshotValidator.validate(tempDirectory)

        assertEquals(17, report.files)
        assertEquals(0, report.registrations)
    }

    @Test
    fun `rejects content changed after manifest generation`() {
        writeSnapshot()
        Files.writeString(tempDirectory.resolve("Conditions.json"), "[ ]")

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("contentDigest"))
    }

    @Test
    fun `rejects a missing output file with a useful error`() {
        writeSnapshot()
        Files.delete(tempDirectory.resolve("Types.json"))

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("missing=[Types.json]"))
    }

    @Test
    fun `rejects unresolved type references`() {
        writeSnapshot(
            mapOf(
                "Types.json" to """[
                    {
                      "typeParseOrder": 0,
                      "registrationId": "type:test",
                      "codeName": "test",
                      "assignableTo": ["missing"],
                      "addon": {"name": "Skript", "version": "2.14.3"}
                    }
                ]""".trimIndent()
            )
        )

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("missing assignableTo type missing"))
    }

    @Test
    fun `rejects duplicate registration ids`() {
        writeSnapshot(
            mapOf(
                "Types.json" to """[
                    {
                      "typeParseOrder": 0,
                      "registrationId": "type:duplicate",
                      "codeName": "first",
                      "assignableTo": [],
                      "addon": {"name": "Skript", "version": "2.14.3"}
                    },
                    {
                      "typeParseOrder": 1,
                      "registrationId": "type:duplicate",
                      "codeName": "second",
                      "assignableTo": [],
                      "addon": {"name": "Skript", "version": "2.14.3"}
                    }
                ]""".trimIndent()
            )
        )

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("Duplicate registrationIds"))
    }

    private fun writeSnapshot(overrides: Map<String, String> = emptyMap()) {
        val outputs = outputFiles.associateWith { fileName ->
            if (fileName == "Operations.json") "{}" else "[]"
        }.toMutableMap()
        outputs.putAll(overrides)
        outputs.forEach { (fileName, content) ->
            Files.writeString(tempDirectory.resolve(fileName), content)
        }

        val server = ServerManifestData(
            name = "Paper",
            version = "test",
            bukkitVersion = "test",
            minecraftVersion = "1.21.11",
            javaVersion = "21"
        )
        val plugins = listOf(
            PluginManifestData(0, "SkriptSyntaxGenerator", "1.0", "generator.Main", true, emptyList(), emptyList(), listOf("Skript"), null),
            PluginManifestData(1, "Skript", "2.14.3", "skript.Main", true, emptyList(), emptyList(), emptyList(), null)
        )
        val files = (outputFiles + "Manifest.json").sorted()
        val contentDigest = SnapshotDigests.contentDigest(outputs)
        val snapshotId = SnapshotDigests.snapshotId(
            schemaVersion = 1,
            contentDigest = contentDigest,
            server = server,
            language = "english",
            plugins = plugins,
            files = files
        )
        val manifest = linkedMapOf(
            "schemaVersion" to 1,
            "snapshotId" to snapshotId,
            "contentDigest" to contentDigest,
            "generatedAt" to "2026-01-01T00:00:00Z",
            "server" to server,
            "language" to "english",
            "plugins" to plugins,
            "files" to files
        )
        Files.writeString(
            tempDirectory.resolve("Manifest.json"),
            objectMapper.writeValueAsString(manifest)
        )
    }

    companion object {
        private val objectMapper = ObjectMapper()
        private val outputFiles = setOf(
            "ClassHierarchy.json",
            "Comparators.json",
            "Conditions.json",
            "Converters.json",
            "Differences.json",
            "Effects.json",
            "EventValues.json",
            "Events.json",
            "Expressions.json",
            "Functions.json",
            "Operations.json",
            "Operators.json",
            "Properties.json",
            "Sections.json",
            "Structures.json",
            "Types.json"
        )
    }
}
