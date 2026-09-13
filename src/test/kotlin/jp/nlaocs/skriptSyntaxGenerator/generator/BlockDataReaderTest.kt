package jp.nlaocs.skriptSyntaxGenerator.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockDataReaderTest {
    @Test
    fun `collects sorted runtime block properties without hardcoded states`() {
        val data = BlockDataReader.read(
            FakeBukkit::class.java,
            FakeMaterial::class.java,
            FakeBlockData::class.java,
        )

        assertEquals("collected", data.state)
        assertTrue(data.isComplete)
        assertEquals("bukkit-runtime-registry", data.registryProvider)
        assertEquals(listOf("minecraft:chest", "minecraft:stone"), data.blocks.keys.toList())
        assertEquals(
            listOf("east", "north", "south", "west"),
            data.blocks.getValue("minecraft:chest").properties.getValue("facing"),
        )
        assertEquals("minecraft:chest[facing=north]", data.blocks.getValue("minecraft:chest").defaultState)
        assertTrue(data.failures.isEmpty())
    }

    @Test
    fun `retains partial runtime failures instead of claiming completeness`() {
        FakeBukkit.failStone = true
        try {
            val data = BlockDataReader.read(
                FakeBukkit::class.java,
                FakeMaterial::class.java,
                FakeBlockData::class.java,
            )

            assertEquals("collected", data.state)
            assertFalse(data.isComplete)
            assertEquals(setOf("minecraft:chest"), data.blocks.keys)
            assertEquals("minecraft:stone", data.failures.single().block)
        } finally {
            FakeBukkit.failStone = false
        }
    }

    private enum class FakeMaterial(
        private val block: Boolean,
        private val legacy: Boolean = false,
    ) {
        CHEST(true),
        LEGACY_STONE(true, legacy = true),
        STONE(true),
        STICK(false);

        fun isBlock(): Boolean = block
        fun isLegacy(): Boolean = legacy
        fun getKey(): FakeKey {
            check(!legacy) { "Cannot get key of Legacy Material" }
            return FakeKey("minecraft:${name.lowercase()}")
        }
    }

    private data class FakeKey(private val value: String) {
        override fun toString(): String = value
    }

    private class FakeProperty(
        private val name: String,
        private val values: List<String>,
    ) {
        fun getName(): String = name
        fun getPossibleValues(): Collection<String> = values
        fun getName(value: String): String = value
    }

    private class FakeState(private val values: Map<FakeProperty, String>) {
        fun getValues(): Map<FakeProperty, String> = values
    }

    private class FakeBlockData(private val id: String) {
        fun getState(): FakeState = if (id.endsWith("chest")) {
            FakeState(mapOf(FakeProperty("facing", listOf("south", "north", "west", "east")) to "north"))
        } else {
            FakeState(emptyMap())
        }

        fun getAsString(): String = if (id.endsWith("chest")) "$id[facing=north]" else id
    }

    private class FakeBukkit {
        companion object {
            var failStone = false

            @JvmStatic
            fun createBlockData(id: String): FakeBlockData {
                if (failStone && id.endsWith("stone")) error("broken stone")
                return FakeBlockData(id)
            }
        }
    }
}
