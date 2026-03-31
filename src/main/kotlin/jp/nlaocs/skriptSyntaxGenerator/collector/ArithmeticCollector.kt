package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ArithmeticData

class ArithmeticCollector : SyntaxCollector<ArithmeticData> {
    override val fileName: String = "arithmetics.json"

    override fun collect(): ArithmeticData =
        ArithmeticData()
}
