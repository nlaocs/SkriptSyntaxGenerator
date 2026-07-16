package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.DifferenceData
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.AbstractList
import java.util.ArrayList

class ClassHierarchyCollectorTest {
    @Test
    fun `collector closes over parents interfaces and array components`() {
        val hierarchy = ClassHierarchyCollector().collect(
            listOf(
                DifferenceData(
                    type = ArrayList::class.java,
                    returnType = Array<String>::class.java,
                    registrationOrder = 0,
                    addon = AddonInfo("Test", "1.0")
                )
            )
        )
        val byName = hierarchy.associateBy { it.name }

        val arrayList = requireNotNull(byName["java.util.ArrayList"])
        assertEquals(AbstractList::class.java.name, arrayList.superClass)
        assertTrue("java.util.List" in arrayList.interfaces)
        assertTrue("java.util.RandomAccess" in arrayList.interfaces)
        assertTrue("java.lang.Object" in byName)
        assertTrue("java.util.Collection" in byName)
        assertNull(arrayList.provider)

        val stringArray = requireNotNull(byName["java.lang.String[]"])
        assertEquals("Array", stringArray.kind)
        assertEquals("java.lang.String", stringArray.componentType)
        assertTrue("java.lang.String" in byName)

        assertEquals(hierarchy.map { it.name }.sorted(), hierarchy.map { it.name })
    }
}
