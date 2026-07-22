package jp.nlaocs.skriptSyntaxGenerator.data;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PluralRuleOrigin {
    BUILT_IN("built-in"),
    OVERRIDE("override");

    private final String serializedName;

    PluralRuleOrigin(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    @JsonValue
    public String toJson() {
        return serializedName;
    }
}
