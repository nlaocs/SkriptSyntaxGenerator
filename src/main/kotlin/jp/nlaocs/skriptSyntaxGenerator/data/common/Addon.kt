package jp.nlaocs.skriptSyntaxGenerator.data.common

data class AddonInfo(
    val name: String,
    val version: String,
)

interface Addon {
    val addon: AddonInfo
}
