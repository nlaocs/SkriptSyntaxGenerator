package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import ch.njol.skript.doc.Name
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.util.annoValue
import org.bukkit.Bukkit
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class ExpressionData(s: DefaultSyntaxInfos.Expression<*, *>) : CommonSyntaxData(s) {
    val returnType: Class<*>? = s.returnType() // todo nullableにすべきかを調べる

    val returnTypeMultiplicity: Multiplicity? = try {
        Multiplicity.fromBoolean(s.instance().isSingle)
    } catch (e: Exception) {
        Bukkit.getLogger().warning(
            "Failed to retrieve multiplicity for expression: ${
                s.type().annoValue<Name, String>()
            }. Falling back to BOTH. Error: ${e.message}"
        )
        null
    } // todo 動的に変動する可能性がある...?

    val acceptedChangers: Map<Changer.ChangeMode, List<Class<*>>>? = try {
        val instance = s.instance()
        val map = instance.acceptedChangeModes
        if (map.isEmpty()) null
        else map.mapValues { it.value.toList() }
    } catch (e: Exception) {
        Bukkit.getLogger().warning(
            "Failed to retrieve accepted changers for expression: ${
                s.type().annoValue<Name, String>()
            }. Setting acceptedChangers to null. Error: ${e.message}"
        )
        null
    } // todo 直接保持しているが見直すべきかも

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
