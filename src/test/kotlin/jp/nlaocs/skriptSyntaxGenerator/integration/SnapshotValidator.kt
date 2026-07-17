package jp.nlaocs.skriptSyntaxGenerator.integration

import com.fasterxml.jackson.databind.JsonNode
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
    val aliases: Int,
    val registrations: Int,
    val types: Int,
    val eventValues: Int,
    val classes: Int
)

object SnapshotValidator {
    private val objectMapper = ObjectMapper()

    private val requiredFiles = SnapshotFormat.getAllFiles().toSet()

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

    fun validate(
        directory: Path,
        expectedEventValueMetadata: String? = null,
        expectedSyntaxApi: String? = null,
        expectedMinecraftVersion: String? = null,
        expectedSkriptVersion: String? = null,
        expectedNonEmptyFiles: Set<String> = emptySet()
    ): SnapshotValidationReport {
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

        (requiredFiles - setOf("Manifest.json", "Operations.json", "Aliases.json")).forEach { fileName ->
            expect(documents.getValue(fileName).isArray, errors) { "$fileName root must be an array" }
        }
        expect(documents.getValue("Operations.json").isObject, errors) {
            "Operations.json root must be an object"
        }
        expect(documents.getValue("Aliases.json").isObject, errors) {
            "Aliases.json root must be an object"
        }
        expect(documents.getValue("Manifest.json").isObject, errors) {
            "Manifest.json root must be an object"
        }
        failIfAny(errors)

        expectedNonEmptyFiles.forEach { fileName ->
            val document = documents[fileName]
            expect(document != null, errors) {
                "Required non-empty file is not part of the snapshot contract: $fileName"
            }
            if (document != null) {
                val size = when (fileName) {
                    "Operations.json" -> flattenObjectArrays(document).size
                    "Aliases.json" -> document["aliases"]?.size() ?: 0
                    else -> document.size()
                }
                expect(size > 0, errors) { "$fileName must not be empty for this compatibility profile" }
            }
        }
        val manifest = documents.getValue("Manifest.json")
        validateManifest(manifest, rawFiles, errors)
        validateExpectedManifest(
            manifest,
            expectedEventValueMetadata,
            expectedSyntaxApi,
            expectedMinecraftVersion,
            expectedSkriptVersion,
            errors
        )

        if ("Aliases.json" in expectedNonEmptyFiles) {
            val aliases = manifest["capabilities"]?.get("aliases")
            expect(aliases?.get("supported")?.asBoolean() == true, errors) {
                "Manifest global aliases are not supported for this compatibility profile"
            }
            expect(aliases?.get("collected")?.asBoolean() == true, errors) {
                "Manifest global aliases were not collected for this compatibility profile"
            }
        }
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
        documents.filterKeys { it != "Aliases.json" }.forEach { (fileName, root) ->
            validateProviders(root, fileName, errors)
        }
        validateAliases(documents.getValue("Aliases.json"), errors)
        validateTypeReferences(documents, errors)
        validateEventValueReferences(documents, errors)
        validateEventValueMetadata(
            documents.getValue("EventValues.json"),
            expectedEventValueMetadata,
            errors
        )
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
            aliases = documents.getValue("Aliases.json")["aliases"]?.size() ?: 0,
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
        expect(manifest["schemaVersion"]?.asInt() == SnapshotFormat.SCHEMA_VERSION, errors) {
            "Unsupported or missing schemaVersion"
        }
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
        val capabilities = parseCapabilities(manifest["capabilities"], errors)

        val snapshotId = SnapshotDigests.snapshotId(
            schemaVersion = manifest["schemaVersion"]?.asInt() ?: -1,
            contentDigest = contentDigest,
            server = server,
            language = manifest.requiredText("language"),
            plugins = plugins,
            capabilities = capabilities,
            files = declaredFiles
        )
        expect(manifest.requiredText("snapshotId") == snapshotId, errors) {
            "Manifest snapshotId cannot be reproduced"
        }
    }

