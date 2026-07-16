package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.Skript
import ch.njol.skript.registrations.Classes
import jp.nlaocs.skriptSyntaxGenerator.data.PropertyData
import org.skriptlang.skript.lang.properties.Property
import org.skriptlang.skript.lang.properties.PropertyRegistry

class PropertyCollector : SyntaxCollector<List<PropertyData>> {
    override val fileName: String = "Properties.json"

    override fun collect(): List<PropertyData> {
        val registry = Skript.instance().registry(PropertyRegistry::class.java)
        val properties = linkedSetOf<Property<*>>()
        properties.addAll(registry.elements())
        Classes.getClassInfos().forEach { classInfo ->
            properties.addAll(classInfo.allProperties)
        }

        return properties
            .sortedWith(compareBy({ it.name() }, { it.provider().name() }, { it.handler().name }))
            .map { property ->
                PropertyData(property, Classes.getClassInfosByProperty(property).toList())
            }
    }
}
