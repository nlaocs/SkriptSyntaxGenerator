package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EventData
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry

class EventCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<EventData>> {
    override val fileName = "events.json"

    override fun collect(): List<EventData> =
        registry.syntaxes(BukkitSyntaxInfos.Event.KEY)
            .map { EventData(it) }
}
