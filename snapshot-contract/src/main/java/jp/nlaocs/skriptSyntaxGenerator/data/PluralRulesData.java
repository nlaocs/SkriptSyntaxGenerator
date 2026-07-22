package jp.nlaocs.skriptSyntaxGenerator.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluralRulesData {
    private final PluralAlgorithm algorithm;
    private final boolean pluralOverrideSupported;
    private final List<PluralRuleData> rules;

    public PluralRulesData(
        PluralAlgorithm algorithm,
        boolean pluralOverrideSupported,
        List<PluralRuleData> rules
    ) {
        if (algorithm == null || rules == null) {
            throw new IllegalArgumentException("Plural rule snapshot fields cannot be null");
        }
        this.algorithm = algorithm;
        this.pluralOverrideSupported = pluralOverrideSupported;
        this.rules = Collections.unmodifiableList(new ArrayList<PluralRuleData>(rules));
    }

    public PluralAlgorithm getAlgorithm() {
        return algorithm;
    }

    public boolean isPluralOverrideSupported() {
        return pluralOverrideSupported;
    }

    public List<PluralRuleData> getRules() {
        return rules;
    }
}
