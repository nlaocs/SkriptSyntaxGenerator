package jp.nlaocs.skriptSyntaxGenerator.data;

public final class PluralRuleData {
    private final int ruleOrder;
    private final String singular;
    private final String plural;
    private final Boolean completeWord;
    private final PluralRuleOrigin origin;
    private final Integer overrideRegistrationOrder;
    private final PluralRuleAddonData addon;

    public PluralRuleData(
        int ruleOrder,
        String singular,
        String plural,
        Boolean completeWord,
        PluralRuleOrigin origin,
        Integer overrideRegistrationOrder,
        PluralRuleAddonData addon
    ) {
        if (ruleOrder < 0 || singular == null || plural == null || origin == null || addon == null) {
            throw new IllegalArgumentException("Plural rule fields cannot be null or negative");
        }
        this.ruleOrder = ruleOrder;
        this.singular = singular;
        this.plural = plural;
        this.completeWord = completeWord;
        this.origin = origin;
        this.overrideRegistrationOrder = overrideRegistrationOrder;
        this.addon = addon;
    }

    public int getRuleOrder() {
        return ruleOrder;
    }

    public String getSingular() {
        return singular;
    }

    public String getPlural() {
        return plural;
    }

    public Boolean getCompleteWord() {
        return completeWord;
    }

    public PluralRuleOrigin getOrigin() {
        return origin;
    }

    public Integer getOverrideRegistrationOrder() {
        return overrideRegistrationOrder;
    }

    public PluralRuleAddonData getAddon() {
        return addon;
    }
}
