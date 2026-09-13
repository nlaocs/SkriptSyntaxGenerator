package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.List;
import java.util.Map;

public final class BlockDataSnapshotData {
    private final String state;
    private final boolean complete;
    private final String registryProvider;
    private final Map<String, BlockDataBlockData> blocks;
    private final List<BlockDataFailureData> failures;

    public BlockDataSnapshotData(
        String state,
        boolean complete,
        String registryProvider,
        Map<String, BlockDataBlockData> blocks,
        List<BlockDataFailureData> failures
    ) {
        this.state = state;
        this.complete = complete;
        this.registryProvider = registryProvider;
        this.blocks = blocks;
        this.failures = failures;
    }

    public String getState() { return state; }
    public boolean isComplete() { return complete; }
    public String getRegistryProvider() { return registryProvider; }
    public Map<String, BlockDataBlockData> getBlocks() { return blocks; }
    public List<BlockDataFailureData> getFailures() { return failures; }
}
