package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.List;
import java.util.Objects;

public final class AliasTargetData {
    private final int amount;
    private final boolean all;
    private final List<AliasItemData> types;

    public AliasTargetData(int amount, boolean all, List<AliasItemData> types) {
        this.amount = amount;
        this.all = all;
        this.types = types;
    }

    public int getAmount() { return amount; }
    public boolean isAll() { return all; }
    public List<AliasItemData> getTypes() { return types; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AliasTargetData)) return false;
        AliasTargetData target = (AliasTargetData) other;
        return amount == target.amount && all == target.all && Objects.equals(types, target.types);
    }

    @Override
    public int hashCode() { return Objects.hash(amount, all, types); }
}