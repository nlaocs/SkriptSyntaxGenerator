package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.Objects;

public final class PluralRuleAddonData {
    private final String name;
    private final String version;

    public PluralRuleAddonData(String name, String version) {
        if (name == null || name.trim().isEmpty() || version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Plural rule addon name and version cannot be blank");
        }
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PluralRuleAddonData)) return false;
        PluralRuleAddonData that = (PluralRuleAddonData) object;
        return name.equals(that.name) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }
}
