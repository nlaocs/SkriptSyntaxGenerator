package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EffectData
import org.skriptlang.skript.registration.SyntaxRegistry

class EffectCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<EffectData>> {
    override val fileName = "Effects.json"

    override fun collect(): List<EffectData> =
        registry.syntaxes(SyntaxRegistry.EFFECT)
            .map { EffectData(it) }
}
