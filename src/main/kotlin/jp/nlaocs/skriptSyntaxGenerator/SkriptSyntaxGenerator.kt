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
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.registration.SyntaxRegistry
import org.skriptlang.skript.util.Priority

import ch.njol.skript.doc.Name
import ch.njol.skript.doc.DocumentationId
import ch.njol.skript.doc.Since
import ch.njol.skript.doc.Description
import ch.njol.skript.doc.Example
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Keywords
import ch.njol.skript.doc.RequiredPlugins
import ch.njol.skript.doc.NoDoc
import ch.njol.skript.doc.Events
import ch.njol.skript.doc.Documentable // todo <- ?!


import java.nio.file.Paths
import java.util.Locale
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

            val eventDataList = mutableListOf<EventData>()
            for (event in events) {
                val eventData = EventData(event)
                eventDataList.add(eventData)
            }
            FileUtils.writeToFile("events.json", gson.toJson(eventDataList))

            val conditionDataList = mutableListOf<ConditionData>()
            for (condition in conditions) {
                val conditionData = ConditionData(condition)
                conditionDataList.add(conditionData)
            }
            FileUtils.writeToFile("conditions.json", gson.toJson(conditionDataList))

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

// todo 拡張関数にしてもいいかも
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
    var priority: Priority? = null
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
            noDoc = Utils.getAnnotation(s.type(), NoDoc::class.java) != null,
            // todo 特定のeventの中でしか使えなくするものだが、eventそのものなのでnullにしている。
            // 将来的には設計変えるかも、Commonにいらないかも...
            events = null,
            deprecated = Utils.getAnnotation(s.type(), Deprecated::class.java) != null,
            priority = s.priority(),
            patterns = s.patterns().toList()
        )
    }

    constructor(s: SyntaxInfo<*>) {
        val type = s.type()

        val name: String? = Utils.getAnnotationValue(type, Name::class.java)

        val examples: List<String> = when {
            type.isAnnotationPresent(Example::class.java) ->
                listOf(type.getAnnotation(Example::class.java).value)

            type.isAnnotationPresent(Example.Examples::class.java) ->
                type.getAnnotation(Example.Examples::class.java)
                    .value.map { it.value }

            type.isAnnotationPresent(Examples::class.java) ->
                type.getAnnotation(Examples::class.java)
                    .value.toList()

            else -> emptyList()
        }.map { it.replaceFirst("\\R$".toRegex(), "") }

        initCommon(
            name = name,
            id = name?.lowercase(Locale.ROOT)?.replace(" ", "_"),
            documentationId = Utils.getAnnotationValue(type, DocumentationId::class.java),
            elementClass = type,
            since = type.arrayAnno<Since>(),
            description = type.arrayAnno<Description>(),
            examples = examples,
            keywords = type.arrayAnno<Keywords>(),
            requiredPlugins = type.arrayAnno<RequiredPlugins>(),
            noDoc = type.hasAnno<NoDoc>(),
            events = type.arrayAnno<Events>(),
            deprecated = type.hasAnno<Deprecated>(),
            priority = s.priority(),
            patterns = s.patterns().toList()
        )
    }

    companion object {
        inline fun <reified T : Annotation> Class<*>.arrayAnno(): List<String>? =
            Utils.getAnnotationValue<T, Array<String>>(this, T::class.java)?.toList()

        inline fun <reified T : Annotation> Class<*>.hasAnno(): Boolean =
            Utils.getAnnotation(this, T::class.java) != null
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

class ConditionData : Common {
    constructor(s: SyntaxInfo<*>) : super(s)
}
// todo addon別
