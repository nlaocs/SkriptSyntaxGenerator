package jp.nlaocs.skriptSyntaxGenerator.generator

import ch.njol.skript.Skript
import jp.nlaocs.skriptSyntaxGenerator.collector.ArithmeticCollector
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
import jp.nlaocs.skriptSyntaxGenerator.serializer.GsonFactory
import jp.nlaocs.skriptSyntaxGenerator.util.FileUtils

class SyntaxDataGenerator {
    private val gson = GsonFactory.create()
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
        ArithmeticCollector(),
        ConverterCollector(),
    )

    fun generate() {
        collectors.forEach { collector ->
            val data = collector.collect()
            FileUtils.writeStringToFile(collector.fileName, gson.toJson(data))
        }
    }
}