    private fun parseCapabilities(
        node: JsonNode?,
        errors: MutableList<String>
    ): SnapshotCapabilitiesData {
        val capabilities = node ?: objectMapper.nullNode()
        val syntaxApi = enumValue<SyntaxApi>(capabilities.requiredText("syntaxApi")) { it.serializedName }
        val eventValueApi = enumValue<EventValueApi>(capabilities.requiredText("eventValueApi")) {
            it.serializedName
        }
        expect(syntaxApi != null, errors) { "Manifest capabilities.syntaxApi is invalid" }
        expect(eventValueApi != null, errors) { "Manifest capabilities.eventValueApi is invalid" }

        val kinds = capabilities["syntaxKinds"] ?: objectMapper.nullNode()
        fun requiredBoolean(field: String): Boolean {
            val value = kinds[field]
            expect(value?.isBoolean == true, errors) {
                "Manifest capabilities.syntaxKinds.$field must be a boolean"
            }
            return value?.asBoolean() ?: false
        }
        val syntaxKinds = SyntaxKindCapabilitiesData(
            requiredBoolean("conditions"),
            requiredBoolean("effects"),
            requiredBoolean("events"),
            requiredBoolean("expressions"),
            requiredBoolean("types"),
            requiredBoolean("functions"),
            requiredBoolean("sections"),
            requiredBoolean("structures"),
            requiredBoolean("properties"),
            requiredBoolean("arithmetic"),
            requiredBoolean("converters"),
            requiredBoolean("comparators"),
            requiredBoolean("eventValues")
        )

        val aliases = capabilities["aliases"] ?: objectMapper.nullNode()
        expect(aliases.fieldNames().asSequence().toSet() == setOf("supported", "collected"), errors) {
            "Manifest capabilities.aliases contains unsupported fields"
        }
        val aliasesSupported = aliases["supported"]?.takeIf(JsonNode::isBoolean)?.asBoolean()
        expect(aliasesSupported != null, errors) {
            "Manifest capabilities.aliases.supported must be a boolean"
        }
        val aliasesCollected = aliases["collected"]?.takeIf(JsonNode::isBoolean)?.asBoolean()
        expect(aliasesCollected != null, errors) {
            "Manifest capabilities.aliases.collected must be a boolean"
        }
        expect(aliasesCollected != true || aliasesSupported == true, errors) {
            "Manifest cannot collect unsupported aliases"
        }
        return SnapshotCapabilitiesData(
            syntaxApi ?: SyntaxApi.REGISTRY,
            eventValueApi ?: EventValueApi.LEGACY,
            syntaxKinds,
            AliasesCapabilitiesData(aliasesSupported ?: false, aliasesCollected ?: false)
        )
    }

    private fun validateExpectedManifest(
        manifest: JsonNode,
        expectedEventValueMetadata: String?,
        expectedSyntaxApi: String?,
        expectedMinecraftVersion: String?,
        expectedSkriptVersion: String?,
        errors: MutableList<String>
    ) {
        expectedEventValueMetadata?.let { expectedShape ->
            val expectedApi = if (expectedShape == "legacy-static") "legacy" else expectedShape
            val actual = manifest["capabilities"]?.requiredText("eventValueApi")
            expect(actual == expectedApi, errors) {
                "Manifest eventValueApi does not match profile: expected=$expectedApi, actual=$actual"
            }
        }
        expectedSyntaxApi?.let { expected ->
            expect(manifest["capabilities"]?.requiredText("syntaxApi") == expected, errors) {
                "Manifest syntaxApi does not match profile: expected=$expected"
            }
        }
        expectedMinecraftVersion?.let { expected ->
            val actual = manifest["server"]?.requiredText("minecraftVersion")
            expect(actual == expected, errors) {
                "Manifest Minecraft version does not match profile: expected=$expected, actual=$actual"
            }
        }
        expectedSkriptVersion?.let { expected ->
            val actual = manifest["plugins"]
                ?.firstOrNull { it.requiredText("name") == "Skript" }
                ?.requiredText("version")
            expect(actual == expected, errors) {
                "Manifest Skript version does not match profile: expected=$expected, actual=$actual"
            }
        }
    }
    private inline fun <reified T : Enum<T>> enumValue(
        value: String,
        serializedName: (T) -> String
    ): T? = enumValues<T>().firstOrNull { serializedName(it) == value }

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

