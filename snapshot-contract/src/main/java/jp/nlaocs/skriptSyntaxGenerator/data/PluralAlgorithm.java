package jp.nlaocs.skriptSyntaxGenerator.data;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PluralAlgorithm {
    LEGACY_FIRST_MATCH("legacy-first-match"),
    SINGULAR_AWARE("singular-aware"),
    UNRESOLVED("unresolved");

    private final String serializedName;

    PluralAlgorithm(String serializedName) {
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
