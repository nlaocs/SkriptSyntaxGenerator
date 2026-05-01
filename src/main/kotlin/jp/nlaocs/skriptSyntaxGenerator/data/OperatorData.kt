package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterOperatorCollector
import org.bukkit.Bukkit
import org.skriptlang.skript.lang.arithmetic.Operator as SkriptOperator
import org.skriptlang.skript.util.Priority

data class OperatorData(
    val sign: String,
    val priority: Priority,
    val key: String?,
) : Addon {
    @Transient
    val snapshot = RegisterOperatorCollector.getInstance()
        .snapshotMap()[RegisterOperatorCollector.Key(sign, key)]

    override val addon: AddonInfo = if (snapshot?.addonName != null && snapshot.addonVersion != null) {
        AddonInfo(snapshot.addonName, snapshot.addonVersion)
    } else {
        Bukkit.getLogger()
            .warning("Operator $sign($key) does not have addon information.")
        AddonInfo("unknown", "unknown")
    }

    constructor(operator: SkriptOperator) : this(
        sign = operator.sign(),
        priority = operator.priority(),
        key = operator.node?.key,
    )
}
