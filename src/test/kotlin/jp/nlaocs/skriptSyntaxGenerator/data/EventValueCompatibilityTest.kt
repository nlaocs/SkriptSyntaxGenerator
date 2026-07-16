package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer.ChangeMode
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import org.bukkit.event.player.PlayerJoinEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventValueCompatibilityTest {
    @Test
    fun `legacy ids stay stable while custom patterns distinguish registrations`() {
        val legacy = eventValue()
        val modernWithoutPatterns = eventValue(patterns = emptyList())
        val modernWithPattern = eventValue(patterns = listOf("joined player"))
        val modernWithOtherPattern = eventValue(patterns = listOf("departing player"))

        assertEquals(legacy.registrationId, modernWithoutPatterns.registrationId)
        assertNotEquals(legacy.registrationId, modernWithPattern.registrationId)
        assertNotEquals(modernWithPattern.registrationId, modernWithOtherPattern.registrationId)
    }

    @Test
    fun `modern empty metadata is emitted while unavailable legacy metadata is omitted`() {
        val mapper = JacksonFactory.create()
        val legacy = mapper.readTree(mapper.writeValueAsString(eventValue()))
        val modern = mapper.readTree(
            mapper.writeValueAsString(
                eventValue(
                    patterns = emptyList(),
                    acceptedChangers = emptyMap(),
                    contextDependent = false
                )
            )
        )

        assertFalse(legacy.has("patterns"))
        assertFalse(legacy.has("acceptedChangers"))
        assertFalse(legacy.has("contextDependent"))
        assertTrue(modern["patterns"].isArray)
        assertTrue(modern["patterns"].isEmpty)
        assertTrue(modern["acceptedChangers"].isObject)
        assertTrue(modern["acceptedChangers"].isEmpty)
        assertFalse(modern["contextDependent"].asBoolean())
    }

    private fun eventValue(
        patterns: List<String>? = null,
        acceptedChangers: Map<ChangeMode, List<Class<*>>>? = null,
        contextDependent: Boolean? = null
    ): EventValueData = EventValueData(
        eventClass = PlayerJoinEvent::class.java,
        valueClass = String::class.java,
        time = 0,
        excludeErrorMessage = null,
        excludes = emptyList(),
        resolutionOrder = 0,
        registrationOrder = 0,
        addon = AddonInfo("Test", "1.0"),
        patterns = patterns,
        acceptedChangers = acceptedChangers,
        contextDependent = contextDependent
    )
}
