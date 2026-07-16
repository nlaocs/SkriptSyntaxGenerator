package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ConverterData
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterConverterCollector

class ConverterCollector : SyntaxCollector<List<ConverterData>> {
    override val fileName: String = "Converters.json"

    override fun collect(): List<ConverterData> =
        RegisterConverterCollector.getInstance()
            .snapshotMap()
            .entries
            .map { (key, snapshot) -> ConverterData(key, snapshot) }
            .sortedBy { it.registrationOrder }
}