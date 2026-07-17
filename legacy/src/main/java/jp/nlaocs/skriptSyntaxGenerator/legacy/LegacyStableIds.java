package jp.nlaocs.skriptSyntaxGenerator.legacy;

import java.security.DigestOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

final class LegacyStableIds {
    private LegacyStableIds() {
    }

    static String definition(String kind, Map<String, Object> addon, Class<?> elementClass) {
        return normalize(kind) + ":" + normalize(addonName(addon)) + ":" +
            digest(encode(Arrays.asList(addonName(addon), stableName(elementClass))));
    }

    static String registration(String definitionId, List<String> patterns, int occurrence) {
        List<String> parts = new ArrayList<String>();
        parts.add(definitionId);
        parts.addAll(patterns);
        return definitionId + ":" + digest(encode(parts)) + ":" + occurrence;
    }

    static String record(String kind, Map<String, Object> addon, String... values) {
        List<String> parts = new ArrayList<String>();
        parts.add(addonName(addon));
        parts.addAll(Arrays.asList(values));
        return normalize(kind) + ":" + normalize(addonName(addon)) + ":" + digest(encode(parts));
    }

    static String contentDigest(Map<String, String> serializedOutputs) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            OutputStream sink = new OutputStream() {
                @Override
                public void write(int value) {
                }

                @Override
                public void write(byte[] buffer, int offset, int length) {
                }
            };
            Writer writer = new OutputStreamWriter(
                new DigestOutputStream(sink, messageDigest),
                StandardCharsets.UTF_8
            );
            try {
                boolean first = true;
                for (Map.Entry<String, String> entry :
                    new TreeMap<String, String>(serializedOutputs).entrySet()) {
                    if (!first) writer.write('|');
                    first = false;
                    String name = entry.getKey();
                    String json = entry.getValue();
                    writer.write(String.valueOf(name.length()));
                    writer.write(':');
                    writer.write(name);
                    writer.write(String.valueOf(json.length()));
                    writer.write(':');
                    writer.write(json);
                }
            } finally {
                writer.close();
            }
            return hex(messageDigest.digest());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String snapshotId(
        int schemaVersion,
        String contentDigest,
        String serverFingerprint,
        String language,
        List<String> pluginFingerprints,
        String capabilitiesFingerprint,
        Collection<String> files
    ) {
        List<String> sortedFiles = new ArrayList<String>(files);
        Collections.sort(sortedFiles);
        return digest(encode(Arrays.asList(
            String.valueOf(schemaVersion), contentDigest, serverFingerprint, language,
            encode(pluginFingerprints), capabilitiesFingerprint, encode(sortedFiles)
        )));
    }

    static String stableName(Class<?> type) {
        return type.isArray() ? stableName(type.getComponentType()) + "[]" : type.getName();
    }

    static String encode(Collection<String> parts) {
        List<String> encoded = new ArrayList<String>();
        for (String part : parts) encoded.add(part.length() + ":" + part);
        return join(encoded);
    }

    static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return hex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
        return result.toString();
    }

    private static String addonName(Map<String, Object> addon) {
        Object value = addon.get("name");
        return value == null ? "unknown" : String.valueOf(value);
    }

    private static String normalize(String value) {
        String normalized = value.trim().toLowerCase(Locale.ENGLISH)
            .replaceAll("[^a-z0-9._-]+", "-")
            .replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private static String join(Collection<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append('|');
            result.append(value);
        }
        return result.toString();
    }
}
