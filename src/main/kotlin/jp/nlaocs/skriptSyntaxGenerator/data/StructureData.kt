package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.CommonSyntaxData
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.registration.DefaultSyntaxInfos

class StructureData(s: DefaultSyntaxInfos.Structure<*>) : CommonSyntaxData(s) {
    val entryValidator: EntryValidator? = s.entryValidator()
    val nodeType: DefaultSyntaxInfos.Structure.NodeType? = s.nodeType() // todo nullの可能性があるかどうかの確認
}