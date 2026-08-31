package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.data.ClassHierarchyData
import jp.nlaocs.skriptSyntaxGenerator.data.ClassMethodData
import jp.nlaocs.skriptSyntaxGenerator.util.AddonResolver
import jp.nlaocs.skriptSyntaxGenerator.util.getTypeStr
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import java.lang.reflect.Modifier
import java.util.IdentityHashMap

class ClassHierarchyCollector {
    private companion object {
        const val CONTAINER_TYPE_ANNOTATION = "ch.njol.skript.util.Container\$ContainerType"
    }

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
        containerElementType(type)?.let(::register)
    }

    private fun toData(type: Class<*>): ClassHierarchyData =
        ClassHierarchyData(
            name = type.stableName(),
            binaryName = type.name,
            kind = type.getTypeStr(),
            superClass = type.superclass?.stableName(),
            interfaces = type.interfaces.map { it.stableName() }.sorted(),
            componentType = type.componentType?.stableName(),
            methods = declaredMethods(type),
            containerElementType = containerElementType(type)?.stableName(),
            provider = AddonResolver.fromClass(type)
        )

    private fun containerElementType(type: Class<*>): Class<*>? =
        try {
            val annotation = type.declaredAnnotations.firstOrNull {
                it.annotationClass.java.name == CONTAINER_TYPE_ANNOTATION
            } ?: return null
            annotation.annotationClass.java.getMethod("value").invoke(annotation) as Class<*>
        } catch (failure: ReflectiveOperationException) {
            throw IllegalStateException("Cannot inspect @ContainerType on ${type.name}", failure)
        } catch (failure: RuntimeException) {
            throw IllegalStateException("Cannot inspect @ContainerType on ${type.name}", failure)
        } catch (failure: LinkageError) {
            throw IllegalStateException("Cannot inspect @ContainerType on ${type.name}", failure)
        }

    private fun declaredMethods(type: Class<*>): List<ClassMethodData> =
        try {
            type.declaredMethods
                .map { method ->
                    ClassMethodData(
                        method.name,
                        method.parameterTypes.map { it.stableName() },
                        method.returnType.stableName(),
                        Modifier.isStatic(method.modifiers)
                    )
                }
                .distinctBy { it.signatureKey() }
                .sortedBy { it.signatureKey() }
        } catch (failure: RuntimeException) {
            classFileMethods(type, failure)
        } catch (failure: LinkageError) {
            classFileMethods(type, failure)
        }

    private fun classFileMethods(type: Class<*>, reflectionFailure: Throwable): List<ClassMethodData> =
        try {
            ClassFileMethodReader.read(type)
        } catch (classFileFailure: Exception) {
            throw methodInspectionFailure(type, reflectionFailure, classFileFailure)
        } catch (classFileFailure: LinkageError) {
            throw methodInspectionFailure(type, reflectionFailure, classFileFailure)
        }

    private fun methodInspectionFailure(
        type: Class<*>,
        reflectionFailure: Throwable,
        classFileFailure: Throwable
    ): IllegalStateException {
        reflectionFailure.addSuppressed(classFileFailure)
        return IllegalStateException("Cannot inspect declared methods of ${type.name}", reflectionFailure)
    }
}
