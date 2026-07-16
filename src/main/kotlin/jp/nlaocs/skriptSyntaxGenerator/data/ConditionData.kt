package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.Condition
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.data.common.SyntaxKind
import org.skriptlang.skript.registration.SyntaxInfo

class ConditionData(
    s: SyntaxInfo<out Condition>,
    registrationOrder: Int,
    registrationOccurrence: Int
) : CommonSyntaxData(s, SyntaxKind.CONDITION, registrationOrder, registrationOccurrence)
