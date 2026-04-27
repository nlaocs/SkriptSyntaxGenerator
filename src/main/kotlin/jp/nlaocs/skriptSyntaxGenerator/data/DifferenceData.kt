package jp.nlaocs.skriptSyntaxGenerator.data

import org.skriptlang.skript.lang.arithmetic.DifferenceInfo

data class DifferenceData(
    val type: Class<*>,
    val returnType: Class<*>,
) {
    constructor(difference: DifferenceInfo<*, *>) : this(
        type = difference.type(),
        returnType = difference.returnType(),
    )
}
