package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.lang.EffectSection
import ch.njol.skript.lang.LoopSection
import ch.njol.skript.lang.Section
import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import org.skriptlang.skript.registration.SyntaxInfo

class SectionData(s: SyntaxInfo<out Section>) : CommonSyntaxData(s) {
    val isLoopSection: Boolean = LoopSection::class.java.isAssignableFrom(s.type())
    val isEffectSection: Boolean = EffectSection::class.java.isAssignableFrom(s.type())
}
