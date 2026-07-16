package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterDifferenceCollector
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.stableName

data class DifferenceData(
    val type: Class<*>,
    val returnType: Class<*>,
    val registrationOrder: Int,
    override val addon: AddonInfo
) : Addon {
    val registrationId: String = StableIds.record(
        "difference",
        addon,
        type.stableName(),
        returnType.stableName()
    )

    constructor(
        key: RegisterDifferenceCollector.Key,
        snapshot: RegisterDifferenceCollector.Snapshot
    ) : this(
        type = key.type(),
        returnType = key.returnType(),
        registrationOrder = snapshot.registrationOrder(),
        addon = AddonInfo(
            requireNotNull(snapshot.addonName()) {
                "Direct difference ${key.type().name} -> ${key.returnType().name} has no addon name"
            },
            requireNotNull(snapshot.addonVersion()) {
                "Direct difference ${key.type().name} -> ${key.returnType().name} has no addon version"
            }
        )
    )
}
