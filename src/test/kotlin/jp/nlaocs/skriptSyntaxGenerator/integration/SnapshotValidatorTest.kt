package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.ObjectMapper
import jp.nlaocs.skriptSyntaxGenerator.data.AliasesCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.EventValueApi
import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxApi
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxKindCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.generator.SnapshotFormat
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

        assertEquals(20, report.files)
        assertEquals(0, report.aliases)
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
    fun `rejects a class record without required methods`() {
        writeSnapshot(
            mapOf(
                "ClassHierarchy.json" to """[
                    {
                      "name":"example.Type",
                      "binaryName":"example.Type",
                      "kind":"Class",
                      "interfaces":[]
                    }
                ]""".trimIndent()
            )
        )

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("ClassHierarchy[0].methods must be an array"))
    }

    @Test
    fun `accepts declared method signature records`() {
        writeSnapshot(
            mapOf(
                "ClassHierarchy.json" to """[
                    {
                      "name":"example.Type",
                      "binaryName":"example.Type",
                      "kind":"Class",
                      "interfaces":[],
                      "methods":[
                        {"name":"alpha","parameterTypes":[],"returnType":"void","static":false},
                        {"name":"beta","parameterTypes":["java.lang.String"],"returnType":"int","static":true}
                      ]
                    }
                ]""".trimIndent()
            )
        )

        val report = SnapshotValidator.validate(tempDirectory)

        assertEquals(1, report.classes)
    }

    @Test
    fun `rejects non-string language values`() {
        writeSnapshot(mapOf("Language.json" to """{"boolean.true":true}"""))

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("Language.json[boolean.true] must be a string"))
    }

    @Test
    fun `rejects malformed event capability fields`() {
        writeSnapshot(
            mapOf(
                "Events.json" to """[
                    {
                      "registrationOrder":0,
                      "registrationId":"event:test",
                      "referenceEvents":[],
                      "eventValues":[],
                      "cancellable":false,
                      "prioritySupported":"yes",
                      "hasOnPrefix":false,
                      "addon":{"name":"Skript","version":"2.14.3"}
                    }
                ]""".trimIndent()
            )
        )

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("prioritySupported must be boolean"))
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

    @Test
    fun `rejects an alias target index outside the target table`() {
        writeSnapshot(
            mapOf(
                "Aliases.json" to """{
                    "aliases": {"stone": 1},
                    "targets": [
                      {"amount": 1, "all": false, "types": []}
                    ]
                }""".trimIndent()
            )
        )

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(tempDirectory)
        }

        assertTrue(error.message.orEmpty().contains("target index is out of bounds"))
    }

    @Test
    fun `rejects a snapshot generated for a different compatibility profile`() {
        writeSnapshot()

        val error = assertThrows<AssertionError> {
            SnapshotValidator.validate(
                tempDirectory,
                expectedEventValueMetadata = "modern-2.16",
                expectedSyntaxApi = "legacy-static",
                expectedMinecraftVersion = "1.12.2",
                expectedSkriptVersion = "2.6.4",
                expectedNonEmptyFiles = setOf("EventValues.json")
            )
        }

        assertTrue(error.message.orEmpty().contains("eventValueApi does not match profile"))
        assertTrue(error.message.orEmpty().contains("syntaxApi does not match profile"))
        assertTrue(error.message.orEmpty().contains("Minecraft version does not match profile"))
        assertTrue(error.message.orEmpty().contains("Skript version does not match profile"))
        assertTrue(error.message.orEmpty().contains("EventValues.json must not be empty"))
    }
    private fun writeSnapshot(overrides: Map<String, String> = emptyMap()) {
        val outputs = outputFiles.associateWith { fileName ->
            when (fileName) {
                SnapshotFormat.OPERATIONS_FILE -> "{}"
                SnapshotFormat.ALIASES_FILE -> "{\"aliases\":{},\"targets\":[]}"
                SnapshotFormat.PLURAL_RULES_FILE ->
                    """{"algorithm":"singular-aware","pluralOverrideSupported":true,"rules":[{"ruleOrder":0,"singular":"","plural":"s","completeWord":false,"origin":"built-in","addon":{"name":"Skript","version":"2.14.3"}}]}"""
                SnapshotFormat.LANGUAGE_FILE -> "{}"
                else -> "[]"
            }
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
        val capabilities = SnapshotCapabilitiesData(
            SyntaxApi.REGISTRY,
            EventValueApi.LEGACY,
            SyntaxKindCapabilitiesData.modern(),
            AliasesCapabilitiesData(true, true)
        )
        val files = SnapshotFormat.getAllFiles()
        val contentDigest = SnapshotDigests.contentDigest(outputs)
        val snapshotId = SnapshotDigests.snapshotId(
            schemaVersion = SnapshotFormat.SCHEMA_VERSION,
            contentDigest = contentDigest,
            server = server,
            language = "english",
            plugins = plugins,
            capabilities = capabilities,
            files = files
        )
        val manifest = linkedMapOf(
            "schemaVersion" to SnapshotFormat.SCHEMA_VERSION,
            "snapshotId" to snapshotId,
            "contentDigest" to contentDigest,
            "generatedAt" to "2026-01-01T00:00:00Z",
            "server" to server,
            "language" to "english",
            "plugins" to plugins,
            "capabilities" to capabilities,
            "files" to files
        )
        Files.writeString(
            tempDirectory.resolve(SnapshotFormat.MANIFEST_FILE),
            objectMapper.writeValueAsString(manifest)
        )
    }

    companion object {
        private val objectMapper = ObjectMapper()
        private val outputFiles = SnapshotFormat.getDataFiles().toSet()
    }
}
