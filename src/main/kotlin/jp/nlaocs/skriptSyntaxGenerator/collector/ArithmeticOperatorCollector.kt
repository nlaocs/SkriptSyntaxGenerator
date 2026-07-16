package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.OperatorData
import org.skriptlang.skript.lang.arithmetic.Arithmetics

class ArithmeticOperatorCollector : SyntaxCollector<List<OperatorData>> {
    override val fileName: String = "Operators.json"

    override fun collect(): List<OperatorData> =
        Arithmetics.getAllOperators()
            .mapIndexed { index, operator -> OperatorData(operator, index) }
}