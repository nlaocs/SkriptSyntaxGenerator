package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.OperationData
import org.skriptlang.skript.lang.arithmetic.Arithmetics

class ArithmeticOperationCollector : SyntaxCollector<Map<String, List<OperationData>>> {
    override val fileName: String = "Operations.json"

    override fun collect(): Map<String, List<OperationData>> =
        Arithmetics.getAllOperators().associate { operator ->
            operator.sign() to Arithmetics.getOperations(operator).map { OperationData(it) }
        }
}
