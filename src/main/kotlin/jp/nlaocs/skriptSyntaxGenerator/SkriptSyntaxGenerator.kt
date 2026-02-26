package jp.nlaocs.skriptSyntaxGenerator

import ch.njol.skript.Skript
import ch.njol.skript.lang.function.Functions
import ch.njol.skript.registrations.Classes
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry

class SkriptSyntaxGenerator : JavaPlugin() {

    override fun onEnable() {
        this.getCommand("skgen")?.setExecutor(SkriptSyntaxCommandExecutor())
        logger.info("SkriptSyntaxGenerator has been enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class SkriptSyntaxCommandExecutor : org.bukkit.command.CommandExecutor {
    override fun onCommand(
        sender: org.bukkit.command.CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("skgen", ignoreCase = true)) {
            sender.sendMessage("Generating Skript syntax data...")
            val registry: SyntaxRegistry = Skript.instance().syntaxRegistry()

            val events = registry.syntaxes(BukkitSyntaxInfos.Event.KEY)
            val conditions = registry.syntaxes(SyntaxRegistry.CONDITION)
            val effects = registry.syntaxes(SyntaxRegistry.EFFECT)
            val expressions = registry.syntaxes(SyntaxRegistry.EXPRESSION)
            val types = Classes.getClassInfos()
            val functions = Functions.getFunctions()
            val sections = registry.syntaxes(SyntaxRegistry.SECTION)
            val structures = registry.syntaxes(SyntaxRegistry.STRUCTURE)

            sender.sendMessage("Skript syntax data generation completed!")
            return true
        }

        return false
    }
}