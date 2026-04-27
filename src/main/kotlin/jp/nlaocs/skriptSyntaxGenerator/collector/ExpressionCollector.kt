package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ExpressionData
import org.skriptlang.skript.registration.SyntaxRegistry

class ExpressionCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<ExpressionData>> {
    override val fileName = "Expressions.json"

    override fun collect(): List<ExpressionData> =
        registry.syntaxes(SyntaxRegistry.EXPRESSION)
            .map { ExpressionData(it) }
}
