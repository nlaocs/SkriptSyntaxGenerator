package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EventValueData

class EventValueCollector : SyntaxCollector<List<EventValueData>> {
    override val fileName: String = "EventValues.json"

    override fun collect(): List<EventValueData> = EventValueData.collectAll()
}
