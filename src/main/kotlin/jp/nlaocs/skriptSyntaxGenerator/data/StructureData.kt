package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class StructureData(s: DefaultSyntaxInfos.Structure<*>) : CommonSyntaxData(s) {
    val entryValidator: EntryValidatorData? = s.entryValidator()?.let(EntryValidatorData::from)
    val nodeType: DefaultSyntaxInfos.Structure.NodeType? = s.nodeType() // todo nullの可能性があるかどうかの確認
}
