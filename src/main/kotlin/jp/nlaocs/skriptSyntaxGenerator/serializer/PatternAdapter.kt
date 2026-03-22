package jp.nlaocs.skriptSyntaxGenerator.serializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.regex.Pattern

class PatternAdapter : JsonSerializer<Pattern>, JsonDeserializer<Pattern> {

    override fun serialize(
        src: Pattern,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.pattern())
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Pattern {
        return Pattern.compile(json.asString)
    }
}
