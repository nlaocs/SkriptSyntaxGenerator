package jp.nlaocs.skriptSyntaxGenerator.data

/** One syntax pattern retained from a type parser's runtime registry. */
data class RegisteredTypeParserPatternData(
    val pattern: String,
    val registrationIndex: Int,
    val patternIndex: Int,
    val sourceCodeName: String?,
    val dataClass: Class<*>,
    val representedClass: Class<*>
)
