package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterComparatorCollector
import org.skriptlang.skript.lang.comparator.Comparator
import org.skriptlang.skript.lang.comparator.ComparatorInfo
import org.skriptlang.skript.lang.comparator.Comparators

class ComparatorData(
    comparator: ComparatorInfo<*, *>
) {
    val firstType: Class<*> = comparator.firstType
    val secondType: Class<*> = comparator.secondType

    //    data class ComparatorData(
//        val supportsOrdering: Boolean,
//        val supportsInversion: Boolean
//    ) {
//        constructor(s: Comparator<*, *>) : this(
//            supportsOrdering = s.supportsOrdering(),
//            supportsInversion = s.supportsInversion()
//        )
//    }
    @Transient
    val snapshot = RegisterComparatorCollector.getInstance()
        .snapshotMap()[comparator.comparator]

    val supportsOrdering: Boolean? = snapshot?.supportsOrdering
    val supportsInversion: Boolean? = snapshot?.supportsInversion
}
