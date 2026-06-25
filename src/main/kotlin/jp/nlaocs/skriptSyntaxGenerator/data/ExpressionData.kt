package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Name
import ch.njol.skript.expressions.base.SectionExpression
import ch.njol.skript.registrations.Classes
import com.fasterxml.jackson.annotation.JsonValue
import jp.nlaocs.skriptSyntaxGenerator.bytecode.ExpressionBytecodeAnalyzer
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.util.annoValue
import org.bukkit.Bukkit
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class ExpressionData(s: DefaultSyntaxInfos.Expression<*, *>) : CommonSyntaxData(s) {
    val returnType: Class<*>? = s.returnType() // todo nullableにすべきかを調べる
    val isSectionExpression: Boolean = SectionExpression::class.java.isAssignableFrom(s.type())

    @Transient
    private val returnTypeMultiplicityResult: ReturnTypeMultiplicityResult =
        when (ExpressionBytecodeAnalyzer.isSingleAnalysis(s.type())) {
            ExpressionBytecodeAnalyzer.IsSingleAnalysis.SINGLE ->
                ReturnTypeMultiplicityResult(
                    returnTypeMultiplicity = Multiplicity.SINGLE,
                    state = ReturnTypeMultiplicityState.RESOLVED
                )
            ExpressionBytecodeAnalyzer.IsSingleAnalysis.MULTIPLE ->
                ReturnTypeMultiplicityResult(
                    returnTypeMultiplicity = Multiplicity.MULTIPLE,
                    state = ReturnTypeMultiplicityState.RESOLVED
                )
            ExpressionBytecodeAnalyzer.IsSingleAnalysis.BOTH ->
                ReturnTypeMultiplicityResult(
                    returnTypeMultiplicity = Multiplicity.BOTH,
                    state = ReturnTypeMultiplicityState.RESOLVED
                )
            ExpressionBytecodeAnalyzer.IsSingleAnalysis.UNRESOLVED ->
                ReturnTypeMultiplicityResult(
                    returnTypeMultiplicity = null,
                    state = ReturnTypeMultiplicityState.UNRESOLVED
                )
        }

    val returnTypeMultiplicity: Multiplicity? =
        returnTypeMultiplicityResult.returnTypeMultiplicity

    val returnTypeMultiplicityState: ReturnTypeMultiplicityState =
        returnTypeMultiplicityResult.state

    @Transient
    private val acceptedChangersResult: AcceptedChangersResult =
        when (ExpressionBytecodeAnalyzer.acceptChangeStrategy(s.type())) {
            ExpressionBytecodeAnalyzer.AcceptChangeStrategy.INSTANCE_CALL -> resolveAcceptedChangers(s)
            ExpressionBytecodeAnalyzer.AcceptChangeStrategy.REGISTERED_RETURN_TYPE ->
                resolveAcceptedChangersFromRegisteredReturnType(s)
            ExpressionBytecodeAnalyzer.AcceptChangeStrategy.UNRESOLVED ->
                AcceptedChangersResult(
                    acceptedChangers = null,
                    state = AcceptedChangersState.UNRESOLVED
                )
        } // todo 直接保持しているが見直すべきかも

    val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>? =
        acceptedChangersResult.acceptedChangers

    val acceptedChangersState: AcceptedChangersState = acceptedChangersResult.state

    private data class AcceptedChangersResult(
        val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>?,
        val state: AcceptedChangersState
    )

    private data class ReturnTypeMultiplicityResult(
        val returnTypeMultiplicity: Multiplicity?,
        val state: ReturnTypeMultiplicityState
    )

    enum class Multiplicity {
        SINGLE, MULTIPLE, BOTH;

        fun toBoolean(): Boolean? = when (this) {
            SINGLE -> true
            MULTIPLE -> false
            BOTH -> null
        }
    }

    private fun resolveAcceptedChangers(
        s: DefaultSyntaxInfos.Expression<*, *>
    ): AcceptedChangersResult = try {
        val instance = s.instance()
        val map = instance.acceptedChangeModes
        AcceptedChangersResult(
            acceptedChangers = map.mapValues { it.value.toList() },
            state = AcceptedChangersState.RESOLVED
        )
    } catch (e: Exception) {
        Bukkit.getLogger().warning(
            "Failed to retrieve accepted changers for expression: ${
                s.type().annoValue<Name, String>()
            }. Setting acceptedChangers to null. Error: ${e.message}"
        )
        AcceptedChangersResult(
            acceptedChangers = null,
            state = AcceptedChangersState.UNRESOLVED
        )
    }

    private fun resolveAcceptedChangersFromRegisteredReturnType(
        s: DefaultSyntaxInfos.Expression<*, *>
    ): AcceptedChangersResult = try {
        val returnType = s.returnType()
        val changer = returnType?.let { Classes.getSuperClassInfo(it).changer }
        val acceptedChangers = Changer.ChangeMode.values()
            .mapNotNull { mode ->
                changer?.acceptChange(mode)?.let { acceptedTypes ->
                    mode to acceptedTypes.toList()
                }
            }
            .toMap()
        AcceptedChangersResult(
            acceptedChangers = acceptedChangers,
            state = AcceptedChangersState.RESOLVED
        )
    } catch (e: Exception) {
        Bukkit.getLogger().warning(
            "Failed to retrieve accepted changers for expression: ${
                s.type().annoValue<Name, String>()
            }. Setting acceptedChangers to null. Error: ${e.message}"
        )
        AcceptedChangersResult(
            acceptedChangers = null,
            state = AcceptedChangersState.UNRESOLVED
        )
    }
} // todo instanceが取得できなかった場合データの信用性が崩壊するので設計を考え直す

enum class AcceptedChangersState(@get:JsonValue val value: String) {
    RESOLVED("resolved"),
    UNRESOLVED("unresolved")
}

enum class ReturnTypeMultiplicityState(@get:JsonValue val value: String) {
    RESOLVED("resolved"),
    UNRESOLVED("unresolved")
}
