package jp.nlaocs.skriptSyntaxGenerator.data

import org.skriptlang.skript.lang.entry.ContainerEntryData
import org.skriptlang.skript.lang.entry.EntryData
import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.KeyValueEntryData
import org.skriptlang.skript.lang.entry.SectionEntryData
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.LiteralEntryData
import org.skriptlang.skript.lang.entry.util.TriggerEntryData
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData
import java.util.regex.Pattern

data class EntryValidatorData(
    val entryData: List<EntryDataInfo>,
) {
    companion object {
        fun from(src: EntryValidator): EntryValidatorData =
            EntryValidatorData(src.entryData.map { EntryDataInfo.from(it) })
    }
}

data class EntryDataInfo(
    val key: String,
    val defaultValue: Any?,
    val optional: Boolean,
    val multiple: Boolean,
    val entryDataClass: Class<*>,
    val kind: String,
    val separator: String? = null,
    val valueType: Class<*>? = null,
    val stringMode: String? = null,
    val returnTypes: List<Class<*>>? = null,
    val flags: Int? = null,
    val nestedValidator: EntryValidatorData? = null,
) {
    companion object {
        fun from(src: EntryData<*>): EntryDataInfo {
            val key = src.key
            val defaultValue = normalizeValue(src.defaultValue)
            val optional = src.isOptional
            val multiple = src.supportsMultiple()
            val entryDataClass = src.javaClass

            return when (src) {
                is LiteralEntryData<*> -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "literal",
                    separator = src.separator,
                    valueType = readPrivateField<Class<*>>(src, "type"),
                )

                is VariableStringEntryData -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "variableString",
                    separator = src.separator,
                    stringMode = readPrivateField<Any>(src, "stringMode")?.toString(),
                )

                is ExpressionEntryData<*> -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "expression",
                    separator = src.separator,
                    returnTypes = readPrivateField<Array<Class<*>>>(src, "returnTypes")?.toList(),
                    flags = readPrivateField<Int>(src, "flags"),
                )

                is TriggerEntryData -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "trigger",
                )

                is ContainerEntryData -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "container",
                    nestedValidator = EntryValidatorData.from(src.entryValidator),
                ) // todo ネスト無限になる可能性

                is SectionEntryData -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "section",
                )

                is KeyValueEntryData<*> -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "keyValue",
                    separator = src.separator,
                )

                else -> EntryDataInfo(
                    key = key,
                    defaultValue = defaultValue,
                    optional = optional,
                    multiple = multiple,
                    entryDataClass = entryDataClass,
                    kind = "unknown",
                )
            }
        }

        private fun normalizeValue(value: Any?): Any? = when (value) {
            null -> null
            is String, is Number, is Boolean -> value
            is Enum<*> -> value.name
            is Pattern -> value.pattern()
            is Collection<*> -> value.map { normalizeValue(it) }
            is Array<*> -> value.map { normalizeValue(it) }
            is Map<*, *> -> value.mapKeys { normalizeValue(it.key) }.mapValues { normalizeValue(it.value) }
            else -> value.toString()
        }

        private fun <T> readPrivateField(target: Any, fieldName: String): T? =
            runCatching {
                val field = target.javaClass.getDeclaredField(fieldName)
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                field.get(target) as T?
            }.getOrNull()
    }
}
