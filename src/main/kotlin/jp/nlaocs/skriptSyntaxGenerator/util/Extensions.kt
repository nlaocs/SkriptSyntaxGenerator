package jp.nlaocs.skriptSyntaxGenerator.util

import ch.njol.skript.expressions.base.EventValueExpression
import ch.njol.skript.expressions.base.PropertyExpression
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.util.Priority
import java.util.Locale

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

fun Class<*>.toStringListSafe(): List<String> {
    if (!isEnum) return emptyList()
    return (enumConstants as Array<Enum<*>>).map { constant ->
        constant.name
            .lowercase(Locale.ENGLISH)
            .replace('_', ' ')
    }
}

fun <T> Collection<T>?.nullIfEmpty(): List<T>? =
    this?.toList()?.ifEmpty { null }

fun <T> Array<T>?.toListOrNullIfEmpty(): List<T>? =
    this?.toList()?.ifEmpty { null }

// listの中のstringをtrimして空文字のものを除外するやつ、nullのものも除外、すべて空文字の場合nullを返す
fun List<String?>?.cleaning(): List<String?>? {
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
