package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

data class FixtureCatalogValidationReport(
    val catalogJar: Path,
    val assertions: Int
)

data class FixtureCatalogCapabilities(
    val expressionImplementationMetadata: Boolean = true,
    val typeRegistrationOrdering: Boolean = true
) {
    fun supports(fileName: String, field: String): Boolean = when {
        fileName == "Expressions.json" && field in EXPRESSION_IMPLEMENTATION_FIELDS ->
            expressionImplementationMetadata
        fileName == "Types.json" && field in TYPE_REGISTRATION_ORDER_FIELDS ->
            typeRegistrationOrdering
        else -> true
    }

    private companion object {
        val EXPRESSION_IMPLEMENTATION_FIELDS = setOf(
            "returnTypeMultiplicity",
            "returnTypeMultiplicityState",
            "acceptedChangers",
            "acceptedChangersState"
        )
        val TYPE_REGISTRATION_ORDER_FIELDS = setOf("before", "after")
    }
}

object FixtureCatalogValidator {
    private const val CATALOG_ENTRY = "META-INF/skript-dummy-addon/catalog.json"
    private val objectMapper = ObjectMapper()

    fun validate(
        snapshotDocuments: Map<String, JsonNode>,
        catalogLocation: Path,
        expectedSkriptVersion: String,
        capabilities: FixtureCatalogCapabilities = FixtureCatalogCapabilities()
    ): FixtureCatalogValidationReport {
        require(catalogLocation.isDirectory() || catalogLocation.isRegularFile()) {
            "Fixture catalog location does not exist: $catalogLocation"
        }

        val loaded = loadCatalog(catalogLocation)
        val catalog = loaded.document
        val errors = mutableListOf<String>()

        expect(catalog["schemaVersion"]?.asInt() == 1, errors) {
            "Unsupported or missing fixture catalog schemaVersion"
        }
        expect(catalog["addon"]?.asText() == "SkriptDummyAddon", errors) {
            "Fixture catalog addon must be SkriptDummyAddon"
        }
        expect(loaded.skriptCompatibility == expectedSkriptVersion, errors) {
            "Fixture JAR targets ${loaded.skriptCompatibility}, expected $expectedSkriptVersion"
        }

        val profiles = catalog["profiles"]
        expect(profiles?.isArray == true, errors) {
            "Fixture catalog profiles must be an array"
        }
        expect(
            profiles?.any { it["skript"]?.asText() == expectedSkriptVersion } == true,
            errors
        ) {
            "Fixture catalog does not declare Skript $expectedSkriptVersion"
        }

        val assertionsNode = catalog["assertions"]
        expect(assertionsNode?.isArray == true, errors) {
            "Fixture catalog assertions must be an array"
        }

        val applicableAssertions = assertionsNode
            ?.filter { assertion ->
                val since = assertion["since"]?.asText().orEmpty()
                since.isNotBlank() && versionAtLeast(expectedSkriptVersion, since)
            }
            .orEmpty()

        applicableAssertions.forEach { assertion ->
            validateAssertion(assertion, snapshotDocuments, capabilities, errors)
        }

        if (errors.isNotEmpty()) {
            throw AssertionError(
                buildString {
                    appendLine("Fixture catalog validation failed with ${errors.size} problem(s):")
                    errors.forEach { appendLine("- $it") }
                }.trimEnd()
            )
        }

        return FixtureCatalogValidationReport(
            catalogJar = loaded.jar,
            assertions = applicableAssertions.size
        )
    }

