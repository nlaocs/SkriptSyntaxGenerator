package jp.nlaocs.skriptSyntaxGenerator.collector

import org.skriptlang.skript.lang.comparator.ComparatorInfo
import org.skriptlang.skript.lang.comparator.Comparators

class ComparatorCollector : SyntaxCollector<List<ComparatorInfo<*, *>>> {
    override val fileName: String = "comparators.json"

    override fun collect(): List<ComparatorInfo<*, *>> = Comparators.getComparatorInfos()
    // todo ComparatorInfo.comparatorを除外
}
