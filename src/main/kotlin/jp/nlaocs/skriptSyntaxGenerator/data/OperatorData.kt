package jp.nlaocs.skriptSyntaxGenerator.data

import org.skriptlang.skript.lang.arithmetic.Operator as SkriptOperator
import org.skriptlang.skript.util.Priority

data class OperatorData(
    val sign: String,
    val priority: Priority,
    val key: String?,
) {
    constructor(operator: SkriptOperator) : this(
        sign = operator.sign(),
        priority = operator.priority(),
        key = operator.node?.key,
    )
}
