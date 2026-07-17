package jp.nlaocs.skriptSyntaxGenerator.data;

public final class SnapshotCapabilitiesData {
    private final SyntaxApi syntaxApi;
    private final EventValueApi eventValueApi;
    private final SyntaxKindCapabilitiesData syntaxKinds;
    private final AliasesCapabilitiesData aliases;

    public SnapshotCapabilitiesData(
        SyntaxApi syntaxApi,
        EventValueApi eventValueApi,
        SyntaxKindCapabilitiesData syntaxKinds,
        AliasesCapabilitiesData aliases
    ) {
        if (syntaxApi == null || eventValueApi == null || syntaxKinds == null || aliases == null) {
            throw new IllegalArgumentException("Snapshot capabilities cannot contain null values");
        }
        this.syntaxApi = syntaxApi;
        this.eventValueApi = eventValueApi;
        this.syntaxKinds = syntaxKinds;
        this.aliases = aliases;
    }

    public SyntaxApi getSyntaxApi() { return syntaxApi; }
    public EventValueApi getEventValueApi() { return eventValueApi; }
    public SyntaxKindCapabilitiesData getSyntaxKinds() { return syntaxKinds; }
    public AliasesCapabilitiesData getAliases() { return aliases; }

    public String fingerprint() {
        String[] parts = {
            syntaxApi.getSerializedName(), eventValueApi.getSerializedName(),
            syntaxKinds.fingerprint(), aliases.fingerprint()
        };
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) result.append('|');
            result.append(parts[index].length()).append(':').append(parts[index]);
        }
        return result.toString();
    }
}
