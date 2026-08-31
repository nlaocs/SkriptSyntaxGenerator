package jp.nlaocs.skriptSyntaxGenerator.data

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockCanBuildEvent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventDataTest {
    @Test
    fun `listening behavior is supported when any referenced event is cancellable`() {
        assertTrue(
            supportsListeningBehavior(
                listOf(PlainEvent::class.java, CancellableEvent::class.java)
            )
        )
        assertFalse(supportsListeningBehavior(listOf(PlainEvent::class.java)))
        assertFalse(supportsListeningBehavior(emptyList()))
        assertTrue(supportsListeningBehavior(listOf(BlockCanBuildEvent::class.java)))
    }
}

private class PlainEvent : Event() {
    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
    }
}

private class CancellableEvent : Event(), Cancellable {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        private val HANDLERS = HandlerList()
    }
}
