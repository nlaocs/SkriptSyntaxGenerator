package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jp.nlaocs.skriptSyntaxGenerator.data.PluginManifestData
import jp.nlaocs.skriptSyntaxGenerator.data.ServerManifestData
import jp.nlaocs.skriptSyntaxGenerator.util.SnapshotDigests
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

data class SnapshotValidationReport(
    val directory: Path,
    val files: Int,
    val registrations: Int,
    val types: Int,
    val eventValues: Int,
    val classes: Int
)

object SnapshotValidator {
    private val objectMapper = ObjectMapper()

    private val requiredFiles = setOf(
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
        "Manifest.json",
        "Operations.json",
        "Operators.json",
        "Properties.json",
        "Sections.json",
        "Structures.json",
        "Types.json"
    )

    private val registrationOrderFiles = setOf(
        "Comparators.json",
        "Conditions.json",
        "Converters.json",
        "Differences.json",
        "Effects.json",
        "Events.json",
        "Expressions.json",
        "Functions.json",
        "Operators.json",
        "Sections.json",
        "Structures.json"
    )

    fun validate(directory: Path): SnapshotValidationReport {
        val errors = mutableListOf<String>()
        require(directory.isDirectory()) { "Snapshot directory does not exist: $directory" }

        val actualFiles = Files.list(directory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "json" }
                .map(Path::name)
                .toList()
                .toSet()
        }
        expect(actualFiles == requiredFiles, errors) {
            "JSON files differ: missing=${requiredFiles - actualFiles}, unexpected=${actualFiles - requiredFiles}"
        }
        failIfAny(errors)

        val rawFiles = actualFiles.associateWith { directory.resolve(it).readText() }
        val documents = rawFiles.mapValues { (fileName, json) ->
            runCatching { objectMapper.readTree(json) }
                .getOrElse { throwable ->
                    errors += "$fileName is not valid JSON: ${throwable.message}"
                    objectMapper.nullNode()
                }
        }

        failIfAny(errors)

        (requiredFiles - setOf("Manifest.json", "Operations.json")).forEach { fileName ->
            expect(documents.getValue(fileName).isArray, errors) { "$fileName root must be an array" }
        }
        expect(documents.getValue("Operations.json").isObject, errors) {
            "Operations.json root must be an object"
        }
        expect(documents.getValue("Manifest.json").isObject, errors) {
            "Manifest.json root must be an object"
        }
        failIfAny(errors)

        val manifest = documents.getValue("Manifest.json")
        validateManifest(manifest, rawFiles, errors)

        registrationOrderFiles.forEach { fileName ->
            validateSequentialOrder(documents.getValue(fileName), "registrationOrder", fileName, errors)
        }
        validateSequentialOrder(flattenObjectArrays(documents.getValue("Operations.json")), "registrationOrder", "Operations.json", errors)
        validateSequentialOrder(documents.getValue("Types.json"), "typeParseOrder", "Types.json", errors)
        validateSequentialOrder(documents.getValue("EventValues.json"), "resolutionOrder", "EventValues.json", errors)

        val registrationNodes = buildList {
            registrationOrderFiles.forEach { addAll(documents.getValue(it)) }
            addAll(flattenObjectArrays(documents.getValue("Operations.json")))
            addAll(documents.getValue("Types.json"))
            addAll(documents.getValue("EventValues.json"))
            addAll(documents.getValue("Properties.json"))
        }
        validateRegistrationIds(registrationNodes, errors)
        documents.forEach { (fileName, root) -> validateProviders(root, fileName, errors) }
        validateTypeReferences(documents, errors)
        validateEventValueReferences(documents, errors)
        validateClassHierarchy(documents.getValue("ClassHierarchy.json"), errors)

        if (errors.isNotEmpty()) {
            throw AssertionError(
                buildString {
                    appendLine("Snapshot validation failed with ${errors.size} problem(s):")
                    errors.take(50).forEach { appendLine("- $it") }
                    if (errors.size > 50) append("- ... and ${errors.size - 50} more")
                }.trimEnd()
            )
        }

