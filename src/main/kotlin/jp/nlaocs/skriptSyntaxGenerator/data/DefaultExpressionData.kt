package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.DefaultExpression
import ch.njol.skript.lang.Literal

data class DefaultExpressionData(
    val implementationClass: Class<out DefaultExpression<*>>,
    val literal: Boolean,
    val returnType: Class<*>?,
    val single: Boolean?
) {
    companion object {
        fun from(expression: DefaultExpression<*>): DefaultExpressionData {
            val returnType = runCatching { expression.returnType }.getOrNull()
            val single = runCatching { expression.isSingle }.getOrNull()
            return DefaultExpressionData(
                implementationClass = expression.javaClass,
                literal = expression is Literal<*>,
                returnType = returnType,
                single = single
            )
        }
    }
}
