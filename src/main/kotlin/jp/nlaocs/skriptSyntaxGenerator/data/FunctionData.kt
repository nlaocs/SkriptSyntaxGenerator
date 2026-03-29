package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.function.Function
import jp.nlaocs.skriptSyntaxGenerator.data.common.Documentable

class FunctionData(s: Function<*>) : Documentable {
    override val name: String? = s.name
    override val description: List<String>?
    override val since: List<String>?
    override val examples: List<String>?
    override val keywords: List<String>?
    override val requires: List<String>?
    val returnType: Class<*>? = s.type()
    val returnTypeIsSingle: Boolean = s.isSingle // Expressionと違って、true/falseしか来ない

    init {
        when (s) {
            is org.skriptlang.skript.common.function.DefaultFunction<*> -> {
                since = s.since()
                description = s.description()
                examples = s.examples()
                keywords = s.keywords()
                requires = s.requires()
            }

            // JavaFunction & SimpleJavaFunction
            is ch.njol.skript.lang.function.JavaFunction<*> -> {
                since = s.since()
                description = s.description()
                examples = s.examples()
                keywords = s.keywords()
                requires = s.requires()
            }

            else -> error("unreachable")
        }
    }
}
