package jp.nlaocs.skriptSyntaxGenerator.data;

public final class SyntaxKindCapabilitiesData {
    private final boolean conditions;
    private final boolean effects;
    private final boolean events;
    private final boolean expressions;
    private final boolean types;
    private final boolean functions;
    private final boolean sections;
    private final boolean structures;
    private final boolean properties;
    private final boolean arithmetic;
    private final boolean converters;
    private final boolean comparators;
    private final boolean eventValues;

    public SyntaxKindCapabilitiesData(
        boolean conditions,
        boolean effects,
        boolean events,
        boolean expressions,
        boolean types,
        boolean functions,
        boolean sections,
        boolean structures,
        boolean properties,
        boolean arithmetic,
        boolean converters,
        boolean comparators,
        boolean eventValues
    ) {
        this.conditions = conditions;
        this.effects = effects;
        this.events = events;
        this.expressions = expressions;
        this.types = types;
        this.functions = functions;
        this.sections = sections;
        this.structures = structures;
        this.properties = properties;
        this.arithmetic = arithmetic;
        this.converters = converters;
        this.comparators = comparators;
        this.eventValues = eventValues;
    }

    public static SyntaxKindCapabilitiesData modern() {
        return new SyntaxKindCapabilitiesData(
            true, true, true, true, true, true, true,
            true, true, true, true, true, true
        );
    }

    public boolean isConditions() { return conditions; }
    public boolean isEffects() { return effects; }
    public boolean isEvents() { return events; }
    public boolean isExpressions() { return expressions; }
    public boolean isTypes() { return types; }
    public boolean isFunctions() { return functions; }
    public boolean isSections() { return sections; }
    public boolean isStructures() { return structures; }
    public boolean isProperties() { return properties; }
    public boolean isArithmetic() { return arithmetic; }
    public boolean isConverters() { return converters; }
    public boolean isComparators() { return comparators; }
    public boolean isEventValues() { return eventValues; }

    public String fingerprint() {
        StringBuilder result = new StringBuilder(13);
        boolean[] values = {
            conditions, effects, events, expressions, types, functions, sections,
            structures, properties, arithmetic, converters, comparators, eventValues
        };
        for (boolean value : values) result.append(value ? '1' : '0');
        return result.toString();
    }
}
