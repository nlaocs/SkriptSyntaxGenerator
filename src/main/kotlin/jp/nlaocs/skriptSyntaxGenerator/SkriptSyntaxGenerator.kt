package jp.nlaocs.skriptSyntaxGenerator

import ch.njol.skript.Skript
import ch.njol.skript.lang.function.Functions
import ch.njol.skript.registrations.Classes
import ch.njol.skript.registrations.EventValues
import com.google.gson.GsonBuilder
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxRegistry
import org.skriptlang.skript.util.Priority

import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

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

            val gson = GsonBuilder()
                .registerTypeAdapter(
                    Class::class.java,
                    com.google.gson.JsonSerializer<Class<*>> { src, typeOfSrc, context ->
                        com.google.gson.JsonPrimitive(src.name)
                    })
                .serializeNulls()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()

            val registry: SyntaxRegistry = Skript.instance().syntaxRegistry()

            val events = registry.syntaxes(BukkitSyntaxInfos.Event.KEY)
            val conditions = registry.syntaxes(SyntaxRegistry.CONDITION)
            val effects = registry.syntaxes(SyntaxRegistry.EFFECT)
            val expressions = registry.syntaxes(SyntaxRegistry.EXPRESSION)
            val types = Classes.getClassInfos()
            val functions = Functions.getFunctions()
            val sections = registry.syntaxes(SyntaxRegistry.SECTION)
            val structures = registry.syntaxes(SyntaxRegistry.STRUCTURE)

            val eventDataList = mutableListOf<Common>()
            for (event in events) {
                val common = Common(event)
                val eventData = EventData(event)
                eventDataList.add(eventData)
            }
            FileUtils.writeToFile("events.json", gson.toJson(eventDataList))

            sender.sendMessage("Skript syntax data generation completed!")
            return true
        }

        return false
    }
}

object FileUtils {
    @JvmStatic
    fun writeToFile(fileName: String, content: String) {
        val dirPath = Paths.get("plugins", "SkriptSyntaxGenerator")
        val filePath = dirPath.resolve(fileName)

        dirPath.createDirectories()
        filePath.writeText(content)
    }
}

class Utils {
    companion object {
        @JvmStatic
        fun <T : Annotation> getAnnotation(clazz: Class<*>, annotationClass: Class<T>): T? {
            return if (clazz.isAnnotationPresent(annotationClass)) {
                clazz.getAnnotation(annotationClass)
            } else {
                null
            }
        }

        @JvmStatic
        fun <T : Annotation, V> getAnnotationValue(clazz: Class<*>, annotationClass: Class<T>): V? {
            val annotation = getAnnotation(clazz, annotationClass)
            return if (annotation != null) {
                try {
                    val value = annotationClass.getMethod("value").invoke(annotation)
                    @Suppress("UNCHECKED_CAST")
                    value as V
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        }
    }
}

open class Common {
    var name: String? = null
    var id: String? = null
    var documentationId: String? = null
    var elementClass: Class<*> = Any::class.java
    var superClass: Class<*>? = null
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
        this.elementClass = elementClass
        this.superClass = elementClass.superclass
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
            noDoc = Utils.getAnnotation(s.type(), ch.njol.skript.doc.NoDoc::class.java) != null,
            // todo 特定のeventの中でしか使えなくするものだが、eventそのものなのでnullにしている。
            // 将来的には設計変えるかも、Commonにいらないかも...
            events = null,
            deprecated = Utils.getAnnotation(s.type(), Deprecated::class.java) != null,
            priority = s.priority(),
            patterns = s.patterns().toList()
        )
    }
}

class EventData : Common {
    var referenceEvents: List<Class<out Event>>? = null
    var eventValues: List<EventValues.EventValueInfo<*, *>> = emptyList()
    var cancellable: Boolean = false
    var hasOnPrefix: Boolean = false

    constructor(s: BukkitSyntaxInfos.Event<*>) : super(s) {

        this.referenceEvents = s.events().toList()
        val allEventValues = EventValues.getPerEventEventValues()
        val eventValueList = mutableListOf<EventValues.EventValueInfo<*, *>>()
        for ((eventClass, info) in allEventValues.entries()) {
            for (refEvent in referenceEvents ?: emptyList()) {
                if (eventClass.isAssignableFrom(refEvent)) {
                    eventValueList.add(info)
                }
            }

        }
        this.eventValues = eventValueList

        this.cancellable = true
        for (it in referenceEvents ?: emptyList()) {
            if (!Cancellable::class.java.isAssignableFrom(it)) {
                this.cancellable = false
                break
            }
        }

        this.hasOnPrefix = s.name().startsWith("On ")
    }
}