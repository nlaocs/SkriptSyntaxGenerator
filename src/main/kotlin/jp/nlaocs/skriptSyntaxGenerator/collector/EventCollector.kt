package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EventData
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventCollector
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry

class EventCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<EventData>> {
    override val fileName = "Events.json"

    override fun collect(): List<EventData> {
        val eventCollector = RegisterEventCollector.getInstance()
        val occurrences = mutableMapOf<RegisterEventCollector.Key, Int>()

        return registry.syntaxes(BukkitSyntaxInfos.Event.KEY)
            .map { event ->
                val key = RegisterEventCollector.keyOf(
                    event.type(),
                    event.name(),
                    event.patterns(),
                    event.events()
                )
                val occurrence = if (key != null) occurrences.getOrDefault(key, 0) else 0
                if (key != null) {
                    occurrences[key] = occurrence + 1
                }

                EventData(event, key?.let { eventCollector.snapshotFor(it, occurrence)?.addon() })
            }
    }
}
