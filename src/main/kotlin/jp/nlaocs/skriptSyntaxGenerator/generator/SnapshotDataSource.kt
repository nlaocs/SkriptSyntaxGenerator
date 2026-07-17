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
import jp.nlaocs.skriptSyntaxGenerator.data.AliasesCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.EventValueApi
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotCapabilitiesData
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxApi
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxKindCapabilitiesData

interface SnapshotDataSource {
    val capabilities: SnapshotCapabilitiesData

    fun collectOutputs(): Map<String, Any>
}

class ModernSnapshotDataSource : SnapshotDataSource {
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
        PropertyCollector()
    )

    override val capabilities: SnapshotCapabilitiesData by lazy {
        ModernCapabilities.detect(javaClass.classLoader)
    }

    override fun collectOutputs(): Map<String, Any> {
        val outputs = linkedMapOf<String, Any>()
        collectors.forEach { collector ->
            outputs[collector.fileName] = requireNotNull(collector.collect())
        }
        outputs["ClassHierarchy.json"] = ClassHierarchyCollector().collect(outputs.values)
        outputs[SnapshotFormat.ALIASES_FILE] = GlobalAliasesReader.read(javaClass.classLoader)
        return outputs
    }
}

private object ModernCapabilities {
    private const val MODERN_EVENT_VALUE =
        "org.skriptlang.skript.bukkit.lang.eventvalue.EventValue"

    fun detect(classLoader: ClassLoader): SnapshotCapabilitiesData {
        val eventValueClass = classOrNull(MODERN_EVENT_VALUE, classLoader)
        val eventValueApi = when {
            eventValueClass == null -> EventValueApi.LEGACY
            eventValueClass.methods.any { it.name == "contextDependent" && it.parameterCount == 0 } ->
                EventValueApi.MODERN_2_16
            else -> EventValueApi.MODERN_2_15
        }
        val aliasesSupported = GlobalAliasesReader.isSupported(classLoader)

        return SnapshotCapabilitiesData(
            SyntaxApi.REGISTRY,
            eventValueApi,
            SyntaxKindCapabilitiesData.modern(),
            AliasesCapabilitiesData(aliasesSupported, aliasesSupported)
        )
    }

    private fun classOrNull(name: String, classLoader: ClassLoader): Class<*>? =
        runCatching { Class.forName(name, false, classLoader) }.getOrNull()
}
