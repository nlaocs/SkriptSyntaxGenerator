package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import ch.njol.skript.classes.ClassInfo
import com.fasterxml.jackson.annotation.JsonValue
import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.util.AddonResolver
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.nullIfEmpty
import org.skriptlang.skript.lang.properties.Property
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler
import org.skriptlang.skript.lang.properties.handlers.TypedValueHandler
import org.skriptlang.skript.lang.properties.handlers.WXYZHandler
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler

data class PropertyData(
    val name: String,
    val documentationId: String,
    val description: String,
    val since: List<String>?,
    val handlerClass: Class<*>,
    val relatedTypes: List<TypePropertyData>,
    override val addon: AddonInfo
) : Addon {
    val registrationId: String = StableIds.record("property", addon, name)

    constructor(property: Property<*>, relatedTypes: List<ClassInfo<*>>) : this(
        name = property.name(),
        documentationId = property.documentationID,
        description = property.description(),
        since = property.since().toList().nullIfEmpty(),
        handlerClass = property.handler(),
        relatedTypes = relatedTypes.map { TypePropertyData.from(property, it) }
            .sortedBy { it.typeCodeName },
        addon = AddonResolver.fromSkriptAddon(property.provider())
    )
}

data class TypePropertyData(
    val typeCodeName: String,
    val typeClass: Class<*>,
    val description: String?,
    val provider: AddonInfo?,
    val handlerClass: Class<*>,
    val handlerKind: PropertyHandlerKind,
    val returnType: Class<*>?,
    val possibleReturnTypes: List<Class<*>>?,
    val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>?,
    val requiresSourceExpressionChange: Boolean?,
    val expressionMetadataState: PropertyMetadataState?,
    val elementTypes: List<Class<*>>?,
    val supportedAxes: List<String>?
) {
    companion object {
        fun from(property: Property<*>, classInfo: ClassInfo<*>): TypePropertyData {
            val handler = propertyInfo(classInfo, property).handler()
            val expressionMetadata = handler.expressionMetadata()
            val docs = classInfo.getPropertyDocumentation(property)

            return TypePropertyData(
                typeCodeName = classInfo.codeName,
                typeClass = classInfo.c,
                description = docs?.description(),
                provider = docs?.provider()?.let(AddonResolver::fromSkriptAddon),
                handlerClass = if (handler.javaClass.isHidden) property.handler() else handler.javaClass,
                handlerKind = handler.kind(),
                returnType = expressionMetadata.value?.returnType,
                possibleReturnTypes = expressionMetadata.value?.possibleReturnTypes,
                acceptedChangers = expressionMetadata.value?.acceptedChangers,
                requiresSourceExpressionChange = expressionMetadata.value?.requiresSourceExpressionChange,
                expressionMetadataState = expressionMetadata.state,
                elementTypes = (handler as? ContainsHandler<*, *>)?.let {
                    runCatching { it.elementTypes().filterNotNull().map { type -> type as Class<*> } }.getOrNull()
                },
                supportedAxes = (handler as? WXYZHandler<*, *>)?.let { axisHandler ->
                    WXYZHandler.Axis.entries
                        .filter { axis -> runCatching { axisHandler.supportsAxis(axis) }.getOrDefault(false) }
                        .map { it.name.lowercase() }
                }
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun propertyInfo(
            classInfo: ClassInfo<*>,
            property: Property<*>
        ): Property.PropertyInfo<PropertyHandler<*>> =
            requireNotNull(
                classInfo.getPropertyInfo(property as Property<PropertyHandler<*>>)
            ) {
                "Property ${property.name()} is missing from type ${classInfo.codeName}"
            }

        private fun PropertyHandler<*>.kind(): PropertyHandlerKind = when (this) {
            is WXYZHandler<*, *> -> PropertyHandlerKind.WXYZ
            is TypedValueHandler<*, *> -> PropertyHandlerKind.TYPED_VALUE
            is ContainsHandler<*, *> -> PropertyHandlerKind.CONTAINS
            is ExpressionPropertyHandler<*, *> -> PropertyHandlerKind.EXPRESSION
            is ConditionPropertyHandler<*> -> PropertyHandlerKind.CONDITION
            else -> PropertyHandlerKind.CUSTOM
        }

        private fun PropertyHandler<*>.expressionMetadata(): PropertyMetadataResult<ExpressionPropertyMetadata> {
            if (this !is ExpressionPropertyHandler<*, *>) {
                return PropertyMetadataResult.notApplicable()
            }
            return runCatching {
                ExpressionPropertyMetadata(
                    returnType = returnType(),
                    possibleReturnTypes = possibleReturnTypes().filterNotNull().map { it as Class<*> },
                    acceptedChangers = Changer.ChangeMode.entries
                        .mapNotNull { mode ->
                            acceptChange(mode)?.filterNotNull()?.map { it as Class<*> }?.let { mode to it }
                        }
                        .toMap(),
                    requiresSourceExpressionChange = requiresSourceExprChange()
                )
            }.fold(
                onSuccess = { PropertyMetadataResult.resolved(it) },
                onFailure = { PropertyMetadataResult.unresolved() }
            )
        }
    }
}

data class ExpressionPropertyMetadata(
    val returnType: Class<*>,
    val possibleReturnTypes: List<Class<*>>,
    val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>,
    val requiresSourceExpressionChange: Boolean
)

data class PropertyMetadataResult<T>(
    val value: T?,
    val state: PropertyMetadataState?
) {
    companion object {
        fun <T> resolved(value: T): PropertyMetadataResult<T> =
            PropertyMetadataResult(value, PropertyMetadataState.RESOLVED)

        fun <T> unresolved(): PropertyMetadataResult<T> =
            PropertyMetadataResult(null, PropertyMetadataState.UNRESOLVED)

        fun <T> notApplicable(): PropertyMetadataResult<T> =
            PropertyMetadataResult(null, null)
    }
}

enum class PropertyMetadataState(@get:JsonValue val value: String) {
    RESOLVED("resolved"),
    UNRESOLVED("unresolved")
}

enum class PropertyHandlerKind(@get:JsonValue val value: String) {
    EXPRESSION("expression"),
    CONDITION("condition"),
    CONTAINS("contains"),
    TYPED_VALUE("typedValue"),
    WXYZ("wxyz"),
    CUSTOM("custom")
}