package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class ExpressionData(s: DefaultSyntaxInfos.Expression<*, *>) : CommonSyntaxData(s) {
    val returnType: Class<*>? = s.returnType() // todo nullableにすべきかを調べる
    val returnTypeMultiplicity: Multiplicity = Multiplicity.fromBoolean(s.instance().isSingle)
    val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>? // todo 直接保持しているが見直すべきかも

    init {
        val changerMap = s.instance().acceptedChangeModes.mapValues { (_, array) -> array.toList() }
        acceptedChangers = if (changerMap.isEmpty()) {
            null
        } else {
            changerMap.entries.associate { entry ->
                entry.key to entry.value.toList()
            }
        }
    }

    enum class Multiplicity {
        SINGLE, MULTIPLE, BOTH;

        fun toBoolean(): Boolean? = when (this) {
            SINGLE -> true
            MULTIPLE -> false
            BOTH -> null
        }

        companion object {
            fun fromBoolean(value: Boolean?): Multiplicity = when (value) {
                true -> SINGLE
                false -> MULTIPLE
                null -> BOTH
            }
        }
    }
} // todo instanceが取得できなかった場合データの信用性が崩壊するので設計を考え直す
