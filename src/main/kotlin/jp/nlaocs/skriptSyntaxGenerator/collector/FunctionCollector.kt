package jp.nlaocs.skriptSyntaxGenerator.collector

import ch.njol.skript.lang.function.Function
import ch.njol.skript.lang.function.Functions
import ch.njol.skript.lang.function.ScriptFunction
import jp.nlaocs.skriptSyntaxGenerator.data.FunctionData

class FunctionCollector : SyntaxCollector<List<FunctionData>> {
    override val fileName: String = "Functions.json"

    override fun collect(): List<FunctionData> =
        registeredJavaFunctions()
            .sortedWith(compareBy<Function<*>> { it.name }.thenBy(::signatureKey))
            .mapIndexed { index, function -> FunctionData(function, index) }

    private fun registeredJavaFunctions(): Collection<Function<*>> {
        val functions = runCatching {
            val registryClass = Class.forName("ch.njol.skript.lang.function.FunctionRegistry")
            val registry = registryClass.getMethod("getRegistry").invoke(null)
            val elements = registryClass.getMethod("elements").invoke(registry) as Collection<*>
            elements.filterIsInstance<Function<*>>()
        }.getOrNull() ?: Functions.getFunctions()

        // FunctionRegistry also contains functions declared by loaded scripts.
        return functions.filterNot { function -> function is ScriptFunction<*> }
    }

    private fun signatureKey(function: Function<*>): String =
        buildString {
            function.signature.parameters().all().forEach { parameter ->
                append(parameter.name())
                append(':')
                append(parameter.type().name)
                append(':')
                append(parameter.isSingle)
                append(':')
                parameter.modifiers().forEach { modifier ->
                    append(modifier.javaClass.name)
                    append(':')
                    append(modifier)
                    append(',')
                }
                append(';')
            }
            append("->")
            append(function.type()?.name)
            append(':')
            append(function.isSingle)
        }
}
