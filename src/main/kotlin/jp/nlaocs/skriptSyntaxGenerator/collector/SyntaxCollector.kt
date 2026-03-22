package jp.nlaocs.skriptSyntaxGenerator.collector

interface SyntaxCollector<T> {
    val fileName: String
    fun collect(): List<T>
}
