package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ConditionData
import org.skriptlang.skript.registration.SyntaxRegistry

class ConditionCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<ConditionData>> {
    override val fileName = "Conditions.json"

    override fun collect(): List<ConditionData> {
        val occurrences = SyntaxOccurrenceTracker()
        return registry.syntaxes(SyntaxRegistry.CONDITION)
            .mapIndexed { index, info -> ConditionData(info, index, occurrences.next(info)) }
    }
}