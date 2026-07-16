package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EffectData
import org.skriptlang.skript.registration.SyntaxRegistry

class EffectCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<EffectData>> {
    override val fileName = "Effects.json"

    override fun collect(): List<EffectData> {
        val occurrences = SyntaxOccurrenceTracker()
        return registry.syntaxes(SyntaxRegistry.EFFECT)
            .mapIndexed { index, info -> EffectData(info, index, occurrences.next(info)) }
    }
}