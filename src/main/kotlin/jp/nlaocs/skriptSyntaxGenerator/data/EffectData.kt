package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.Effect
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.data.common.SyntaxKind
import org.skriptlang.skript.registration.SyntaxInfo

class EffectData(
    s: SyntaxInfo<out Effect>,
    registrationOrder: Int,
    registrationOccurrence: Int
) : CommonSyntaxData(s, SyntaxKind.EFFECT, registrationOrder, registrationOccurrence)
