package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.List;
import java.util.Map;

public final class AliasSnapshotData {
    private final Map<String, Integer> aliases;
    private final List<AliasTargetData> targets;

    public AliasSnapshotData(Map<String, Integer> aliases, List<AliasTargetData> targets) {
        this.aliases = aliases;
        this.targets = targets;
    }

    public Map<String, Integer> getAliases() { return aliases; }
    public List<AliasTargetData> getTargets() { return targets; }
}