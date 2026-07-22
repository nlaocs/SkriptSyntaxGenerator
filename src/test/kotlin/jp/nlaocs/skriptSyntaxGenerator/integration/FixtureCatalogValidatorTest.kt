package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class FixtureCatalogValidatorTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun `validates applicable assertions from embedded catalog`() {
        val plugins = tempDirectory.resolve("plugins")
        val jar = writeCatalogJar(plugins)
        val documents = fixtureDocuments()

        val report = FixtureCatalogValidator.validate(
            documents,
            plugins,
            expectedSkriptVersion = "2.14.3"
        )

        assertEquals(jar, report.catalogJar)
        assertEquals(3, report.assertions)
    }

    @Test
    fun `accepts a fixture jar directly`() {
        val jar = writeCatalogJar(tempDirectory.resolve("plugins"))

        val report = FixtureCatalogValidator.validate(
            fixtureDocuments(),
            jar,
            expectedSkriptVersion = "2.14.3"
        )

        assertEquals(jar, report.catalogJar)
        assertEquals(3, report.assertions)
    }

    @Test
    fun `reports mismatched expected fields and forbidden patterns`() {
        val plugins = tempDirectory.resolve("plugins")
        writeCatalogJar(plugins)
        val documents = fixtureDocuments(
            effect = """{
                "elementClass": "fixture.Effect",
                "name": "Wrong name",
                "patterns": ["fixture effect", "placeholder"]
            }""".trimIndent()
        )

        val error = assertThrows<AssertionError> {
            FixtureCatalogValidator.validate(documents, plugins, "2.14.3")
        }

        assertTrue(error.message.orEmpty().contains("fixture-effect field name differs"))
        assertTrue(error.message.orEmpty().contains("forbidden patterns"))
    }

    @Test
    fun `legacy capabilities exclude only unavailable metadata fields`() {
        val capabilities = FixtureCatalogCapabilities(
            expressionImplementationMetadata = false,
            typeRegistrationOrdering = false
        )

        assertFalse(capabilities.supports("Expressions.json", "returnTypeMultiplicity"))
        assertFalse(capabilities.supports("Expressions.json", "acceptedChangersState"))
        assertFalse(capabilities.supports("Types.json", "before"))
        assertFalse(capabilities.supports("Types.json", "after"))
        assertTrue(capabilities.supports("Expressions.json", "patterns"))
        assertTrue(capabilities.supports("Types.json", "codeName"))
        assertTrue(capabilities.supports("Effects.json", "acceptedChangersState"))
    }

    @Test
    fun `requires an embedded fixture catalog`() {
        val plugins = tempDirectory.resolve("plugins")
        Files.createDirectories(plugins)

        val error = assertThrows<AssertionError> {
            FixtureCatalogValidator.validate(emptyMap(), plugins, "2.14.3")
        }

        assertTrue(error.message.orEmpty().contains("No SkriptDummyAddon fixture catalog"))
    }

    private fun fixtureDocuments(
        effect: String = """{
            "elementClass": "fixture.Effect",
            "name": "Fixture Effect",
            "patterns": ["fixture effect"]
        }""".trimIndent()
    ): Map<String, JsonNode> = mapOf(
        "Effects.json" to objectMapper.readTree("[$effect]"),
        "PluralRules.json" to objectMapper.readTree(
            """{
                "algorithm": "singular-aware",
                "pluralOverrideSupported": true,
                "rules": [
                    {
                      "ruleOrder": 0,
                      "singular": "fixtureperson",
                      "plural": "fixturepeople",
                      "completeWord": true,
                      "origin": "override",
                      "overrideRegistrationOrder": 0,
                      "addon": {"name": "FixtureAddon", "version": "1.0"}
                    }
                ]
            }""".trimIndent()
        ),
        "Operations.json" to objectMapper.readTree(
            """{
                "fixture": [
                    {
                      "left": "fixture.Left",
                      "right": "fixture.Right",
                      "returnType": "fixture.Result"
                    }
                ]
            }""".trimIndent()
        )
    )

    private fun writeCatalogJar(plugins: Path): Path {
        val directory = plugins.resolve(".paper-remapped").resolve("extra-plugins")
        Files.createDirectories(directory)
        val jarPath = directory.resolve("SkriptDummyAddon.jar")
        val catalog = """{
          "schemaVersion": 1,
          "addon": "SkriptDummyAddon",
          "profiles": [
            {"skript": "2.14.3"}
          ],
          "assertions": [
            {
              "key": "fixture-effect",
              "since": "2.6.4",
              "file": "Effects.json",
              "match": {"elementClass": "fixture.Effect"},
              "expected": {
                "name": "Fixture Effect",
                "patterns": ["fixture effect"]
              },
              "forbiddenPatterns": ["placeholder"]
            },
            {
              "key": "fixture-operation",
              "since": "2.8.7",
              "file": "Operations.json",
              "match": {
                "left": "fixture.Left",
                "right": "fixture.Right"
              },
              "expected": {"returnType": "fixture.Result"}
            },
            {
              "key": "fixture-plural-override",
              "since": "2.14.3",
              "file": "PluralRules.json",
              "match": {"singular": "fixtureperson"},
              "expected": {
                "plural": "fixturepeople",
                "origin": "override",
                "addon": {"name": "FixtureAddon"}
              }
            },
            {
              "key": "future-fixture",
              "since": "2.15.4",
              "file": "Effects.json",
              "match": {"elementClass": "fixture.Future"}
            }
          ]
        }""".trimIndent()

        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes.putValue("Skript-Compatibility", "2.14.3")
        }
        JarOutputStream(Files.newOutputStream(jarPath), manifest).use { jar ->
            jar.putNextEntry(JarEntry("META-INF/skript-dummy-addon/catalog.json"))
            jar.write(catalog.toByteArray(Charsets.UTF_8))
            jar.closeEntry()
        }
        return jarPath
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