    private fun validateAssertion(
        assertion: JsonNode,
        documents: Map<String, JsonNode>,
        capabilities: FixtureCatalogCapabilities,
        errors: MutableList<String>
    ) {
        val key = assertion["key"]?.asText()?.takeIf(String::isNotBlank) ?: "<unnamed>"
        val fileName = assertion["file"]?.asText()?.takeIf(String::isNotBlank)
        if (fileName == null) {
            errors += "$key has no target file"
            return
        }

        val root = documents[fileName]
        if (root == null) {
            errors += "$key targets unknown snapshot file $fileName"
            return
        }

        val records = when {
            root.isArray -> root.toList()
            fileName == "Operations.json" && root.isObject ->
                root.fields().asSequence().flatMap { (_, values) -> values.asSequence() }.toList()
            fileName == "PluralRules.json" && root.isObject ->
                root.path("rules").toList()
            else -> {
                errors += "$key cannot search non-array root in $fileName"
                return
            }
        }

        val match = assertion["match"]
        if (match?.isObject != true) {
            errors += "$key match must be an object"
            return
        }

        val matches = records.filter { record ->
            match.fields().asSequence().all { (field, expected) ->
                record[field] == expected
            }
        }
        expect(matches.size == 1, errors) {
            "$key expected exactly one match in $fileName, found ${matches.size}"
        }
        val record = matches.singleOrNull() ?: return

        assertion["expected"]?.let { expected ->
            if (!expected.isObject) {
                errors += "$key expected must be an object"
            } else {
                expected.fields().forEachRemaining { (field, expectedValue) ->
                    if (!capabilities.supports(fileName, field)) return@forEachRemaining
                    expect(matchesExpected(record[field], expectedValue), errors) {
                        "$key field $field differs: expected=$expectedValue, actual=${record[field]}"
                    }
                }
            }
        }

        val forbiddenPatterns = assertion["forbiddenPatterns"]?.map(JsonNode::asText).orEmpty()
        if (forbiddenPatterns.isNotEmpty()) {
            val actualPatterns = record["patterns"]?.map(JsonNode::asText).orEmpty().toSet()
            val present = forbiddenPatterns.filter(actualPatterns::contains)
            expect(present.isEmpty(), errors) {
                "$key contains forbidden patterns: $present"
            }
        }
    }

    private fun loadCatalog(catalogLocation: Path): LoadedCatalog {
        val jars = if (catalogLocation.isRegularFile()) {
            listOf(catalogLocation)
        } else {
            Files.walk(catalogLocation).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.extension.equals("jar", ignoreCase = true) }
                    .sorted()
                    .toList()
            }
        }

        jars.forEach { jarPath ->
            val payload = runCatching {
                JarFile(jarPath.toFile()).use { jar ->
                    val entry = jar.getJarEntry(CATALOG_ENTRY) ?: return@use null
                    val json = jar.getInputStream(entry)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    CatalogPayload(
                        document = objectMapper.readTree(json),
                        skriptCompatibility = jar.manifest
                            ?.mainAttributes
                            ?.getValue("Skript-Compatibility")
                    )
                }
            }.getOrNull()
            if (payload != null) {
                return LoadedCatalog(jarPath, payload.document, payload.skriptCompatibility)
            }
        }

        throw AssertionError(
            "No SkriptDummyAddon fixture catalog was found at $catalogLocation"
        )
    }

    private fun versionAtLeast(actual: String, minimum: String): Boolean {
        val actualParts = versionParts(actual)
        val minimumParts = versionParts(minimum)
        val length = maxOf(actualParts.size, minimumParts.size)
        return (0 until length)
            .map { index ->
                actualParts.getOrElse(index) { 0 }.compareTo(minimumParts.getOrElse(index) { 0 })
            }
            .firstOrNull { it != 0 }
            ?.let { it > 0 }
            ?: true
    }

    private fun versionParts(version: String): List<Int> =
        version.split(".").map { part ->
            part.takeWhile(Char::isDigit).toIntOrNull() ?: 0
        }

    private fun matchesExpected(actual: JsonNode?, expected: JsonNode): Boolean {
        if (!expected.isObject) return actual == expected
        if (actual?.isObject != true) return false

        return expected.fields().asSequence().all { (field, expectedValue) ->
            matchesExpected(actual[field], expectedValue)
        }
    }

    private inline fun expect(
        condition: Boolean,
        errors: MutableList<String>,
        message: () -> String
    ) {
        if (!condition) errors += message()
    }

    private data class LoadedCatalog(
        val jar: Path,
        val document: JsonNode,
        val skriptCompatibility: String?
    )

    private data class CatalogPayload(
        val document: JsonNode,
        val skriptCompatibility: String?
    )
}
