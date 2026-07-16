package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.DifferenceData
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterDifferenceCollector

class ArithmeticDifferenceCollector : SyntaxCollector<List<DifferenceData>> {
    override val fileName: String = "Differences.json"

    override fun collect(): List<DifferenceData> =
        RegisterDifferenceCollector.getInstance()
            .snapshotMap()
            .entries
            .map { (key, snapshot) -> DifferenceData(key, snapshot) }
            .sortedBy { it.registrationOrder }
}
