package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.lang.function.Functions
import jp.nlaocs.skriptSyntaxGenerator.data.FunctionData

class FunctionCollector : SyntaxCollector<List<FunctionData>> {
    override val fileName: String = "functions.json"

    override fun collect(): List<FunctionData> =
        Functions.getFunctions()
            .map { FunctionData(it) }
}
