package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ComparatorData
import org.skriptlang.skript.lang.comparator.ComparatorInfo
import org.skriptlang.skript.lang.comparator.Comparators

class ComparatorCollector : SyntaxCollector<List<ComparatorData>> {
    override val fileName: String = "comparators.json"

    override fun collect(): List<ComparatorData> = Comparators.getComparatorInfos()
        .map { ComparatorData(it) }
}
