package jp.nlaocs.skriptSyntaxGenerator.generator

import jp.nlaocs.skriptSyntaxGenerator.data.PluralAlgorithm
import jp.nlaocs.skriptSyntaxGenerator.data.PluralOverrideRegistration
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleAddonData
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleOrigin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PluralRulesReaderTest {
    private val skript = PluralRuleAddonData("Skript", "test")
    private val addon = PluralRuleAddonData("FixtureAddon", "1.0")

    @Test
    fun `reads legacy pair arrays without complete word metadata`() {
        val data = PluralRulesReader.readRaw(
            arrayOf(arrayOf("child", "children"), arrayOf("", "s")),
            false,
            skript,
            emptyList()
        )

        assertEquals(PluralAlgorithm.LEGACY_FIRST_MATCH, data.algorithm)
        assertEquals(false, data.isPluralOverrideSupported)
        assertEquals(listOf(0, 1), data.rules.map { it.ruleOrder })
        assertEquals(listOf(skript, skript), data.rules.map { it.addon })
        assertNull(data.rules.first().completeWord)
    }

    @Test
    fun `attributes addFirst overrides in effective runtime order`() {
        val overrides = listOf(
            PluralOverrideRegistration("first", "firsts", 0, addon),
            PluralOverrideRegistration("second", "seconds", 1, addon)
        )
        val data = PluralRulesReader.readRaw(
            listOf(
                FakeWordEnding("second", "seconds", true),
                FakeWordEnding("first", "firsts", true),
                FakeWordEnding("", "s", false)
            ),
            true,
            skript,
            overrides
        )

        assertEquals(PluralAlgorithm.SINGULAR_AWARE, data.algorithm)
        assertEquals(listOf(PluralRuleOrigin.OVERRIDE, PluralRuleOrigin.OVERRIDE, PluralRuleOrigin.BUILT_IN), data.rules.map { it.origin })
        assertEquals(listOf(1, 0, null), data.rules.map { it.overrideRegistrationOrder })
        assertEquals(listOf(addon, addon, skript), data.rules.map { it.addon })
        assertEquals(listOf(true, true, false), data.rules.map { it.completeWord })
    }

    @Test
    fun `rejects a captured override that does not match the runtime table`() {
        assertThrows(IllegalStateException::class.java) {
            PluralRulesReader.readRaw(
                listOf(FakeWordEnding("actual", "actuals", true)),
                true,
                skript,
                listOf(PluralOverrideRegistration("captured", "captureds", 0, addon))
            )
        }
    }

    private data class FakeWordEnding(
        private val singular: String,
        private val plural: String,
        private val completeWord: Boolean
    ) {
        fun singular(): String = singular
        fun plural(): String = plural
        fun isCompleteWord(): Boolean = completeWord
    }
}
