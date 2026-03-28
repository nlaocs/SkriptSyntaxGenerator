package jp.nlaocs.skriptSyntaxGenerator.data.common

interface Documentable {
    val name: String?
    val description: List<String>?
    val since: List<String>?
    val examples: List<String>?
    val keywords: List<String>?
    val requiredPlugins: List<String>?
}
