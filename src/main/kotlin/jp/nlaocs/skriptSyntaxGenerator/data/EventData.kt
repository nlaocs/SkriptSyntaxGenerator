package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.registrations.EventValues
import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventValueCollector
import jp.nlaocs.skriptSyntaxGenerator.util.AddonResolver
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos

class EventData(
    s: BukkitSyntaxInfos.Event<*>,
    registrationOrder: Int,
    registrationOccurrence: Int,
    addonOverride: AddonInfo? = null,
    allEventValues: List<EventValueData>
) : CommonSyntaxData(s, registrationOrder, registrationOccurrence, addonOverride) {
    val referenceEvents: List<Class<out Event>> = s.events().toList()
    val eventValues: List<EventValueData> = allEventValues
        .filter { it.isAvailableFor(referenceEvents) }
        .distinctBy { it.registrationId }
    val cancellable: Boolean = referenceEvents
        .all { Cancellable::class.java.isAssignableFrom(it) }
    val hasOnPrefix: Boolean = s.name().startsWith("On ")
}

data class EventValueData(
    val eventClass: Class<out Event>,
    val valueClass: Class<*>,
    val time: Int,
    val excludeErrorMessage: String?,
    val excludes: List<Class<*>>?,
    val resolutionOrder: Int,
    val registrationOrder: Int?,
    override val addon: AddonInfo,
) : Addon {
    val registrationId: String = StableIds.record(
        "event-value",
        addon,
        eventClass.stableName(),
        valueClass.stableName(),
        time.toString()
    )

    constructor(info: EventValues.EventValueInfo<*, *>, resolutionOrder: Int) : this(
        eventClass = info.eventClass(),
        valueClass = info.valueClass(),
        time = info.time(),
        excludeErrorMessage = info.excludeErrorMessage(),
        excludes = info.excludes()?.filterNotNull()?.map { it as Class<*> },
        resolutionOrder = resolutionOrder,
        registrationOrder = snapshotFor(info)?.registrationOrder(),
        addon = resolveAddon(info)
    )

    fun isAvailableFor(referenceEvents: List<Class<out Event>>): Boolean =
        referenceEvents.any { referenceEvent ->
            eventClass.isAssignableFrom(referenceEvent) &&
                excludes?.none { excluded -> excluded.isAssignableFrom(referenceEvent) } != false
        }

    companion object {
        fun collectAll(): List<EventValueData> {
            var resolutionOrder = 0
            return EventValues.getTimeStates().flatMap { time ->
                EventValues.getEventValuesListForTime(time).map { info ->
                    EventValueData(info, resolutionOrder++)
                }
            }
        }

        private fun snapshotFor(info: EventValues.EventValueInfo<*, *>) =
            RegisterEventValueCollector.getInstance()
                .snapshotFor(info.eventClass(), info.valueClass(), info.time())

        private fun resolveAddon(info: EventValues.EventValueInfo<*, *>): AddonInfo {
            snapshotFor(info)?.addon()?.let { return it }
            AddonResolver.fromClass(info.converter().javaClass)?.let { return it }

            Bukkit.getLogger().warning(
                "EventValue ${info.eventClass().name} -> ${info.valueClass().name} does not have addon information."
            )
            return AddonInfo("unknown", "unknown")
        }
    }
}
