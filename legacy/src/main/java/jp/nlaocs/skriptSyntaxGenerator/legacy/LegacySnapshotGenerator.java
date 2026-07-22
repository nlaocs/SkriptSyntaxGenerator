package jp.nlaocs.skriptSyntaxGenerator.legacy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jp.nlaocs.skriptSyntaxGenerator.data.AliasesCapabilitiesData;
import jp.nlaocs.skriptSyntaxGenerator.data.EventValueApi;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralOverrideRegistration;
import jp.nlaocs.skriptSyntaxGenerator.data.PluralRuleAddonData;
import jp.nlaocs.skriptSyntaxGenerator.data.SnapshotCapabilitiesData;
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxApi;
import jp.nlaocs.skriptSyntaxGenerator.data.SyntaxKindCapabilitiesData;
import jp.nlaocs.skriptSyntaxGenerator.generator.GlobalAliasesReader;
import jp.nlaocs.skriptSyntaxGenerator.generator.PluralRulesReader;
import jp.nlaocs.skriptSyntaxGenerator.generator.SnapshotFormat;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LegacySnapshotGenerator {
    private static final String OUTPUT_DIRECTORY_PROPERTY =
        "skriptSyntaxGenerator.outputDirectory";

    private final ClassLoader skriptClassLoader;
    private final Class<?> skriptClass;
    private final ObjectMapper objectMapper;

    LegacySnapshotGenerator(JavaPlugin plugin) {
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        if (skript == null) throw new IllegalStateException("Skript is not installed");
        this.skriptClassLoader = skript.getClass().getClassLoader();
        this.skriptClass = requireClass("ch.njol.skript.Skript");
        this.objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    void generate() {
        LegacyAddonResolver addonResolver = new LegacyAddonResolver(skriptClassLoader);
        LegacyClassHierarchy hierarchy = new LegacyClassHierarchy(addonResolver);
        LegacySyntaxCollector syntax = new LegacySyntaxCollector(
            skriptClass, skriptClassLoader, addonResolver, hierarchy
        );
        LegacyRegistryCollector registries = new LegacyRegistryCollector(
            skriptClassLoader, addonResolver, hierarchy
        );
        LegacyArithmeticCollector arithmetic = new LegacyArithmeticCollector(
            skriptClassLoader, addonResolver, hierarchy
        );
        LegacyPropertyCollector properties = new LegacyPropertyCollector(
            skriptClassLoader, addonResolver, hierarchy
        );

        List<LegacyEventValueRecord> eventValueRecords = syntax.collectEventValues();
        Map<String, Object> outputs = new LinkedHashMap<String, Object>();
        outputs.put("Conditions.json", syntax.collectBasic("getConditions", "condition"));
        outputs.put("Effects.json", syntax.collectBasic("getEffects", "effect"));
        outputs.put("Events.json", syntax.collectEvents(eventValueRecords));
        outputs.put("Expressions.json", syntax.collectBasic("getExpressions", "expression"));
        outputs.put("Sections.json", syntax.collectBasic("getSections", "section"));
        outputs.put("Structures.json", syntax.collectBasic("getStructures", "structure"));
        outputs.put("Types.json", registries.collectTypes());
        outputs.put("Functions.json", registries.collectFunctions());
        outputs.put("Converters.json", registries.collectConverters());
        outputs.put("Comparators.json", registries.collectComparators());
        outputs.put("EventValues.json", eventValueData(eventValueRecords));
        outputs.put("Properties.json", properties.collect());
        outputs.put("Differences.json", arithmetic.collectDifferences());
        outputs.put("Operators.json", arithmetic.collectOperators());
        outputs.put("Operations.json", arithmetic.collectOperations());
        outputs.put("ClassHierarchy.json", hierarchy.toData());
        outputs.put(SnapshotFormat.ALIASES_FILE, GlobalAliasesReader.read(skriptClassLoader));
        Plugin skriptPlugin = Bukkit.getPluginManager().getPlugin("Skript");
        if (skriptPlugin == null) throw new IllegalStateException("Skript is not installed");
        outputs.put(
            SnapshotFormat.PLURAL_RULES_FILE,
            PluralRulesReader.read(
                skriptClassLoader,
                new PluralRuleAddonData(
                    skriptPlugin.getName(),
                    skriptPlugin.getDescription().getVersion()
                ),
                Collections.<PluralOverrideRegistration>emptyList()
            )
        );

        Map<String, Object> normalized = SnapshotFormat.normalize(outputs);
        Map<String, String> serializedOutputs = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> entry : normalized.entrySet()) {
            serializedOutputs.put(entry.getKey(), writeJson(entry.getValue()));
        }
        String contentDigest = LegacyStableIds.contentDigest(serializedOutputs);
        SnapshotCapabilitiesData capabilities = capabilities();
        Map<String, Object> manifest = manifest(contentDigest, capabilities);

        for (Map.Entry<String, String> entry : serializedOutputs.entrySet()) {
            writeFile(entry.getKey(), entry.getValue());
        }
        writeFile(SnapshotFormat.MANIFEST_FILE, writeJson(manifest));
    }

    private SnapshotCapabilitiesData capabilities() {
        boolean hasSections = LegacyReflection.hasMethod(skriptClass, "getSections", 0);
        boolean hasStructures = LegacyReflection.hasMethod(skriptClass, "getStructures", 0);
        boolean hasTypes = LegacyReflection.hasClass("ch.njol.skript.registrations.Classes", skriptClassLoader);
        boolean hasFunctions = LegacyReflection.hasClass("ch.njol.skript.lang.function.Functions", skriptClassLoader);
        boolean hasConverters =
            LegacyReflection.hasClass("org.skriptlang.skript.lang.converter.Converters", skriptClassLoader) ||
            LegacyReflection.hasClass("ch.njol.skript.registrations.Converters", skriptClassLoader);
        boolean hasComparators =
            LegacyReflection.hasClass("org.skriptlang.skript.lang.comparator.Comparators", skriptClassLoader) ||
            LegacyReflection.hasClass("ch.njol.skript.registrations.Comparators", skriptClassLoader);
        boolean hasArithmetic = LegacyReflection.hasClass(
            "org.skriptlang.skript.lang.arithmetic.Arithmetics", skriptClassLoader
        );
        boolean hasProperties = LegacyReflection.hasClass(
            "org.skriptlang.skript.lang.properties.Property", skriptClassLoader
        );
        boolean hasEventValues = LegacyReflection.hasClass("ch.njol.skript.registrations.EventValues", skriptClassLoader);
        SyntaxKindCapabilitiesData syntaxKinds = new SyntaxKindCapabilitiesData(
            LegacyReflection.hasMethod(skriptClass, "getConditions", 0),
            LegacyReflection.hasMethod(skriptClass, "getEffects", 0),
            LegacyReflection.hasMethod(skriptClass, "getEvents", 0),
            LegacyReflection.hasMethod(skriptClass, "getExpressions", 0),
            hasTypes, hasFunctions, hasSections, hasStructures,
            hasProperties, hasArithmetic, hasConverters, hasComparators, hasEventValues
        );

        boolean aliasesSupported = GlobalAliasesReader.isSupported(skriptClassLoader);
        return new SnapshotCapabilitiesData(
            SyntaxApi.LEGACY_STATIC,
            EventValueApi.LEGACY,
            syntaxKinds,
            new AliasesCapabilitiesData(aliasesSupported, aliasesSupported)
        );
    }

    private Map<String, Object> manifest(String contentDigest, SnapshotCapabilitiesData capabilities) {
        Map<String, Object> server = serverData();
        List<Map<String, Object>> plugins = pluginData();
        List<String> pluginFingerprints = new ArrayList<String>();
        for (Map<String, Object> pluginData : plugins) {
            pluginFingerprints.add(pluginFingerprint(pluginData));
        }
        String language = language();
        List<String> files = SnapshotFormat.getAllFiles();
        String snapshotId = LegacyStableIds.snapshotId(
            SnapshotFormat.SCHEMA_VERSION,
            contentDigest,
            serverFingerprint(server),
            language,
            pluginFingerprints,
            capabilities.fingerprint(),
            files
        );

        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("schemaVersion", SnapshotFormat.SCHEMA_VERSION);
        manifest.put("snapshotId", snapshotId);
        manifest.put("contentDigest", contentDigest);
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("server", server);
        manifest.put("language", language);
        manifest.put("plugins", plugins);
        manifest.put("capabilities", capabilities);
        manifest.put("files", files);
        return manifest;
    }

    private Map<String, Object> serverData() {
        Map<String, Object> server = new LinkedHashMap<String, Object>();
        server.put("name", Bukkit.getName());
        server.put("version", Bukkit.getVersion());
        server.put("bukkitVersion", Bukkit.getBukkitVersion());
        Object minecraftVersion = LegacyReflection.invokeStaticOrNull(skriptClass, "getMinecraftVersion");
        server.put(
            "minecraftVersion",
            minecraftVersion == null ? Bukkit.getBukkitVersion().split("-")[0] : String.valueOf(minecraftVersion)
        );
        server.put("javaVersion", System.getProperty("java.version"));
        return server;
    }

    private String language() {
        Class<?> languageClass = LegacyReflection.classOrNull(
            "ch.njol.skript.localization.Language", skriptClassLoader
        );
        Object name = languageClass == null ? null :
            LegacyReflection.invokeStaticOrNull(languageClass, "getName");
        return name == null ? "unknown" : String.valueOf(name);
    }

    private List<Map<String, Object>> pluginData() {
        Plugin[] installedPlugins = Bukkit.getPluginManager().getPlugins();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < installedPlugins.length; index++) {
            Plugin installedPlugin = installedPlugins[index];
            PluginDescriptionFile description = installedPlugin.getDescription();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("loadOrder", index);
            data.put("name", installedPlugin.getName());
            data.put("version", description.getVersion());
            data.put("main", description.getMain());
            data.put("enabled", installedPlugin.isEnabled());
            data.put("depend", strings(LegacyReflection.invokeOrNull(description, "getDepend")));
            data.put("softDepend", strings(LegacyReflection.invokeOrNull(description, "getSoftDepend")));
            data.put("loadBefore", strings(LegacyReflection.invokeOrNull(description, "getLoadBefore")));
            String jarHash = hashPluginJar(installedPlugin);
            if (jarHash != null) data.put("jarSha256", jarHash);
            result.add(data);
        }
        return result;
    }

    private String hashPluginJar(Plugin target) {
        try {
            URI location = target.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(location);
            if (!Files.isRegularFile(path)) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream input = Files.newInputStream(path);
            try {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            } finally {
                input.close();
            }
            StringBuilder result = new StringBuilder();
            for (byte item : digest.digest()) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String serverFingerprint(Map<String, Object> server) {
        List<String> parts = new ArrayList<String>();
        parts.add(String.valueOf(server.get("name")));
        parts.add(String.valueOf(server.get("version")));
        parts.add(String.valueOf(server.get("bukkitVersion")));
        parts.add(String.valueOf(server.get("minecraftVersion")));
        parts.add(String.valueOf(server.get("javaVersion")));
        return LegacyStableIds.encode(parts);
    }

    private String pluginFingerprint(Map<String, Object> data) {
        List<String> parts = new ArrayList<String>();
        parts.add(String.valueOf(data.get("loadOrder")));
        parts.add(String.valueOf(data.get("name")));
        parts.add(String.valueOf(data.get("version")));
        parts.add(String.valueOf(data.get("main")));
        parts.add(String.valueOf(data.get("enabled")));
        parts.add(join(strings(data.get("depend")), ","));
        parts.add(join(strings(data.get("softDepend")), ","));
        parts.add(join(strings(data.get("loadBefore")), ","));
        parts.add(data.get("jarSha256") == null ? "" : String.valueOf(data.get("jarSha256")));
        return LegacyStableIds.encode(parts);
    }

    private List<Map<String, Object>> eventValueData(List<LegacyEventValueRecord> records) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (LegacyEventValueRecord record : records) result.add(record.data);
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize snapshot data", exception);
        }
    }

    private void writeFile(String fileName, String content) {
        try {
            String configured = System.getProperty(OUTPUT_DIRECTORY_PROPERTY);
            Path directory = configured == null || configured.trim().isEmpty()
                ? Paths.get("plugins", "SkriptSyntaxGenerator")
                : Paths.get(configured);
            Files.createDirectories(directory);
            Files.write(directory.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot write " + fileName, exception);
        }
    }

    private Class<?> requireClass(String name) {
        Class<?> type = LegacyReflection.classOrNull(name, skriptClassLoader);
        if (type == null) throw new IllegalStateException("Required Skript class is missing: " + name);
        return type;
    }

    private static List<String> strings(Object value) {
        return new ArrayList<String>(LegacyReflection.strings(value));
    }

    private static String join(Collection<String> values, String delimiter) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(delimiter);
            result.append(value);
        }
        return result.toString();
    }
}
