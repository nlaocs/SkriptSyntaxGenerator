package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.lang.DefaultExpression
import jp.nlaocs.skriptSyntaxGenerator.data.DefaultExpressionData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URLClassLoader

class ClassHierarchyCollectorTest {
    @Test
    fun `collects classes nested in default expression metadata`() {
        val records = ClassHierarchyCollector().collect(
            listOf(
                DefaultExpressionData(
                    implementationClass = DefaultExpression::class.java,
                    literal = false,
                    returnType = null,
                    single = null
                )
            )
        )

        assertTrue(records.any { it.name == DefaultExpression::class.java.name })
    }

    @Test
    fun `reads method descriptors when a declared type is unavailable`() {
        val testClasses = BrokenMethodOwnerFixture::class.java.protectionDomain.codeSource.location
        val loader = object : URLClassLoader(
            arrayOf(testClasses),
            ClassHierarchyCollectorTest::class.java.classLoader
        ) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name == MissingDependencyFixture::class.java.name) {
                    throw ClassNotFoundException(name)
                }
                if (name == BrokenMethodOwnerFixture::class.java.name) {
                    return findClass(name)
                }
                return super.loadClass(name, resolve)
            }
        }
        loader.use {
            val brokenType = Class.forName(BrokenMethodOwnerFixture::class.java.name, false, loader)
            val record = ClassHierarchyCollector().collect(listOf(brokenType))
                .single { it.name == BrokenMethodOwnerFixture::class.java.name }
            assertTrue(record.methods.any { method ->
                method.name == "missing" &&
                    method.parameterTypes == listOf(MissingDependencyFixture::class.java.name) &&
                    method.returnType == MissingDependencyFixture::class.java.name
            })
            assertEquals(1, record.methods.count { it.name == "missing" })
        }
    }
}
