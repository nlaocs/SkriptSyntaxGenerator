package jp.nlaocs.skriptSyntaxGenerator.hook.collector

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterPluralOverrideCollectorTest {
    private val collector = RegisterPluralOverrideCollector.getInstance()

    @BeforeEach
    fun clearBefore() {
        collector.clear()
    }

    @AfterEach
    fun clearAfter() {
        collector.clear()
    }

    @Test
    fun `accepts only the two string overload`() {
        assertTrue(collector.isValidArguments("person", "people"))
        assertFalse(collector.isValidArguments("person"))
        assertFalse(collector.isValidArguments("person", 2))
    }

    @Test
    fun `preserves duplicate overrides and resets registration order`() {
        collector.add(RegisterPluralOverrideCollector.Registration("person", "people", 0, "One", "1.0"))
        collector.add(RegisterPluralOverrideCollector.Registration("person", "people", 1, "Two", "2.0"))

        assertEquals(listOf(0, 1), collector.overrides.map { it.registrationOrder() })
        assertEquals(listOf("One", "Two"), collector.overrides.map { it.addonName() })

        collector.clear()
        assertTrue(collector.overrides.isEmpty())
    }
}
