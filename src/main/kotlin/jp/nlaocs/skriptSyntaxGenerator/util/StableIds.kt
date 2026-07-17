package jp.nlaocs.skriptSyntaxGenerator.util

import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import java.security.DigestOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object StableIds {
    fun definition(kind: String, addon: AddonInfo, elementClass: Class<*>): String =
        "${normalize(kind)}:${normalize(addon.name)}:${digest(encode(listOf(addon.name, elementClass.stableName())))}"

    fun registration(definitionId: String, patterns: List<String>, occurrence: Int): String =
        "$definitionId:${digest(encode(listOf(definitionId) + patterns))}:$occurrence"

    fun record(kind: String, addon: AddonInfo, vararg parts: String): String =
        "${normalize(kind)}:${normalize(addon.name)}:${digest(encode(listOf(addon.name) + parts.asList()))}"

    fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .toHexString()

    fun digest(writeValue: (Writer) -> Unit): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val sink = object : OutputStream() {
            override fun write(value: Int) = Unit
            override fun write(buffer: ByteArray, offset: Int, length: Int) = Unit
        }
        OutputStreamWriter(
            DigestOutputStream(sink, messageDigest),
            StandardCharsets.UTF_8
        ).use(writeValue)
        return messageDigest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun encode(parts: Collection<String>): String =
        parts.joinToString("|") { part -> "${part.length}:$part" }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase(Locale.ENGLISH)
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifEmpty { "unknown" }
}
