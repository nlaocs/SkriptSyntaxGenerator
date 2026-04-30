package jp.nlaocs.skriptSyntaxGenerator.hook;

public final class HookLogOptions {

    private static final String SYSTEM_PROPERTY_KEY = "skriptSyntaxGenerator.hookLog";
    private static final String COMMAND_LINE_FLAG = "--hook-log";
    private static final boolean ENABLED = resolveEnabled();

    private HookLogOptions() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    private static boolean resolveEnabled() {
        String propertyValue = System.getProperty(SYSTEM_PROPERTY_KEY);
        if (propertyValue != null) {
            return Boolean.parseBoolean(propertyValue);
        }

        String command = System.getProperty("sun.java.command", "");
        if (command.isBlank()) {
            return false;
        }

        for (String token : command.split("\\s+")) {
            if (COMMAND_LINE_FLAG.equals(token)) {
                return true;
            }

            if (token.startsWith(COMMAND_LINE_FLAG + "=")) {
                return Boolean.parseBoolean(token.substring(COMMAND_LINE_FLAG.length() + 1));
            }
        }

        return false;
    }
}

