package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ConverterData
import org.skriptlang.skript.lang.converter.Converters

class ConverterCollector : SyntaxCollector<List<ConverterData>> {
    override val fileName: String = "Converters.json"

    override fun collect(): List<ConverterData> = Converters.getConverterInfos().map { ConverterData(it) }
}
