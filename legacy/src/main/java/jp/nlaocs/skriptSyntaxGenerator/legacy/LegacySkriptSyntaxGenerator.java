package jp.nlaocs.skriptSyntaxGenerator.legacy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LegacySkriptSyntaxGenerator extends JavaPlugin implements CommandExecutor {
    private static final int MAX_REGISTRATION_WAIT_TICKS = 1200;

    @Override
    public void onEnable() {
        if (getCommand("skgen") != null) getCommand("skgen").setExecutor(this);
        if (Boolean.getBoolean("skriptSyntaxGenerator.integration")) waitForSkriptRegistrations(0);
        getLogger().info("SkriptSyntaxGenerator legacy adapter has been enabled!");
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] arguments
    ) {
        if (!command.getName().equalsIgnoreCase("skgen")) return false;
        sender.sendMessage("Generating Skript syntax data...");
        new LegacySnapshotGenerator(this).generate();
        sender.sendMessage("Skript syntax data generation completed!");
        return true;
    }

    private void waitForSkriptRegistrations(final int attempt) {
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (isAcceptingRegistrations()) {
                    if (attempt >= MAX_REGISTRATION_WAIT_TICKS) {
                        getLogger().severe("Timed out waiting for Skript registrations to finish.");
                        Bukkit.shutdown();
                    } else {
                        waitForSkriptRegistrations(attempt + 1);
                    }
                    return;
                }
                try {
                    getLogger().info("Starting automated legacy Skript syntax generation...");
                    new LegacySnapshotGenerator(LegacySkriptSyntaxGenerator.this).generate();
                    getLogger().info("Automated legacy Skript syntax generation completed!");
                } catch (Throwable throwable) {
                    getLogger().log(Level.SEVERE, "Automated legacy Skript syntax generation failed.", throwable);
                } finally {
                    Bukkit.shutdown();
                }
            }
        }, 1L);
    }

    private boolean isAcceptingRegistrations() {
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        if (skript == null) return true;
        Class<?> skriptClass = LegacyReflection.classOrNull(
            "ch.njol.skript.Skript", skript.getClass().getClassLoader()
        );
        Object accepting = skriptClass == null ? null :
            LegacyReflection.invokeStaticOrNull(skriptClass, "isAcceptRegistrations");
        return !Boolean.FALSE.equals(accepting);
    }
}
