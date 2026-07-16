package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.registrations.Classes
import jp.nlaocs.skriptSyntaxGenerator.data.TypeData

class TypeCollector : SyntaxCollector<List<TypeData>> {
    override val fileName = "Types.json"

    override fun collect(): List<TypeData> =
        Classes.getClassInfos()
            .mapIndexed { index, info -> TypeData(info, index) }
}