package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.addon.SkriptAddon
import org.skriptlang.skript.docs.Origin

object AddonResolver {
    fun fromPlugin(plugin: Plugin): AddonInfo =
        AddonInfo(plugin.name, plugin.description.version)

    fun fromClass(type: Class<*>): AddonInfo? = runCatching {
        fromPlugin(JavaPlugin.getProvidingPlugin(type))
    }.getOrNull()

    fun fromSkriptAddon(addon: SkriptAddon): AddonInfo =
        Bukkit.getPluginManager().getPlugin(addon.name())?.let(::fromPlugin)
            ?: fromClass(addon.source())
            ?: AddonInfo(addon.name(), "unknown")

    fun fromOrigin(origin: Origin): AddonInfo? = when (origin) {
        is Origin.AddonOrigin -> fromSkriptAddon(origin.addon())
        else -> null
    }
}