        return SnapshotValidationReport(
            directory = directory,
            files = actualFiles.size,
            registrations = registrationNodes.size,
            types = documents.getValue("Types.json").size(),
            eventValues = documents.getValue("EventValues.json").size(),
            classes = documents.getValue("ClassHierarchy.json").size()
        )
    }

    private fun validateManifest(
        manifest: JsonNode,
        rawFiles: Map<String, String>,
        errors: MutableList<String>
    ) {
        val declaredFiles = manifest["files"]?.map(JsonNode::asText).orEmpty()
        expect(declaredFiles == declaredFiles.sorted(), errors) { "Manifest files are not sorted" }
        expect(declaredFiles.toSet() == requiredFiles, errors) { "Manifest files do not match the snapshot files" }
        expect(manifest["schemaVersion"]?.asInt() == 1, errors) { "Unsupported or missing schemaVersion" }
        runCatching { Instant.parse(manifest.requiredText("generatedAt")) }
            .onFailure { errors += "Manifest generatedAt is not an ISO-8601 instant" }

        val serializedOutputs = rawFiles.filterKeys { it != "Manifest.json" }
        val contentDigest = SnapshotDigests.contentDigest(serializedOutputs)
        expect(manifest.requiredText("contentDigest") == contentDigest, errors) {
            "Manifest contentDigest does not match generated files"
        }

        val serverNode = manifest["server"] ?: objectMapper.nullNode()
        val server = ServerManifestData(
            name = serverNode.requiredText("name"),
            version = serverNode.requiredText("version"),
            bukkitVersion = serverNode.requiredText("bukkitVersion"),
            minecraftVersion = serverNode.requiredText("minecraftVersion"),
            javaVersion = serverNode.requiredText("javaVersion")
        )
        val plugins = manifest["plugins"]?.map { plugin ->
            PluginManifestData(
                loadOrder = plugin["loadOrder"]?.asInt() ?: -1,
                name = plugin.requiredText("name"),
                version = plugin.requiredText("version"),
                main = plugin.requiredText("main"),
                enabled = plugin["enabled"]?.asBoolean() ?: false,
                depend = plugin.textList("depend"),
                softDepend = plugin.textList("softDepend"),
                loadBefore = plugin.textList("loadBefore"),
                jarSha256 = plugin["jarSha256"]?.takeUnless(JsonNode::isNull)?.asText()
            )
        }.orEmpty()
        expect(plugins.map(PluginManifestData::loadOrder) == plugins.indices.toList(), errors) {
            "Plugin loadOrder is not contiguous"
        }
        expect(plugins.any { it.name == "Skript" && it.enabled }, errors) { "Enabled Skript plugin is missing from Manifest" }
        expect(plugins.any { it.name == "SkriptSyntaxGenerator" && it.enabled }, errors) {
            "Enabled SkriptSyntaxGenerator plugin is missing from Manifest"
        }

        val snapshotId = SnapshotDigests.snapshotId(
            schemaVersion = manifest["schemaVersion"]?.asInt() ?: -1,
            contentDigest = contentDigest,
            server = server,
            language = manifest.requiredText("language"),
            plugins = plugins,
            files = declaredFiles
        )
        expect(manifest.requiredText("snapshotId") == snapshotId, errors) {
            "Manifest snapshotId cannot be reproduced"
        }
    }

    private fun validateSequentialOrder(
        nodes: Iterable<JsonNode>,
        field: String,
        fileName: String,
        errors: MutableList<String>
    ) {
        val emittedNodes = nodes.toList()
        val values = emittedNodes.mapNotNull { it[field]?.takeIf(JsonNode::isIntegralNumber)?.asInt() }
        expect(values.size == emittedNodes.size, errors) {
            "$fileName has ${emittedNodes.size - values.size} record(s) without an integer $field"
        }
        expect(values == values.indices.toList(), errors) {
            "$fileName $field is not contiguous in emitted order"
        }
    }

    private fun validateRegistrationIds(nodes: List<JsonNode>, errors: MutableList<String>) {
        val ids = nodes.mapNotNull { it["registrationId"]?.asText()?.takeIf(String::isNotBlank) }
        expect(ids.size == nodes.size, errors) { "${nodes.size - ids.size} registrations have no registrationId" }
        val duplicates = ids.groupingBy(String::toString).eachCount().filterValues { it > 1 }.keys
        expect(duplicates.isEmpty(), errors) { "Duplicate registrationIds: ${duplicates.take(5)}" }
    }

    private fun validateProviders(root: JsonNode, fileName: String, errors: MutableList<String>) {
        root.walkObjects { node ->
            listOf("addon", "provider").forEach { field ->
                val provider = node[field] ?: return@forEach
                val name = provider["name"]?.asText().orEmpty()
                val version = provider["version"]?.asText().orEmpty()
                expect(name.isNotBlank() && !name.equals("unknown", ignoreCase = true), errors) {
                    "$fileName contains an unresolved $field name"
                }
                expect(version.isNotBlank() && !version.equals("unknown", ignoreCase = true), errors) {
                    "$fileName contains an unresolved $field version"
                }
            }
        }
    }

    private fun validateTypeReferences(documents: Map<String, JsonNode>, errors: MutableList<String>) {
        val types = documents.getValue("Types.json")
        val codeNames = types.mapNotNull { it["codeName"]?.asText() }.toSet()
        expect(codeNames.size == types.size(), errors) { "Type codeNames are missing or duplicated" }

        types.forEach { type ->
            type.textList("assignableTo").forEach { reference ->
                expect(reference in codeNames, errors) {
                    "Type ${type["codeName"]?.asText()} references missing assignableTo type $reference"
                }
            }
        }
        documents.getValue("Properties.json").forEach { property ->
            property["relatedTypes"]?.forEach { relatedType ->
                val reference = relatedType["typeCodeName"]?.asText().orEmpty()
                expect(reference in codeNames, errors) {
                    "Property ${property["name"]?.asText()} references missing type $reference"
                }
            }
        }
    }

    private fun validateEventValueReferences(documents: Map<String, JsonNode>, errors: MutableList<String>) {
        val eventValueIds = documents.getValue("EventValues.json")
            .mapNotNull { it["registrationId"]?.asText() }
            .toSet()
        documents.getValue("Events.json").forEach { event ->
            event["eventValues"]?.forEach { value ->
                val reference = value["registrationId"]?.asText().orEmpty()
                expect(reference in eventValueIds, errors) {
                    "Event ${event["registrationId"]?.asText()} references missing event value $reference"
                }
            }
        }
    }

    private fun validateClassHierarchy(classes: JsonNode, errors: MutableList<String>) {
        val names = classes.mapNotNull { it["name"]?.asText() }
        expect(names == names.sorted(), errors) { "ClassHierarchy names are not sorted" }
        expect(names.size == names.toSet().size, errors) { "ClassHierarchy contains duplicate names" }
    }

    private fun flattenObjectArrays(root: JsonNode): List<JsonNode> =
        root.fields().asSequence().flatMap { (_, values) -> values.asSequence() }.toList()

    private fun JsonNode.requiredText(field: String): String =
        this[field]?.asText()?.takeIf(String::isNotBlank).orEmpty()

    private fun JsonNode.textList(field: String): List<String> =
        this[field]?.map(JsonNode::asText).orEmpty()

    private fun JsonNode.walkObjects(visitor: (JsonNode) -> Unit) {
        when {
            isObject -> {
                visitor(this)
                elements().forEachRemaining { it.walkObjects(visitor) }
            }
            isArray -> elements().forEachRemaining { it.walkObjects(visitor) }
        }
    }

    private inline fun expect(condition: Boolean, errors: MutableList<String>, message: () -> String) {
        if (!condition) errors += message()
    }

    private fun failIfAny(errors: List<String>) {
        if (errors.isNotEmpty()) throw AssertionError(errors.joinToString("\n"))
    }
}

object SnapshotValidatorMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: SnapshotValidatorMain <snapshot-directory>" }
        val report = SnapshotValidator.validate(Path.of(args.single()))
        println(
            "Validated ${report.files} files, ${report.registrations} registrations, " +
                "${report.types} types, ${report.eventValues} event values, and ${report.classes} classes."
        )
    }
}
