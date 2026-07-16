package jp.nlaocs.skriptSyntaxGenerator.generator

import ch.njol.skript.Skript
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticDifferenceCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticOperationCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticOperatorCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ClassHierarchyCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ComparatorCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ConditionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ConverterCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.EffectCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.EventCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.EventValueCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ExpressionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.FunctionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.PropertyCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.SectionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.StructureCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.SyntaxCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.TypeCollector
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotManifestData
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import jp.nlaocs.skriptSyntaxGenerator.util.FileUtils
import jp.nlaocs.skriptSyntaxGenerator.util.SnapshotDigests

class SyntaxDataGenerator {
    private val objectMapper = JacksonFactory.create()
    private val registry = Skript.instance().syntaxRegistry()

    private val collectors: List<SyntaxCollector<*>> = listOf(
        EventCollector(registry),
        ConditionCollector(registry),
        EffectCollector(registry),
        ExpressionCollector(registry),
        TypeCollector(),
        FunctionCollector(),
        SectionCollector(registry),
        StructureCollector(registry),
        ArithmeticOperatorCollector(),
        ArithmeticOperationCollector(),
        ArithmeticDifferenceCollector(),
        ConverterCollector(),
        ComparatorCollector(),
        EventValueCollector(),
        PropertyCollector(),
    )

    fun generate() {
        val outputs = linkedMapOf<String, Any>()
        collectors.forEach { collector ->
            outputs[collector.fileName] = requireNotNull(collector.collect())
        }
        outputs["ClassHierarchy.json"] = ClassHierarchyCollector().collect(outputs.values)

        val serializedOutputs = outputs.mapValuesTo(linkedMapOf<String, String>()) { (_, data) ->
            objectMapper.writeValueAsString(data)
        }
        val contentDigest = SnapshotDigests.contentDigest(serializedOutputs)
        val manifest = SnapshotManifestData.create(
            files = serializedOutputs.keys + "Manifest.json",
            contentDigest = contentDigest
        )

        serializedOutputs.forEach { (fileName, json) ->
            FileUtils.writeStringToFile(fileName, json)
        }
        FileUtils.writeStringToFile(
            "Manifest.json",
            objectMapper.writeValueAsString(manifest)
        )
    }
}
