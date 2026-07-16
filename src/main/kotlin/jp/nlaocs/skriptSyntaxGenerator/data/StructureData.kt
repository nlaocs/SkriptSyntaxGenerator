package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import jp.nlaocs.skriptSyntaxGenerator.data.common.SyntaxKind
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class StructureData(
    s: DefaultSyntaxInfos.Structure<*>,
    registrationOrder: Int,
    registrationOccurrence: Int
) : CommonSyntaxData(s, SyntaxKind.STRUCTURE, registrationOrder, registrationOccurrence) {
    val entryValidator: EntryValidatorData? = s.entryValidator()?.let(EntryValidatorData::from)
    val nodeType: DefaultSyntaxInfos.Structure.NodeType? = s.nodeType() // todo nullの可能性があるかどうかの確認
}
