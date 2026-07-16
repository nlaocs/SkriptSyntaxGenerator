package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ClassHierarchyData
import jp.nlaocs.skriptSyntaxGenerator.util.AddonResolver
import jp.nlaocs.skriptSyntaxGenerator.util.getTypeStr
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import java.lang.reflect.Modifier
import java.util.IdentityHashMap

class ClassHierarchyCollector {
    private val classes = linkedMapOf<String, Class<*>>()
    private val visited = IdentityHashMap<Any, Boolean>()

    fun collect(values: Collection<Any?>): List<ClassHierarchyData> {
        values.forEach(::visit)
        return classes.values
            .sortedBy { it.stableName() }
            .map(::toData)
    }

    private fun visit(value: Any?) {
        if (value == null) {
            return
        }
        if (value is Class<*>) {
            register(value)
            return
        }
        if (value is String || value is Number || value is Boolean || value is Enum<*>) {
            return
        }
        if (visited.put(value, true) != null) {
            return
        }

        when (value) {
            is Map<*, *> -> value.forEach { (key, entryValue) ->
                visit(key)
                visit(entryValue)
            }
            is Iterable<*> -> value.forEach(::visit)
            is Array<*> -> value.forEach(::visit)
            else -> visitDataFields(value)
        }
    }

    private fun visitDataFields(value: Any) {
        var type: Class<*>? = value.javaClass
        while (type != null && type != Any::class.java) {
            if (!type.packageName.startsWith("jp.nlaocs.skriptSyntaxGenerator.data")) {
                type = type.superclass
                continue
            }
            type.declaredFields
                .asSequence()
                .filterNot { Modifier.isStatic(it.modifiers) || Modifier.isTransient(it.modifiers) || it.isSynthetic }
                .forEach { field ->
                    runCatching {
                        field.isAccessible = true
                        visit(field.get(value))
                    }
                }
            type = type.superclass
        }
    }

    private fun register(type: Class<*>) {
        if (classes.putIfAbsent(type.stableName(), type) != null) {
            return
        }
        type.superclass?.let(::register)
        type.interfaces.forEach(::register)
        type.componentType?.let(::register)
    }

    private fun toData(type: Class<*>): ClassHierarchyData =
        ClassHierarchyData(
            name = type.stableName(),
            binaryName = type.name,
            kind = type.getTypeStr(),
            superClass = type.superclass?.stableName(),
            interfaces = type.interfaces.map { it.stableName() }.sorted(),
            componentType = type.componentType?.stableName(),
            provider = AddonResolver.fromClass(type)
        )
}