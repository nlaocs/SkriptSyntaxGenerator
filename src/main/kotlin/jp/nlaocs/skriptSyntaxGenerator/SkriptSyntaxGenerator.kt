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

class Common {
    var name: String? = null
    var id: String? = null
    var documentationID: String? = null
    var elementClass: String? = null
    var superClass: String? = null
    var since: Array<String>? = null
    var description: Array<String>? = null
    var examples: Array<String>? = null
    var keywords: Array<String>? = null
    var requiredPlugins: Array<String>? = null
    var noDoc: Boolean = false
    var events: Array<String>? = null
    var deprecated: Boolean = false
    var priority: String? = null

    // todo list
    var patterns: Array<String> = emptyArray()

    // todo Skript-Reflectで追加したExpressionなどはどのような扱いなのか？
    var addon: AddonInfo? = null

    data class AddonInfo(
        var name: String,
        var version: String
    )

    private fun initCommon(
        name: String?,
        id: String?,
        documentationId: String?,
        elementClass: Class<*>,
        since: Array<String>?,
        description: Array<String>?,
        examples: Array<String>?,
        keywords: Array<String>?,
        requiredPlugins: Array<String>?,
        noDoc: Boolean,
        events: Array<String>?,
        deprecated: Boolean,
        priority: String?,
        patterns: Array<String>
    ) {
        this.name = name
        this.id = id
        this.documentationID = documentationId
        this.elementClass = elementClass.name
        this.superClass = elementClass.superclass?.name
        this.since = since
        this.description = description
        this.examples = examples
        this.keywords = keywords
        this.requiredPlugins = requiredPlugins
        this.noDoc = noDoc
        this.events = events
        this.deprecated = deprecated
        this.priority = priority
        this.patterns = patterns

        val providerPlugin = JavaPlugin.getProvidingPlugin(elementClass)
        this.addon = AddonInfo(
            name = providerPlugin.name,
            version = providerPlugin.description.version
        )
    }
}
