package jp.nlaocs.skriptSyntaxGenerator.data;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventValueApi {
    LEGACY("legacy"),
    MODERN_2_15("modern-2.15"),
    MODERN_2_16("modern-2.16");

    private final String serializedName;

    EventValueApi(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() { return serializedName; }

    @JsonValue
    public String toJson() { return serializedName; }
}
