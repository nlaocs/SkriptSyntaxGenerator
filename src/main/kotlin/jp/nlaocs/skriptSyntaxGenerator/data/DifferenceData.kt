package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterDifferenceCollector
import org.bukkit.Bukkit
import org.skriptlang.skript.lang.arithmetic.DifferenceInfo

data class DifferenceData(
    val type: Class<*>,
    val returnType: Class<*>,
) : Addon {
    @Transient
    val snapshot = RegisterDifferenceCollector.getInstance()
        .snapshotMap()[RegisterDifferenceCollector.Key(type, returnType)]

    override val addon: AddonInfo = if (snapshot?.addonName != null && snapshot.addonVersion != null) {
        AddonInfo(snapshot.addonName, snapshot.addonVersion)
    } else {
        Bukkit.getLogger()
            .warning("Difference $type($returnType) does not have addon information.")
        AddonInfo("unknown", "unknown")
    }

    constructor(difference: DifferenceInfo<*, *>) : this(
        type = difference.type(),
        returnType = difference.returnType(),
    )
}
