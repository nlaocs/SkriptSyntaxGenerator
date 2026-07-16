package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterOperationCollector
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import org.bukkit.Bukkit
import org.skriptlang.skript.lang.arithmetic.OperationInfo

data class OperationData(
    val operatorSign: String,
    val left: Class<*>,
    val right: Class<*>,
    val returnType: Class<*>,
    val registrationOrder: Int,
) : Addon {
    @Transient
    val snapshot = RegisterOperationCollector.getInstance()
        .snapshotMap()[RegisterOperationCollector.Key(operatorSign, left, right, returnType)]

    override val addon: AddonInfo = if (snapshot?.addonName != null && snapshot.addonVersion != null) {
        AddonInfo(snapshot.addonName, snapshot.addonVersion)
    } else {
        Bukkit.getLogger()
            .warning("Operation $operatorSign($left, $right -> $returnType) does not have addon information.")
        AddonInfo("unknown", "unknown")
    }

    val registrationId: String = StableIds.record(
        "operation",
        addon,
        operatorSign,
        left.stableName(),
        right.stableName(),
        returnType.stableName()
    )

    constructor(operatorSign: String, operation: OperationInfo<*, *, *>, registrationOrder: Int) : this(
        operatorSign = operatorSign,
        left = operation.left(),
        right = operation.right(),
        returnType = operation.returnType(),
        registrationOrder = registrationOrder,
    )
}