    private fun validateEventValueMetadata(
        eventValues: JsonNode,
        expected: String?,
        errors: MutableList<String>
    ) {
        if (expected == null) return

        eventValues.forEachIndexed { index, eventValue ->
            if (expected != "legacy-static") {
                expect(eventValue["registrationOrder"]?.isIntegralNumber == true, errors) {
                    "EventValues.json[$index] is missing registrationOrder"
                }
            }
            when (expected) {
                "legacy", "legacy-static" -> {
                    expect(!eventValue.has("patterns"), errors) {
                        "EventValues.json[$index] unexpectedly contains modern patterns"
                    }
                    expect(!eventValue.has("acceptedChangers"), errors) {
                        "EventValues.json[$index] unexpectedly contains modern acceptedChangers"
                    }
                    expect(!eventValue.has("contextDependent"), errors) {
                        "EventValues.json[$index] unexpectedly contains modern contextDependent"
                    }
                }
                "modern-2.15" -> {
                    expect(eventValue["patterns"]?.isArray == true, errors) {
                        "EventValues.json[$index] is missing modern patterns"
                    }
                    expect(eventValue["acceptedChangers"]?.isObject == true, errors) {
                        "EventValues.json[$index] is missing modern acceptedChangers"
                    }
                    expect(!eventValue.has("contextDependent"), errors) {
                        "EventValues.json[$index] contains 2.16-only contextDependent"
                    }
                }
                "modern-2.16" -> {
                    expect(eventValue["patterns"]?.isArray == true, errors) {
                        "EventValues.json[$index] is missing modern patterns"
                    }
                    expect(eventValue["acceptedChangers"]?.isObject == true, errors) {
                        "EventValues.json[$index] is missing modern acceptedChangers"
                    }
                    expect(eventValue["contextDependent"]?.isBoolean == true, errors) {
                        "EventValues.json[$index] is missing modern contextDependent"
                    }
                }
                else -> errors += "Unknown EventValue metadata expectation: $expected"
            }
        }
    }
    private fun validateAliases(aliases: JsonNode, errors: MutableList<String>) {
        val aliasMap = aliases["aliases"]
        val targets = aliases["targets"]
        expect(aliasMap?.isObject == true, errors) { "Aliases.json aliases must be an object" }
        expect(targets?.isArray == true, errors) { "Aliases.json targets must be an array" }
        if (aliasMap?.isObject != true || targets?.isArray != true) return

        val names = aliasMap.fieldNames().asSequence().toList()
        expect(names == names.sorted(), errors) { "Aliases.json names are not sorted" }
        val referencedTargets = mutableSetOf<Int>()
        names.forEach { name ->
            expect(name.isNotBlank(), errors) { "Aliases.json contains a blank alias name" }
            val index = aliasMap[name]
            expect(index?.isIntegralNumber == true, errors) {
                "Alias '$name' target index must be an integer"
            }
            if (index?.isIntegralNumber != true) return@forEach
            val targetIndex = index.asInt()
            expect(targetIndex in 0 until targets.size(), errors) {
                "Alias '$name' target index is out of bounds"
            }
            if (targetIndex in 0 until targets.size()) referencedTargets += targetIndex
        }
        expect(referencedTargets == (0 until targets.size()).toSet(), errors) {
            "Aliases.json contains unreferenced targets"
        }

        targets.forEachIndexed { targetIndex, target ->
            expect(target.isObject, errors) { "Alias target[$targetIndex] must be an object" }
            if (!target.isObject) return@forEachIndexed
            expect(target["amount"]?.isIntegralNumber == true, errors) {
                "Alias target[$targetIndex] has no integer amount"
            }
            expect(target["all"]?.isBoolean == true, errors) {
                "Alias target[$targetIndex] has no boolean all flag"
            }
            val types = target["types"]
            expect(types?.isArray == true, errors) {
                "Alias target[$targetIndex] types must be an array"
            }
            types?.takeIf(JsonNode::isArray)?.forEachIndexed { index, item ->
                expect(item["material"]?.asText()?.isNotBlank() == true, errors) {
                    "Alias target[$targetIndex] types[$index] has no material"
                }
                expect(item["durability"]?.isIntegralNumber == true, errors) {
                    "Alias target[$targetIndex] types[$index] has no integer durability"
                }
                expect(item["plain"]?.isBoolean == true, errors) {
                    "Alias target[$targetIndex] types[$index] has no boolean plain flag"
                }
                expect(item["alias"]?.isBoolean == true, errors) {
                    "Alias target[$targetIndex] types[$index] has no boolean alias flag"
                }
                expect(item["blockValues"] == null || item["blockValues"].isObject, errors) {
                    "Alias target[$targetIndex] types[$index] blockValues must be an object"
                }
                expect(item["itemMeta"] == null || item["itemMeta"].isObject, errors) {
                    "Alias target[$targetIndex] types[$index] itemMeta must be an object"
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
        require(args.size in 1..6) {
            "Usage: SnapshotValidatorMain <snapshot-directory> [event-value-api] [syntax-api] " +
                "[minecraft-version] [skript-version] [non-empty-files]"
        }
        val report = SnapshotValidator.validate(
            Path.of(args[0]),
            args.getOrNull(1),
            args.getOrNull(2),
            args.getOrNull(3),
            args.getOrNull(4),
            args.getOrNull(5)
                ?.split(",")
                ?.filter(String::isNotBlank)
                ?.toSet()
                .orEmpty()
        )
        println(
            "Validated ${report.files} files, ${report.aliases} aliases, ${report.registrations} registrations, " +
                "${report.types} types, ${report.eventValues} event values, and ${report.classes} classes."
        )
    }
}
