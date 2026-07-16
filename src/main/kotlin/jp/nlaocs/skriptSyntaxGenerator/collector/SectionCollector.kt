package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.SectionData
import org.skriptlang.skript.registration.SyntaxRegistry

class SectionCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<SectionData>> {
    override val fileName = "Sections.json"

    override fun collect(): List<SectionData> {
        val occurrences = SyntaxOccurrenceTracker()
        return registry.syntaxes(SyntaxRegistry.SECTION)
            .mapIndexed { index, info -> SectionData(info, index, occurrences.next(info)) }
    }
}