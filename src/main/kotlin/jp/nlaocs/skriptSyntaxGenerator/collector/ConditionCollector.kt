package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ConditionData
import org.skriptlang.skript.registration.SyntaxRegistry

class ConditionCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<ConditionData>> {
    override val fileName = "conditions.json"

    override fun collect(): List<ConditionData> =
        registry.syntaxes(SyntaxRegistry.CONDITION)
            .map { ConditionData(it) }
}
