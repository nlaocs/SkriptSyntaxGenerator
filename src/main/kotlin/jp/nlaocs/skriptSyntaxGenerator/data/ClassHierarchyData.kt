package jp.nlaocs.skriptSyntaxGenerator.data

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo

data class ClassHierarchyData(
    val name: String,
    val binaryName: String,
    val kind: String,
    val superClass: String?,
    val interfaces: List<String>,
    val componentType: String?,
    val methods: List<ClassMethodData>,
    val containerElementType: String?,
    val provider: AddonInfo?
)
