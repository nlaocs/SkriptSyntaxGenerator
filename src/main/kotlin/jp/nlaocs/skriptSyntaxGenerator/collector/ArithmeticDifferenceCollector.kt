package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.registrations.Classes
import jp.nlaocs.skriptSyntaxGenerator.data.DifferenceData
import org.skriptlang.skript.lang.arithmetic.Arithmetics

class ArithmeticDifferenceCollector : SyntaxCollector<Map<Class<*>, DifferenceData>> {
    override val fileName: String = "Differences.json"

    override fun collect(): Map<Class<*>, DifferenceData> =
        Classes.getClassInfos()
            .mapNotNull { info ->
                Arithmetics.getDifferenceInfo(info.c)?.let { info.c to DifferenceData(it) }
            }
            .toMap()
}
