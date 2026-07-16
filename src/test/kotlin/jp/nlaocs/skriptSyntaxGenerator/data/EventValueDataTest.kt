package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventValueDataTest {
    @Test
    fun `event values apply through event inheritance`() {
        assertTrue(eventValue().isAvailableFor(listOf(PlayerJoinEvent::class.java)))
        assertFalse(eventValue().isAvailableFor(listOf(BlockBreakEvent::class.java)))
    }

    @Test
    fun `excluded subclasses are rejected without rejecting siblings`() {
        val value = eventValue(excludes = listOf(PlayerJoinEvent::class.java))

        assertFalse(value.isAvailableFor(listOf(PlayerJoinEvent::class.java)))
        assertTrue(value.isAvailableFor(listOf(PlayerQuitEvent::class.java)))
    }

    private fun eventValue(excludes: List<Class<*>>? = null): EventValueData = EventValueData(
        eventClass = PlayerEvent::class.java,
        valueClass = String::class.java,
        time = 0,
        excludeErrorMessage = null,
        excludes = excludes,
        resolutionOrder = 0,
        registrationOrder = 0,
        addon = AddonInfo("Test", "1.0")
    )
}
