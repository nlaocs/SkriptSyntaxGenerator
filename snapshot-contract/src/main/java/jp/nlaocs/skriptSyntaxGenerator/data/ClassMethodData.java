package jp.nlaocs.skriptSyntaxGenerator.data;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonPropertyOrder({"name", "parameterTypes", "returnType", "static"})
public final class ClassMethodData {
    private final String name;
    private final List<String> parameterTypes;
    private final String returnType;
    private final boolean staticMethod;

    public ClassMethodData(
        String name,
        List<String> parameterTypes,
        String returnType,
        boolean staticMethod
    ) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Method name cannot be empty");
        }
        if (parameterTypes == null || returnType == null || returnType.isEmpty()) {
            throw new IllegalArgumentException("Method signature types cannot be null or empty");
        }
        for (String parameterType : parameterTypes) {
            if (parameterType == null || parameterType.isEmpty()) {
                throw new IllegalArgumentException("Method parameter types cannot be null or empty");
            }
        }
        this.name = name;
        this.parameterTypes = Collections.unmodifiableList(new ArrayList<String>(parameterTypes));
        this.returnType = returnType;
        this.staticMethod = staticMethod;
    }

    public String getName() { return name; }
    public List<String> getParameterTypes() { return parameterTypes; }
    public String getReturnType() { return returnType; }
    public boolean isStatic() { return staticMethod; }

    /** Exact JSON signature used for deterministic deduplication and ordering. */
    public String signatureKey() {
        StringBuilder result = new StringBuilder(name);
        result.append('\0');
        for (String parameterType : parameterTypes) {
            result.append(parameterType).append('\0');
        }
        result.append(returnType).append('\0').append(staticMethod ? '1' : '0');
        return result.toString();
    }
}
