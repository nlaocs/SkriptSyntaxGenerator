package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.EventData
import jp.nlaocs.skriptSyntaxGenerator.data.EventValueData
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventCollector
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry

class EventCollector(private val registry: SyntaxRegistry) : SyntaxCollector<List<EventData>> {
    override val fileName = "Events.json"

    override fun collect(): List<EventData> {
        val eventCollector = RegisterEventCollector.getInstance()
        val hookOccurrences = mutableMapOf<RegisterEventCollector.Key, Int>()
        val identityOccurrences = SyntaxOccurrenceTracker()
        val eventValues = EventValueData.collectAll()

        return registry.syntaxes(BukkitSyntaxInfos.Event.KEY)
            .mapIndexed { index, event ->
                val key = RegisterEventCollector.keyOf(
                    event.type(),
                    event.name(),
                    event.patterns(),
                    event.events()
                )
                val hookOccurrence = if (key != null) hookOccurrences.getOrDefault(key, 0) else 0
                if (key != null) {
                    hookOccurrences[key] = hookOccurrence + 1
                }

                EventData(
                    event,
                    registrationOrder = index,
                    registrationOccurrence = identityOccurrences.next(event),
                    addonOverride = key?.let { eventCollector.snapshotFor(it, hookOccurrence)?.addon() },
                    allEventValues = eventValues
                )
            }
    }
}
