package jp.nlaocs.skriptSyntaxGenerator.serializer

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.skriptlang.skript.lang.entry.ContainerEntryData
import org.skriptlang.skript.lang.entry.EntryData
import org.skriptlang.skript.lang.entry.KeyValueEntryData
import org.skriptlang.skript.lang.entry.SectionEntryData
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.LiteralEntryData
import org.skriptlang.skript.lang.entry.util.TriggerEntryData
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData
import java.lang.reflect.Type

class EntryDataSerializer : JsonSerializer<EntryData<*>> {

    override fun serialize(src: EntryData<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        obj.addProperty("key", src.key)
        obj.add("defaultValue", context.serialize(src.defaultValue))
        obj.addProperty("optional", src.isOptional)
        obj.addProperty("multiple", src.supportsMultiple())
        obj.addProperty("entryDataClass", src::class.simpleName)

        when (src) {
            is LiteralEntryData<*> -> {}
            is VariableStringEntryData -> {}
            is ExpressionEntryData<*> -> {}
            is TriggerEntryData -> {}
            is ContainerEntryData -> {}
            is KeyValueEntryData -> {}
            is SectionEntryData -> {}
            /*is LiteralEntryData<*> -> {
                try {
                    val typeField = LiteralEntryData::class.java.getDeclaredField("type")
                    typeField.isAccessible = true
                    val type = typeField.get(src) as Class<*>
                    obj.addProperty("type", type.name)
                } catch (e: Exception) {
                    obj.add("type", JsonNull.INSTANCE)
                }
            }

            is VariableStringEntryData -> {
                try {
                    val stringModeField = VariableStringEntryData::class.java.getDeclaredField("stringMode")
                    stringModeField.isAccessible = true
                    val stringMode = stringModeField.get(src) as StringMode
                    obj.addProperty("stringMode", stringMode.name)
                } catch (e: Exception) {
                    obj.add("stringMode", JsonNull.INSTANCE)
                }
            }*/

            /*is KeyValueEntryData -> {

            }*/
            // SectionEntryData の場合は単純に型名だけを記録
        }

        return obj
    }
} // todo!!!!!
