package jp.nlaocs.skriptSyntaxGenerator.data;

public final class PluralOverrideRegistration {
    private final String singular;
    private final String plural;
    private final int registrationOrder;
    private final PluralRuleAddonData addon;

    public PluralOverrideRegistration(
        String singular,
        String plural,
        int registrationOrder,
        PluralRuleAddonData addon
    ) {
        if (singular == null || plural == null || registrationOrder < 0) {
            throw new IllegalArgumentException("Plural override fields are invalid");
        }
        this.singular = singular;
        this.plural = plural;
        this.registrationOrder = registrationOrder;
        this.addon = addon;
    }

    public String getSingular() {
        return singular;
    }

    public String getPlural() {
        return plural;
    }

    public int getRegistrationOrder() {
        return registrationOrder;
    }

    public PluralRuleAddonData getAddon() {
        return addon;
    }
}
