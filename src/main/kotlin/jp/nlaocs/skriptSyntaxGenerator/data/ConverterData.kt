package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterConverterCollector
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.stableName

data class ConverterData(
    val from: Class<*>,
    val to: Class<*>,
    val flags: Int,
    val registrationOrder: Int,
    override val addon: AddonInfo
) : Addon {
    val registrationId: String = StableIds.record(
        "converter",
        addon,
        from.stableName(),
        to.stableName(),
        flags.toString()
    )

    constructor(
        key: RegisterConverterCollector.Key,
        snapshot: RegisterConverterCollector.Snapshot
    ) : this(
        from = key.from(),
        to = key.to(),
        flags = key.flags(),
        registrationOrder = snapshot.registrationOrder(),
        addon = AddonInfo(
            requireNotNull(snapshot.addonName()) {
                "Direct converter ${key.from().name} -> ${key.to().name} has no addon name"
            },
            requireNotNull(snapshot.addonVersion()) {
                "Direct converter ${key.from().name} -> ${key.to().name} has no addon version"
            }
        )
    )
}