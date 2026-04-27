package jp.nlaocs.skriptSyntaxGenerator.collector

import org.skriptlang.skript.lang.converter.ConverterInfo
import org.skriptlang.skript.lang.converter.Converters

class ConverterCollector : SyntaxCollector<List<ConverterInfo<*, *>>> {
    override val fileName: String = "Converters.json"

    override fun collect(): List<ConverterInfo<*, *>> = Converters.getConverterInfos()
    // todo ConverterInfo.converterを除外
}
