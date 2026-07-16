package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ExpressionData
import org.skriptlang.skript.registration.SyntaxRegistry

class ExpressionCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<ExpressionData>> {
    override val fileName = "Expressions.json"

    override fun collect(): List<ExpressionData> {
        val occurrences = SyntaxOccurrenceTracker()
        return registry.syntaxes(SyntaxRegistry.EXPRESSION)
            .mapIndexed { index, info -> ExpressionData(info, index, occurrences.next(info)) }
    }
}