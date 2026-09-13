package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.List;
import java.util.Map;

public final class BlockDataBlockData {
    private final String defaultState;
    private final Map<String, List<String>> properties;

    public BlockDataBlockData(String defaultState, Map<String, List<String>> properties) {
        this.defaultState = defaultState;
        this.properties = properties;
    }

    public String getDefaultState() { return defaultState; }
    public Map<String, List<String>> getProperties() { return properties; }
}
