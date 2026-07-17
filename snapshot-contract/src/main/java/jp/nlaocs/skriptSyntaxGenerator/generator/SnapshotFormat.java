package jp.nlaocs.skriptSyntaxGenerator.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SnapshotFormat {
    public static final int SCHEMA_VERSION = 2;
    public static final String MANIFEST_FILE = "Manifest.json";
    public static final String ALIASES_FILE = "Aliases.json";
    public static final String OPERATIONS_FILE = "Operations.json";

    private static final List<String> DATA_FILES = Collections.unmodifiableList(Arrays.asList(
        ALIASES_FILE, "ClassHierarchy.json", "Comparators.json", "Conditions.json", "Converters.json",
        "Differences.json", "Effects.json", "EventValues.json", "Events.json",
        "Expressions.json", "Functions.json", OPERATIONS_FILE, "Operators.json",
        "Properties.json", "Sections.json", "Structures.json", "Types.json"
    ));
    private static final List<String> ALL_FILES;

    static {
        List<String> allFiles = new ArrayList<String>(DATA_FILES);
        allFiles.add(MANIFEST_FILE);
        Collections.sort(allFiles);
        ALL_FILES = Collections.unmodifiableList(allFiles);
    }

    private SnapshotFormat() {
    }

    public static List<String> getDataFiles() { return DATA_FILES; }
    public static List<String> getAllFiles() { return ALL_FILES; }

    public static LinkedHashMap<String, Object> normalize(Map<String, ?> outputs) {
        Set<String> unexpected = new LinkedHashSet<String>(outputs.keySet());
        unexpected.removeAll(DATA_FILES);
        if (!unexpected.isEmpty()) {
            throw new IllegalArgumentException("Unexpected snapshot outputs: " + unexpected);
        }

        LinkedHashMap<String, Object> normalized = new LinkedHashMap<String, Object>();
        for (String fileName : DATA_FILES) {
            Object value = outputs.get(fileName);
            normalized.put(fileName, value == null ? emptyRoot(fileName) : value);
        }
        return normalized;
    }

    private static Object emptyRoot(String fileName) {
        if (ALIASES_FILE.equals(fileName)) {
            Map<String, Object> aliases = new LinkedHashMap<String, Object>();
            aliases.put("aliases", Collections.emptyMap());
            aliases.put("targets", Collections.emptyList());
            return aliases;
        }
        return OPERATIONS_FILE.equals(fileName) ? Collections.emptyMap() : Collections.emptyList();
    }
}
