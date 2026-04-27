package jp.nlaocs.skriptSyntaxGenerator.data

import org.skriptlang.skript.lang.arithmetic.OperationInfo

data class OperationData(
    val left: Class<*>,
    val right: Class<*>,
    val returnType: Class<*>,
) {
    constructor(operation: OperationInfo<*, *, *>) : this(
        left = operation.left(),
        right = operation.right(),
        returnType = operation.returnType(),
    )
}
