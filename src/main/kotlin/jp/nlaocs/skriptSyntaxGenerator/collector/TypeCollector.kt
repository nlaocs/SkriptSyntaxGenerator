package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.registrations.Classes
import jp.nlaocs.skriptSyntaxGenerator.data.TypeData

class TypeCollector : SyntaxCollector<TypeData> {
    override val fileName = "types.json"

    override fun collect(): List<TypeData> =
        Classes.getClassInfos()
            .map { TypeData(it) }
}
