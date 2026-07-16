package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.StructureData
import org.skriptlang.skript.registration.SyntaxRegistry

class StructureCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<StructureData>> {
    override val fileName = "Structures.json"

    override fun collect(): List<StructureData> {
        val occurrences = SyntaxOccurrenceTracker()
        return registry.syntaxes(SyntaxRegistry.STRUCTURE)
            .mapIndexed { index, info -> StructureData(info, index, occurrences.next(info)) }
    }
}