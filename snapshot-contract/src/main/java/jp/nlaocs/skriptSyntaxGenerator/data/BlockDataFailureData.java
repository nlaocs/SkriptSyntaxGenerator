package jp.nlaocs.skriptSyntaxGenerator.data;

public final class BlockDataFailureData {
    private final String block;
    private final String message;

    public BlockDataFailureData(String block, String message) {
        this.block = block;
        this.message = message;
    }

    public String getBlock() { return block; }
    public String getMessage() { return message; }
}
