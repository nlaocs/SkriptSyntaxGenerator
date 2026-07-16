package jp.nlaocs.skriptSyntaxGenerator.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class JacksonFactoryTest {
    @Test
    fun `serializer preserves the snapshot json contract`() {
        val tree = JacksonFactory.create().valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            SerializationPayload(
                type = Array<String>::class.java,
                keyedTypes = mapOf(IntArray::class.java to "primitive array"),
                pattern = Pattern.compile("a+b"),
                emptyValues = emptyList(),
                missing = null
            )
        )

        assertEquals("java.lang.String[]", tree["type"].asText())
        assertEquals("primitive array", tree["keyedTypes"]["int[]"].asText())
        assertEquals("a+b", tree["pattern"].asText())
        assertTrue(tree["emptyValues"].isArray)
        assertEquals(0, tree["emptyValues"].size())
        assertFalse(tree.has("missing"))
    }
}

data class SerializationPayload(
    val type: Class<*>,
    val keyedTypes: Map<Class<*>, String>,
    val pattern: Pattern,
    val emptyValues: List<String>,
    val missing: String?
)
