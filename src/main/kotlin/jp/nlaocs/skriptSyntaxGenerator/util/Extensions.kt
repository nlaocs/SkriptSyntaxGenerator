package jp.nlaocs.skriptSyntaxGenerator.util

import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.classes.Parser
import ch.njol.skript.expressions.base.EventValueExpression
import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.lang.ParseContext
import ch.njol.skript.localization.Language
import jp.nlaocs.skriptSyntaxGenerator.data.TypeLiteralData
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.util.Priority
import java.util.Locale
import java.util.function.Supplier

// --- Annotation Extensions ---
inline fun <reified T : Annotation> Class<*>.anno(): T? =
    getAnnotation(T::class.java)

inline fun <reified T : Annotation> Class<*>.hasAnno(): Boolean =
    isAnnotationPresent(T::class.java)

inline fun <reified T : Annotation, reified V> Class<*>.annoValue(method: String = "value"): V? =
    anno<T>()?.let { ann ->
        try {
            T::class.java.getMethod(method).invoke(ann) as? V
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
            null
        }
    }

inline fun <reified T : Annotation, reified V> Class<*>.annoValues(method: String = "value"): List<V>? =
    annoValue<T, Array<V>>(method).toListOrNullIfEmpty()

// ------
fun Class<*>.getTypeStr(): String = when {
    isAnnotation -> "Annotation"
    isEnum -> "Enum"
    isInterface -> "Interface"
    isArray -> "Array"
    isPrimitive -> "Primitive"
    isRecord -> "Record"
    isSealed -> "Sealed"
    isSynthetic -> "Synthetic"
    isMemberClass -> "MemberClass"
    isLocalClass -> "LocalClass"
    isAnonymousClass -> "AnonymousClass"
    else -> "Class"
}

fun Class<*>.stableName(): String =
    if (isArray) "${componentType.stableName()}[]" else name

fun Class<*>.enumValues(): List<String> {
    if (!isEnum) return emptyList()
    return enumConstants.mapNotNull { constant ->
        (constant as? Enum<*>)?.name
            ?.lowercase(Locale.ENGLISH)
            ?.replace('_', ' ')
    }
}

fun ClassInfo<*>.parserPatterns(): List<String>? = parserPatterns(parser)

internal fun parserPatterns(parser: Any?): List<String>? {
    parser ?: return null
    val method = parser.javaClass.methods.firstOrNull {
        it.name == "getPatterns" && it.parameterCount == 0 && it.returnType.isArray
    } ?: return null
    return runCatching { method.invoke(parser) }
        .getOrNull()
        ?.let { patterns ->
            (0 until java.lang.reflect.Array.getLength(patterns))
                .mapNotNull { index -> java.lang.reflect.Array.get(patterns, index) as? String }
                .cleaning()
        }
}

@Suppress("UNCHECKED_CAST")
fun ClassInfo<*>.literalValues(): List<String>? = literalValues(parser, supplier)

fun ClassInfo<*>.typeLiterals(): List<TypeLiteralData>? = typeLiterals(parser, supplier)

fun ClassInfo<*>.parseContexts(): List<String>? {
    val parser = parser ?: return null
    return ParseContext.entries
        .filter { context -> runCatching { parser.canParse(context) }.getOrDefault(false) }
        .map { it.name }
        .ifEmpty { null }
}

@Suppress("UNCHECKED_CAST")
internal fun literalValues(
    parser: Parser<*>?,
    supplier: Supplier<out Iterator<*>>?
): List<String>? {
    val typedParser = parser as? Parser<Any> ?: return null
    supplier ?: return null
    return runCatching {
        supplier.get()
            .asSequence()
            .mapNotNull { value ->
                runCatching { typedParser.toString(value, 0) }
                    .getOrNull()
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }
            .distinct()
            .toList()
            .ifEmpty { null }
    }.getOrNull()
}

@Suppress("UNCHECKED_CAST")
internal fun typeLiterals(
    parser: Parser<*>?,
    supplier: Supplier<out Iterator<*>>?
): List<TypeLiteralData>? {
    val typedParser = parser as? Parser<Any> ?: return null
    supplier ?: return null
    return runCatching {
        supplier.get()
            .asSequence()
            .mapNotNull { value ->
                value ?: return@mapNotNull null
                val text = parserText { typedParser.toString(value, 0) } ?: return@mapNotNull null
                TypeLiteralData(
                    text = text,
                    pluralText = parserText { typedParser.toString(value, Language.F_PLURAL) }
                        ?.takeUnless { it == text },
                    variableName = parserText { typedParser.toVariableNameString(value) },
                    debugText = parserText { typedParser.getDebugMessage(value) }
                        ?.takeUnless { it == text },
                    valueClass = value.javaClass,
                    representedClass = representedClass(value),
                    enumConstant = (value as? Enum<*>)?.name
                )
            }
            .distinct()
            .toList()
            .ifEmpty { null }
    }.getOrNull()
}

private inline fun parserText(value: () -> String?): String? =
    runCatching(value).getOrNull()?.trim()?.takeIf(String::isNotEmpty)

private fun representedClass(value: Any): Class<*>? {
    val method = value.javaClass.methods.firstOrNull {
        it.name == "getType" && it.parameterCount == 0 && it.returnType == Class::class.java
    } ?: return null
    return runCatching { method.invoke(value) as? Class<*> }.getOrNull()
}

fun <T> Collection<T>?.nullIfEmpty(): List<T>? =
    this?.toList()?.ifEmpty { null }

fun <T> Array<T>?.toListOrNullIfEmpty(): List<T>? =
    this?.toList()?.ifEmpty { null }

// listの中のstringをtrimして空文字のものを除外するやつ、nullのものも除外、すべて空文字の場合nullを返す
fun List<String?>?.cleaning(): List<String>? {
    if (this == null) return null
    val cleaned = this.mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
    return cleaned.ifEmpty { null }
}

fun Priority?.toPriorityStr(): String? = when (this) {
    null -> null
    SyntaxInfo.SIMPLE -> "SyntaxInfos.SIMPLE" // = ExpressionType.SIMPLE
    SyntaxInfo.COMBINED -> "SyntaxInfos.COMBINED" // = ExpressionType.COMBINED
    SyntaxInfo.PATTERN_MATCHES_EVERYTHING -> "SyntaxInfos.PATTERN_MATCHES_EVERYTHING" // = ExpressionType.PATTERN_MATCHES_EVERYTHING
    EventValueExpression.DEFAULT_PRIORITY -> "EventValueExpression.DEFAULT_PRIORITY" // = ExpressionType.EVENT
    PropertyExpression.DEFAULT_PRIORITY -> "PropertyExpression.DEFAULT_PRIORITY" // = ExpressionType.PROPERTY
    else -> "CUSTOM"
}
