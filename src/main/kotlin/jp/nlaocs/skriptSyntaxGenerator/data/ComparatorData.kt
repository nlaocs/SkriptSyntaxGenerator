package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterComparatorCollector
import org.skriptlang.skript.lang.comparator.ComparatorInfo

class ComparatorData(
    comparator: ComparatorInfo<*, *>
) {
    val firstType: Class<*> = comparator.firstType
    val secondType: Class<*> = comparator.secondType

    @Transient
    val snapshot = RegisterComparatorCollector.getInstance()
        .snapshotMap()[comparator.comparator]

    val supportsOrdering: Boolean? = snapshot?.supportsOrdering
    val supportsInversion: Boolean? = snapshot?.supportsInversion
    val addon: AddonInfo? =
        if (snapshot?.addonName != null && snapshot.addonVersion != null) {
            AddonInfo(snapshot.addonName, snapshot.addonVersion)
        } else {
            null
        }
}
