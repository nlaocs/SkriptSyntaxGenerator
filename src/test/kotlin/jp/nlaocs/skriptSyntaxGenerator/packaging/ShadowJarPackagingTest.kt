package jp.nlaocs.skriptSyntaxGenerator.packaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.util.zip.ZipFile

class ShadowJarPackagingTest {
    private val shadowJar = File(requireNotNull(System.getProperty("ssg.shadowJar")))

    @Test
    fun `packaged dependency exposes the versioned JDK bridge`() {
        JarFile(shadowJar, true, ZipFile.OPEN_READ, Runtime.Version.parse("25")).use { jar ->
            assertTrue(jar.isMultiRelease, "The shaded JAR must retain dependency multi-release metadata")
            for (name in listOf("JdkClassReader", "JdkClassWriter")) {
                val entry = jar.getJarEntry("net/bytebuddy/jar/asmjdkbridge/$name.class")
                assertNotNull(entry, "Java 25 must resolve the packaged $name")
                assertTrue(entry.realName.startsWith("META-INF/versions/"), "The bridge must come from a versioned entry")
            }
        }
    }

    @Test
    fun `actual converter Advice generates from the packaged Byte Buddy`() {
        assertNull(System.getProperty("net.bytebuddy.experimental"))
        assertNull(System.getProperty("net.bytebuddy.processor"))
        val fixtureUrl = ConverterHookPackagingFixture::class.java.protectionDomain.codeSource.location
        URLClassLoader(arrayOf(shadowJar.toURI().toURL(), fixtureUrl), ClassLoader.getPlatformClassLoader()).use { loader ->
            val byteBuddy = loader.loadClass("net.bytebuddy.ByteBuddy")
            assertEquals(shadowJar.toURI().toURL(), byteBuddy.protectionDomain.codeSource.location)
            val target = loader.loadClass(ConverterHookPackagingFixture::class.java.name)
            val builder = byteBuddy.getMethod("redefine", Class::class.java)
                .invoke(byteBuddy.getConstructor().newInstance(), target)
            val advice = loader.loadClass("net.bytebuddy.asm.Advice")
            val converterAdvice = loader.loadClass("jp.nlaocs.skriptSyntaxGenerator.hook.RegisterConverterAdvice")
            val visitor = advice.getMethod("to", Class::class.java).invoke(null, converterAdvice)
            val matcher = loader.loadClass("net.bytebuddy.matcher.ElementMatchers")
                .getMethod("named", String::class.java).invoke(null, "registerConverter")
            val methodVisitor = advice.getMethod("on", loader.loadClass("net.bytebuddy.matcher.ElementMatcher"))
                .invoke(visitor, matcher)
            val builderApi = loader.loadClass("net.bytebuddy.dynamic.DynamicType\$Builder")
            val advised = builderApi.getMethod("visit", loader.loadClass("net.bytebuddy.asm.AsmVisitorWrapper"))
                .invoke(builder, methodVisitor)
            val generated = builderApi.getMethod("make").invoke(advised)
            val bytes = loader.loadClass("net.bytebuddy.dynamic.DynamicType").getMethod("getBytes").invoke(generated) as ByteArray
            assertTrue(bytes.isNotEmpty())
        }
    }
}
