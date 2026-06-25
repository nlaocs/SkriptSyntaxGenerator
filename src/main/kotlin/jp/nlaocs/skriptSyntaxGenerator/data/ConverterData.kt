package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterConverterCollector
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.lang.converter.ConverterInfo

class ConverterData(
    converterInfo: ConverterInfo<*, *>
) : Addon {
    val from: Class<*> = converterInfo.from
    val to: Class<*> = converterInfo.to
    val flags: Int = converterInfo.flags
    val converterClass: Class<*> = converterInfo.converter.javaClass

    @Transient
    val snapshot = RegisterConverterCollector.getInstance()
        .snapshotMap()[RegisterConverterCollector.Key(from, to, flags)]

    override val addon: AddonInfo = snapshot?.takeIf { it.addonName != null && it.addonVersion != null }
        ?.let { AddonInfo(it.addonName, it.addonVersion) }
        ?: resolveAddonFromClasses()
        ?: run {
            Bukkit.getLogger()
                .warning("Converter $converterInfo($from -> $to, flags=$flags) does not have addon information.")
            AddonInfo("unknown", "unknown")
        }

    private fun resolveAddonFromClasses(): AddonInfo? {
        val candidates = listOf(converterClass, from, to)
        for (candidate in candidates) {
            try {
                val plugin = JavaPlugin.getProvidingPlugin(candidate)
                return AddonInfo(plugin.name, plugin.description.version)
            } catch (_: IllegalArgumentException) {
            }
        }
        return null
    }
}



