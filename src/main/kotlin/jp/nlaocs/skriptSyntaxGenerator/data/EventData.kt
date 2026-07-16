package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer.ChangeMode
import jp.nlaocs.skriptSyntaxGenerator.compat.EventValueAdapters
import jp.nlaocs.skriptSyntaxGenerator.compat.EventValueRecord
import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
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
    val patterns: List<String>? = null,
    val acceptedChangers: Map<ChangeMode, List<Class<*>>>? = null,
    val contextDependent: Boolean? = null,
) : Addon {
    val registrationId: String = StableIds.record(
        "event-value",
        addon,
        *buildList {
            add(eventClass.stableName())
            add(valueClass.stableName())
            add(time.toString())
            if (!patterns.isNullOrEmpty()) addAll(patterns)
        }.toTypedArray()
    )

    constructor(record: EventValueRecord, resolutionOrder: Int) : this(
        eventClass = record.eventClass,
        valueClass = record.valueClass,
        time = record.time,
        excludeErrorMessage = record.excludeErrorMessage,
        excludes = record.excludes,
        resolutionOrder = resolutionOrder,
        registrationOrder = record.registrationOrder,
        addon = record.addon,
        patterns = record.patterns,
        acceptedChangers = record.acceptedChangers,
        contextDependent = record.contextDependent
    )

    fun isAvailableFor(referenceEvents: List<Class<out Event>>): Boolean =
        referenceEvents.any { referenceEvent ->
            eventClass.isAssignableFrom(referenceEvent) &&
                excludes?.none { excluded -> excluded.isAssignableFrom(referenceEvent) } != false
        }

    companion object {
        fun collectAll(): List<EventValueData> = EventValueAdapters.collect()
            .mapIndexed { resolutionOrder, record -> EventValueData(record, resolutionOrder) }
    }
}
