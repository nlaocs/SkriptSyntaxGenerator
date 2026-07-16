package jp.nlaocs.skriptSyntaxGenerator.collector

import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import org.skriptlang.skript.registration.SyntaxInfo

interface SyntaxCollector<T> {
    val fileName: String
    fun collect(): T
}

internal class SyntaxOccurrenceTracker {
    private val occurrences = mutableMapOf<SyntaxOccurrenceKey, Int>()

    fun next(info: SyntaxInfo<*>): Int {
        val key = SyntaxOccurrenceKey(
            elementClass = info.type().stableName(),
            patterns = info.patterns().toList()
        )
        val occurrence = occurrences.getOrDefault(key, 0)
        occurrences[key] = occurrence + 1
        return occurrence
    }
}

private data class SyntaxOccurrenceKey(
    val elementClass: String,
    val patterns: List<String>
)
