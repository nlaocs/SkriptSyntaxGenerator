package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.function.Function
import jp.nlaocs.skriptSyntaxGenerator.data.common.Documentable
import org.skriptlang.skript.common.function.Parameter

class FunctionData(s: Function<*>) : Documentable {
    override val name: String? = s.name
    override val description: List<String>?
    override val since: List<String>?
    override val examples: List<String>?
    override val keywords: List<String>?
    override val requires: List<String>?
    val returnType: Class<*>? = s.type()
    val returnTypeIsSingle: Boolean = s.isSingle // Expressionと違って、true/falseしか来ない
    val parameters: List<ParameterInfo> = s.signature.parameters().all().map { ParameterInfo(it) }.toList()

    data class ParameterInfo(
        val name: String,
        val type: Class<*>,
        val modifiers: List<ModifierInfo>,
        val isSingle: Boolean,
    ) {
        data class ModifierInfo(
            val type: String,
            val min: Any? = null,
            val max: Any? = null,
        ) {
            companion object {
                fun from(mod: Parameter.Modifier): ModifierInfo {
                    return when (mod) {
                        Parameter.Modifier.OPTIONAL ->
                            ModifierInfo("optional")

                        Parameter.Modifier.KEYED ->
                            ModifierInfo("keyed")

                        is Parameter.Modifier.RangedModifier<*> ->
                            ModifierInfo(
                                "range",
                                mod.min,
                                mod.max
                            )

                        else ->
                            ModifierInfo("unknown")
                    }
                }
            }
        }

        constructor(param: Parameter<*>) : this(
            name = param.name(),
            type = param.type(),
            modifiers =
                param.modifiers().map { ModifierInfo.from(it) }.toList(),
            isSingle = param.isSingle,
        )
    }

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
