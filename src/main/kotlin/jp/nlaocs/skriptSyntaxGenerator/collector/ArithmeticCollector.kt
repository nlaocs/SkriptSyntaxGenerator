package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ArithmeticData

class ArithmeticCollector : SyntaxCollector<ArithmeticData> {
    override val fileName: String = "Arithmetics.json"

    override fun collect(): ArithmeticData =
        ArithmeticData()
}
