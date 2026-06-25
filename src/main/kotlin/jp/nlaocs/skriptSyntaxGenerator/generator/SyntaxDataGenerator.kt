package jp.nlaocs.skriptSyntaxGenerator.generator

import ch.njol.skript.Skript
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticDifferenceCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticOperationCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticOperatorCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ComparatorCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ConditionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ConverterCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.EffectCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.EventCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.ExpressionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.FunctionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.SectionCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.StructureCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.SyntaxCollector
import jp.nlaocs.skriptSyntaxGenerator.collector.TypeCollector
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import jp.nlaocs.skriptSyntaxGenerator.util.FileUtils

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
    )

    fun generate() {
        collectors.forEach { collector ->
            val data = collector.collect()
            FileUtils.writeStringToFile(collector.fileName, objectMapper.writeValueAsString(data))
        }
    }
}
