package jp.nlaocs.skriptSyntaxGenerator.data

/** One finite value exposed by a registered type's supplier and parser. */
data class TypeLiteralData(
    val text: String,
    val pluralText: String?,
    val variableName: String?,
    val debugText: String?,
    val valueClass: Class<*>,
    val representedClass: Class<*>?,
    val enumConstant: String?
)
