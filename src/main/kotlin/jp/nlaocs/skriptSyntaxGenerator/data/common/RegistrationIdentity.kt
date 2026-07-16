package jp.nlaocs.skriptSyntaxGenerator.data.common

import com.fasterxml.jackson.annotation.JsonValue

enum class SyntaxKind(@get:JsonValue val value: String) {
    EVENT("event"),
    CONDITION("condition"),
    EFFECT("effect"),
    EXPRESSION("expression"),
    SECTION("section"),
    STRUCTURE("structure")
}
