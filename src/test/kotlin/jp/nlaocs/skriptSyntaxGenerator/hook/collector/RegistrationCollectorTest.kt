package jp.nlaocs.skriptSyntaxGenerator.hook.collector

import ch.njol.skript.classes.Changer.ChangeMode
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skriptlang.skript.lang.arithmetic.Operation
import org.skriptlang.skript.lang.converter.Converter

class RegistrationCollectorTest {
    private val eventValues = RegisterEventValueCollector.getInstance()

    @BeforeEach
    fun clearBefore() {
        eventValues.clear()
    }

    @AfterEach
    fun clearAfter() {
        eventValues.clear()
    }

    @Test
    fun `converter and difference collectors accept only canonical overloads`() {
        val converter = Converter<String, Int> { it.length }
        val operation = Operation<Int, Int, Int> { left, right -> left + right }

        assertFalse(
            RegisterConverterCollector.getInstance()
                .isValidArguments(String::class.java, Int::class.javaObjectType, converter)
        )
        assertTrue(
            RegisterConverterCollector.getInstance()
                .isValidArguments(String::class.java, Int::class.javaObjectType, converter, 0)
        )

        assertFalse(
            RegisterDifferenceCollector.getInstance()
                .isValidArguments(Int::class.javaObjectType, operation)
        )
        assertTrue(
            RegisterDifferenceCollector.getInstance()
                .isValidArguments(Int::class.javaObjectType, Int::class.javaObjectType, operation)
        )
    }

    @Test
    fun `event value validation matches the canonical six argument overload`() {
        val converter = Converter<String, Int> { it.length }
        val valid = arrayOf<Any?>(
            PlayerJoinEvent::class.java,
            Int::class.javaObjectType,
            converter,
            0,
            null,
            arrayOf(PlayerQuitEvent::class.java)
        )

        assertTrue(eventValues.isValidArguments(*valid))
        assertFalse(eventValues.isValidArguments(*valid.copyOf(5)))
        valid[0] = String::class.java
        assertFalse(eventValues.isValidArguments(*valid))
    }

    @Test
    fun `event value registrations are deduplicated ordered and resettable`() {
        val firstAddon = AddonInfo("First", "1.0")
        val ignoredDuplicateAddon = AddonInfo("Second", "1.0")
        val first = RegisterEventValueCollector.Registration(
            PlayerJoinEvent::class.java,
            String::class.java,
            0,
            firstAddon
        )

        eventValues.add(first)
        eventValues.add(
            RegisterEventValueCollector.Registration(
                PlayerJoinEvent::class.java,
                String::class.java,
                0,
                ignoredDuplicateAddon
            )
        )
        eventValues.add(
            RegisterEventValueCollector.Registration(
                PlayerJoinEvent::class.java,
                Int::class.javaObjectType,
                0,
                firstAddon
            )
        )

        val firstSnapshot = eventValues.snapshotFor(PlayerJoinEvent::class.java, String::class.java, 0)
        val secondSnapshot = eventValues.snapshotFor(PlayerJoinEvent::class.java, Int::class.javaObjectType, 0)

        assertEquals(2, eventValues.snapshotMap().size)
        assertEquals(firstAddon, firstSnapshot?.addon())
        assertEquals(0, firstSnapshot?.registrationOrder())
        assertEquals(1, secondSnapshot?.registrationOrder())

        eventValues.clear()
        eventValues.add(first)
        assertEquals(
            0,
            eventValues.snapshotFor(PlayerJoinEvent::class.java, String::class.java, 0)?.registrationOrder()
        )
    }
}
