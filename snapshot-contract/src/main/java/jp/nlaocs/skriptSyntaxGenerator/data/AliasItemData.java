package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.Map;
import java.util.Objects;

public final class AliasItemData {
    private final String material;
    private final String minecraftId;
    private final int durability;
    private final boolean plain;
    private final boolean alias;
    private final Object blockValues;
    private final Map<String, Object> itemMeta;

    public AliasItemData(
        String material,
        String minecraftId,
        int durability,
        boolean plain,
        boolean alias,
        Object blockValues,
        Map<String, Object> itemMeta
    ) {
        this.material = material;
        this.minecraftId = minecraftId;
        this.durability = durability;
        this.plain = plain;
        this.alias = alias;
        this.blockValues = blockValues;
        this.itemMeta = itemMeta;
    }

    public String getMaterial() { return material; }
    public String getMinecraftId() { return minecraftId; }
    public int getDurability() { return durability; }
    public boolean isPlain() { return plain; }
    public boolean isAlias() { return alias; }
    public Object getBlockValues() { return blockValues; }
    public Map<String, Object> getItemMeta() { return itemMeta; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AliasItemData)) return false;
        AliasItemData item = (AliasItemData) other;
        return durability == item.durability && plain == item.plain && alias == item.alias &&
            Objects.equals(material, item.material) && Objects.equals(minecraftId, item.minecraftId) &&
            Objects.equals(blockValues, item.blockValues) && Objects.equals(itemMeta, item.itemMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, minecraftId, durability, plain, alias, blockValues, itemMeta);
    }
}