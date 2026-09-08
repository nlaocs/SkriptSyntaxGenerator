package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.DefaultExpression
import jp.nlaocs.skriptSyntaxGenerator.serializer.JacksonFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class DefaultExpressionDataTest {
    @Test
    fun `serializes structured default expression metadata`() {
        val json = JacksonFactory.create().valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            DefaultExpressionData(
                implementationClass = DefaultExpression::class.java,
                literal = false,
                returnType = Number::class.java,
                single = true
            )
        )

        assertEquals("ch.njol.skript.lang.DefaultExpression", json["implementationClass"].asText())
        assertFalse(json["literal"].asBoolean())
        assertEquals("java.lang.Number", json["returnType"].asText())
        assertEquals(true, json["single"].asBoolean())
    }

    @Test
    fun `collection records static expression shape when available`() {
        @Suppress("UNCHECKED_CAST")
        val expression = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(DefaultExpression::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getReturnType" -> Number::class.java
                "isSingle" -> true
                else -> error("unexpected call to ${method.name}")
            }
        } as DefaultExpression<Any>

        val data = DefaultExpressionData.from(expression)

        assertEquals(Number::class.java, data.returnType)
        assertEquals(true, data.single)
    }

    @Test
    fun `collection leaves context dependent shape unresolved`() {
        @Suppress("UNCHECKED_CAST")
        val expression = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(DefaultExpression::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getReturnType", "isSingle" -> error("${method.name} needs parser context")
                else -> error("unexpected call to ${method.name}")
            }
        } as DefaultExpression<Any>

        val data = DefaultExpressionData.from(expression)

        assertEquals(expression.javaClass, data.implementationClass)
        assertFalse(data.literal)
        assertEquals(null, data.returnType)
        assertEquals(null, data.single)
    }
}
