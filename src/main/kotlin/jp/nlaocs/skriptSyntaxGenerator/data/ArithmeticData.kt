package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.registrations.Classes
import org.skriptlang.skript.lang.arithmetic.Arithmetics
import org.skriptlang.skript.lang.arithmetic.DifferenceInfo
import org.skriptlang.skript.lang.arithmetic.OperationInfo
import org.skriptlang.skript.lang.arithmetic.Operator as SkriptOperator
import org.skriptlang.skript.util.Priority

class ArithmeticData(
    operators: List<SkriptOperator> = Arithmetics.getAllOperators().toList()
) {
    val operators: List<OperatorData> = operators.map { OperatorData(it) }
    val operations: Map<String, List<OperationInfo<*, *, *>>> =
        operators.associate { op ->
            op.sign() to Arithmetics.getOperations(op)
        }
    val differences: Map<Class<*>, DifferenceInfo<*, *>> =
        Classes.getClassInfos()
            .mapNotNull { info ->
                Arithmetics.getDifferenceInfo(info.c)?.let { info.c to it }
            }
            .toMap()
    // DefaultValue..?

    data class OperatorData(
        val sign: String,
        val priority: Priority,
        val key: String?,
    ) {
        constructor(s: SkriptOperator) : this(
            sign = s.sign(),
            priority = s.priority(),
            key = s.node?.key,
        )
    } // Gson側で生成方法を変えてもいいかもしれない。冗長な気がする。
}
