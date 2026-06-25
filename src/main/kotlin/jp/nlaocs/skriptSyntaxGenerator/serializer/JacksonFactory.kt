package jp.nlaocs.skriptSyntaxGenerator.serializer

import ch.njol.skript.localization.Noun
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.skriptlang.skript.util.Priority
import java.util.regex.Pattern

object JacksonFactory {
    fun create(): ObjectMapper = JsonMapper.builder()
        .addModule(skriptSyntaxModule())
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build()

    private fun skriptSyntaxModule(): SimpleModule = SimpleModule()
        .addSerializer(classType(), ClassSerializer())
        .addKeySerializer(Class::class.java, ClassKeySerializer())
        .addSerializer(Pattern::class.java, PatternSerializer())
        .addSerializer(Noun::class.java, NounSerializer())
        .addSerializer(Priority::class.java, PrioritySerializer())

    @Suppress("UNCHECKED_CAST")
    private fun classType(): Class<Class<*>> = Class::class.java as Class<Class<*>>

    private class ClassSerializer : JsonSerializer<Class<*>>() {
        override fun serialize(value: Class<*>, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.toString())
        }
    }

    private class ClassKeySerializer : JsonSerializer<Class<*>>() {
        override fun serialize(value: Class<*>, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeFieldName(value.toString())
        }
    }

    private class PatternSerializer : JsonSerializer<Pattern>() {
        override fun serialize(value: Pattern, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeString(value.pattern())
        }
    }

    private class NounSerializer : JsonSerializer<Noun>() {
        override fun serialize(value: Noun, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartObject()
            gen.writeStringField("key", value.key)
            value.value?.let { gen.writeStringField("value", it) }
            gen.writeStringField("singular", value.singular)
            gen.writeStringField("plural", value.plural)
            gen.writeNumberField("gender", value.gender)
            gen.writeEndObject()
        }
    }

    private class PrioritySerializer : JsonSerializer<Priority>() {
        override fun serialize(value: Priority, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartObject()
            gen.writeObjectField("after", value.after())
            gen.writeObjectField("before", value.before())
            gen.writeEndObject()
        }
    }
}