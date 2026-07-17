package jp.nlaocs.skriptSyntaxGenerator.data;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SyntaxApi {
    REGISTRY("registry"),
    LEGACY_STATIC("legacy-static");

    private final String serializedName;

    SyntaxApi(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() { return serializedName; }

    @JsonValue
    public String toJson() { return serializedName; }
}
