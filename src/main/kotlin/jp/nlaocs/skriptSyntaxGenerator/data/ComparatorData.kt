package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterComparatorCollector
import org.bukkit.Bukkit
import org.skriptlang.skript.lang.comparator.ComparatorInfo

class ComparatorData(
    comparator: ComparatorInfo<*, *>
) : Addon {
    val firstType: Class<*> = comparator.firstType
    val secondType: Class<*> = comparator.secondType

    @Transient
    val snapshot = RegisterComparatorCollector.getInstance()
        .snapshotMap()[RegisterComparatorCollector.Key(firstType, secondType, comparator.comparator)]

    val supportsOrdering: Boolean? = snapshot?.supportsOrdering
    val supportsInversion: Boolean? = snapshot?.supportsInversion
    override val addon: AddonInfo =
        if (snapshot?.addonName != null && snapshot.addonVersion != null) {
            AddonInfo(snapshot.addonName, snapshot.addonVersion)
        } else {
            Bukkit.getLogger()
                .warning("Comparator $comparator($firstType, $secondType) does not have addon information.")
            AddonInfo("unknown", "unknown")
        } // todo 実装が汚い気がする
}
