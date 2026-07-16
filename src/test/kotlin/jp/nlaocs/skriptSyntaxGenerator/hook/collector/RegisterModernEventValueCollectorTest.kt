package jp.nlaocs.skriptSyntaxGenerator.hook.collector

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterModernEventValueCollectorTest {
    private val collector = RegisterModernEventValueCollector.getInstance()

    @BeforeEach
    fun clearBefore() {
        collector.clear()
    }

    @AfterEach
    fun clearAfter() {
        collector.clear()
    }

    @Test
    fun `modern event values are tracked by identity and first registration`() {
        val firstValue = Any()
        val secondValue = Any()
        val firstAddon = AddonInfo("First", "1.0")

        collector.add(RegisterModernEventValueCollector.Registration(firstValue, firstAddon))
        collector.add(
            RegisterModernEventValueCollector.Registration(
                firstValue,
                AddonInfo("Ignored", "1.0")
            )
        )
        collector.add(RegisterModernEventValueCollector.Registration(secondValue, firstAddon))

        assertEquals(2, collector.size())
        assertEquals(firstAddon, collector.snapshotFor(firstValue)?.addon())
        assertEquals(0, collector.snapshotFor(firstValue)?.registrationOrder())
        assertEquals(1, collector.snapshotFor(secondValue)?.registrationOrder())
    }
}
