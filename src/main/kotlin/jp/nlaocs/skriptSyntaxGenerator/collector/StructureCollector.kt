package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.StructureData
import org.skriptlang.skript.registration.SyntaxRegistry

class StructureCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<StructureData>> {
    override val fileName = "structures.json"

    override fun collect(): List<StructureData> =
        registry.syntaxes(SyntaxRegistry.STRUCTURE)
            .map { StructureData(it) }
}
