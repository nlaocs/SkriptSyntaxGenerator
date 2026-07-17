package jp.nlaocs.skriptSyntaxGenerator.data;

public final class AliasesCapabilitiesData {
    private final boolean supported;
    private final boolean collected;

    public AliasesCapabilitiesData(boolean supported, boolean collected) {
        if (collected && !supported)
            throw new IllegalArgumentException("Unsupported aliases cannot be collected");
        this.supported = supported;
        this.collected = collected;
    }

    public boolean isSupported() { return supported; }
    public boolean isCollected() { return collected; }

    public String fingerprint() {
        return (supported ? "1" : "0") + ":" + (collected ? "1" : "0");
    }
}