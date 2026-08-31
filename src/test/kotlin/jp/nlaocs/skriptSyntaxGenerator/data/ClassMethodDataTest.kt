package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClassMethodDataTest {
    @Test
    fun `serializes the static method flag under the contract key`() {
        val json = JacksonFactory.create().readTree(
            JacksonFactory.create().writeValueAsString(
                ClassMethodData("call", listOf("int"), "java.lang.String", true)
            )
        )

        assertEquals("call", json["name"].asText())
        assertEquals(listOf("int"), json["parameterTypes"].map { it.asText() })
        assertEquals("java.lang.String", json["returnType"].asText())
        assertTrue(json["static"].asBoolean())
        assertFalse(json.has("isStatic"))
    }

    @Test
    fun `signature key includes exact return and static fields`() {
        val instance = ClassMethodData("call", emptyList(), "void", false)
        val static = ClassMethodData("call", emptyList(), "void", true)
        val otherReturn = ClassMethodData("call", emptyList(), "int", false)

        assertEquals(instance.signatureKey(), ClassMethodData("call", emptyList(), "void", false).signatureKey())
        assertFalse(instance.signatureKey() == static.signatureKey())
        assertFalse(instance.signatureKey() == otherReturn.signatureKey())
    }
}
