package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.registrations.EventValues
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos

class EventData(s: BukkitSyntaxInfos.Event<*>, addonOverride: AddonInfo? = null) : CommonSyntaxData(s, addonOverride) {
    val referenceEvents: List<Class<out Event>> = s.events().toList() // todo 本当にnullableではないのか？
    val eventValues: List<EventValues.EventValueInfo<*, *>>? // todo nullableにすべきか
    val cancellable: Boolean = referenceEvents
        .all { Cancellable::class.java.isAssignableFrom(it) }
    val hasOnPrefix: Boolean = s.name().startsWith("On ") // Nameで判断しているのは、Skriptが自動追加しているため。

    init {
        val allEventValues = EventValues.getPerEventEventValues()
        val eventValueList = mutableListOf<EventValues.EventValueInfo<*, *>>()
        for ((eventClass, info) in allEventValues.entries()) {
            for (refEvent in referenceEvents) {
                if (eventClass.isAssignableFrom(refEvent)) {
                    eventValueList.add(info)
                }
            }

        }
        eventValues = eventValueList.ifEmpty { null }
    }
}
