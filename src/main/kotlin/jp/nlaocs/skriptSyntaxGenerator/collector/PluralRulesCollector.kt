package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.Skript
import jp.nlaocs.skriptSyntaxGenerator.data.PluralOverrideRegistration
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleAddonData
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRulesData
import jp.nlaocs.skriptSyntaxGenerator.generator.PluralRulesReader
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterPluralOverrideCollector
import org.bukkit.Bukkit

class PluralRulesCollector : SyntaxCollector<PluralRulesData> {
    override val fileName: String = "PluralRules.json"

    override fun collect(): PluralRulesData {
        val skript = requireNotNull(Bukkit.getPluginManager().getPlugin("Skript")) {
            "Skript plugin is not installed"
        }
        val overrides = RegisterPluralOverrideCollector.getInstance().overrides.map { registration ->
            PluralOverrideRegistration(
                registration.singular(),
                registration.plural(),
                registration.registrationOrder(),
                registration.addonName()?.let { name ->
                    registration.addonVersion()?.let { version ->
                        PluralRuleAddonData(name, version)
                    }
                }
            )
        }

        return PluralRulesReader.read(
            Skript::class.java.classLoader,
            PluralRuleAddonData(skript.name, skript.description.version),
            overrides
        )
    }
}
