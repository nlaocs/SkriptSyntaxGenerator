package jp.nlaocs.skriptSyntaxGenerator.serializer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import jp.nlaocs.skriptSyntaxGenerator.EntryDataSerializer
import org.skriptlang.skript.lang.entry.EntryData
import java.util.regex.Pattern

object GsonFactory {
    fun create(): Gson = GsonBuilder()
        .registerTypeAdapter(
            Class::class.java,
            JsonSerializer<Class<*>> { src, _, _ ->
                JsonPrimitive(src.name)
            })
        .registerTypeAdapter(Pattern::class.java, PatternAdapter())
        .registerTypeHierarchyAdapter(EntryData::class.java, EntryDataSerializer())
        // .registerTypeAdapterFactory(entryDataAdapter)
        // .registerTypeAdapter(java.awt.Color::class.java, ColorAdapter())
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()
}
