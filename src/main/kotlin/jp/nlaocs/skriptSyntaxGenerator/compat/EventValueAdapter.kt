package jp.nlaocs.skriptSyntaxGenerator.compat

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer.ChangeMode
import ch.njol.skript.registrations.EventValues
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterEventValueCollector
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterModernEventValueCollector
import jp.nlaocs.skriptSyntaxGenerator.util.AddonResolver
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.skriptlang.skript.Skript as ModernSkript
import java.lang.reflect.Method

data class EventValueRecord(
    val eventClass: Class<out Event>,
    val valueClass: Class<*>,
    val time: Int,
    val excludeErrorMessage: String?,
    val excludes: List<Class<*>>?,
    val patterns: List<String>?,
    val acceptedChangers: Map<ChangeMode, List<Class<*>>>?,
    val contextDependent: Boolean?,
    val hasCustomInputValidator: Boolean?,
    val hasCustomEventValidator: Boolean?,
    val registrationOrder: Int?,
    val addon: AddonInfo
)

interface EventValueAdapter {
    fun collect(): List<EventValueRecord>
}

object EventValueAdapters {
    private const val MODERN_REGISTRY_CLASS =
        "org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry"

    fun collect(): List<EventValueRecord> {
        if (!isClassPresent(MODERN_REGISTRY_CLASS)) {
            return LegacyEventValueAdapter.collect()
        }

        return runCatching { ModernEventValueAdapter.collect() }
            .getOrElse { cause ->
                Bukkit.getLogger().warning(
                    "Modern EventValue collection failed; using the legacy compatibility API. " +
                        cause.javaClass.simpleName + ": " + cause.message
                )
                LegacyEventValueAdapter.collect()
            }
    }

    private fun isClassPresent(name: String): Boolean = runCatching {
        Class.forName(name, false, EventValueAdapters::class.java.classLoader)
    }.isSuccess
}

private object LegacyEventValueAdapter : EventValueAdapter {
    override fun collect(): List<EventValueRecord> =
        EventValues.getTimeStates().flatMap { time ->
            EventValues.getEventValuesListForTime(time).map(::toRecord)
        }

    private fun toRecord(info: EventValues.EventValueInfo<*, *>): EventValueRecord {
        val snapshot = RegisterEventValueCollector.getInstance()
            .snapshotFor(info.eventClass(), info.valueClass(), info.time())
        val addon = snapshot?.addon()
            ?: AddonResolver.fromClass(info.converter().javaClass)
            ?: unresolvedAddon(info.eventClass(), info.valueClass())

        @Suppress("UNCHECKED_CAST")
        return EventValueRecord(
            eventClass = info.eventClass() as Class<out Event>,
            valueClass = info.valueClass(),
            time = info.time(),
            excludeErrorMessage = info.excludeErrorMessage(),
            excludes = info.excludes()?.filterNotNull()?.map { it as Class<*> },
            patterns = null,
            acceptedChangers = null,
            contextDependent = null,
            hasCustomInputValidator = null,
            hasCustomEventValidator = null,
            registrationOrder = snapshot?.registrationOrder(),
            addon = addon
        )
    }
}

private object ModernEventValueAdapter : EventValueAdapter {
    private const val REGISTRY_CLASS =
        "org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry"
    private const val EVENT_VALUE_CLASS =
        "org.skriptlang.skript.bukkit.lang.eventvalue.EventValue"

    override fun collect(): List<EventValueRecord> {
        val loader = EventValueAdapters::class.java.classLoader
        val registryClass = Class.forName(REGISTRY_CLASS, false, loader)
        val eventValueClass = Class.forName(EVENT_VALUE_CLASS, false, loader)
        val registry = ModernSkript::class.java
            .getMethod("registry", Class::class.java)
            .invoke(Skript.instance(), registryClass)
        val eventValues = registryClass.getMethod("elements").invoke(registry) as Collection<*>
        val access = ModernAccess(eventValueClass)

        return eventValues.filterNotNull().map { eventValue ->
            access.toRecord(eventValue)
        }
    }

    private class ModernAccess(eventValueClass: Class<*>) {
        private val eventClass: Method = eventValueClass.getMethod("eventClass")
        private val valueClass: Method = eventValueClass.getMethod("valueClass")
        private val time: Method = eventValueClass.getMethod("time")
        private val patterns: Method = eventValueClass.getMethod("patterns")
        private val hasChanger: Method = eventValueClass.getMethod("hasChanger", ChangeMode::class.java)
        private val excludedEvents: Method = eventValueClass.getMethod("excludedEvents")
        private val excludedErrorMessage: Method = eventValueClass.getMethod("excludedErrorMessage")
        private val contextDependent: Method? = eventValueClass.methods
            .firstOrNull { it.name == "contextDependent" && it.parameterCount == 0 }
        private val converter: Method = eventValueClass.getMethod("converter")

        fun toRecord(eventValue: Any): EventValueRecord {
            val eventClass = eventClass.invoke(eventValue) as Class<*>
            require(Event::class.java.isAssignableFrom(eventClass)) {
                "Modern EventValue eventClass is not a Bukkit Event: " + eventClass.name
            }
            val valueClass = valueClass.invoke(eventValue) as Class<*>
            val snapshot = RegisterModernEventValueCollector.getInstance().snapshotFor(eventValue)
            val addon = snapshot?.addon()
                ?: converter.invoke(eventValue)?.javaClass?.let(AddonResolver::fromClass)
                ?: unresolvedAddon(eventClass, valueClass)

            @Suppress("UNCHECKED_CAST")
            return EventValueRecord(
                eventClass = eventClass as Class<out Event>,
                valueClass = valueClass,
                time = timeValue(time.invoke(eventValue)),
                excludeErrorMessage = excludedErrorMessage.invoke(eventValue) as String?,
                excludes = (excludedEvents.invoke(eventValue) as Collection<*>)
                    .filterIsInstance<Class<*>>(),
                patterns = (patterns.invoke(eventValue) as Collection<*>)
                    .filterIsInstance<String>(),
                acceptedChangers = ChangeMode.entries
                    .filter { mode -> hasChanger.invoke(eventValue, mode) == true }
                    .associateWith { mode ->
                        if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
                            emptyList()
                        } else {
                            listOf(valueClass)
                        }
                    },
                contextDependent = contextDependent?.invoke(eventValue) as Boolean?,
                hasCustomInputValidator = booleanField(eventValue, "hasCustomInputValidator"),
                hasCustomEventValidator = objectField(eventValue, "eventValidator")?.let { true }
                    ?: fieldExists(eventValue, "eventValidator")?.let { false },
                registrationOrder = snapshot?.registrationOrder(),
                addon = addon
            )
        }

        private fun timeValue(value: Any): Int =
            value.javaClass.getMethod("value").invoke(value) as Int

        private fun booleanField(instance: Any, name: String): Boolean? =
            objectField(instance, name) as? Boolean

        private fun objectField(instance: Any, name: String): Any? = runCatching {
            instance.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(instance)
        }.getOrNull()

        private fun fieldExists(instance: Any, name: String): Boolean? = runCatching {
            instance.javaClass.getDeclaredField(name)
            true
        }.getOrNull()
    }
}

private fun unresolvedAddon(eventClass: Class<*>, valueClass: Class<*>): AddonInfo {
    Bukkit.getLogger().warning(
        "EventValue " + eventClass.name + " -> " + valueClass.name +
            " does not have addon information."
    )
    return AddonInfo("unknown", "unknown")
}
