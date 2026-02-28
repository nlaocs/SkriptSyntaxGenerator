package jp.nlaocs.skriptSyntaxGenerator

import ch.njol.skript.Skript
import ch.njol.skript.lang.function.Functions
import ch.njol.skript.registrations.Classes
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry
import org.skriptlang.skript.util.Priority

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
    var documentationId: String? = null
    var elementClass: String? = null
    var superClass: String? = null
    var since: List<String>? = null
    var description: List<String>? = null
    var examples: List<String>? = null
    var keywords: List<String>? = null
    var requiredPlugins: List<String>? = null
    var noDoc: Boolean = false
    var events: List<String>? = null
    var deprecated: Boolean = false

    //var priority: String? = null
    var priority: Priority? = null

    // todo list
    var patterns: List<String> = emptyList()

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
        since: List<String>?,
        description: List<String>?,
        examples: List<String>?,
        keywords: List<String>?,
        requiredPlugins: List<String>?,
        noDoc: Boolean,
        events: List<String>?,
        deprecated: Boolean,
        priority: Priority?,
        patterns: List<String>
    ) {
        this.name = name
        this.id = id
        this.documentationId = documentationId
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

    constructor(s: BukkitSyntaxInfos.Event<*>) {
        initCommon(
            name = s.name(),
            id = s.id(),
            documentationId = s.documentationId(),
            elementClass = s.type(),
            since = s.since().toList(),
            description = s.description().toList(),
            examples = s.examples().toList(),
            keywords = s.keywords().toList(),
            requiredPlugins = s.requiredPlugins().toList(),
            noDoc = false, // todo
            // todo 特定のeventの中でしか使えなくするものだが、eventそのものなのでnullにしている。
            // 将来的には設計変えるかも
            events = null,
            deprecated = false, // todo
            priority = s.priority(),
            patterns = s.patterns().toList()
        )
    }
}
