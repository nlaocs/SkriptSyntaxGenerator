package jp.nlaocs.skriptSyntaxGenerator

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.classes.ClassInfo
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
import ch.njol.skript.expressions.base.EventValueExpression
import ch.njol.skript.expressions.base.PropertyExpression
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.DefaultExpression
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Section
import ch.njol.skript.localization.Noun
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonDeserializationContext

import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.registration.DefaultSyntaxInfos

import java.nio.file.Paths
import java.util.Locale
import java.util.function.Supplier
import java.util.regex.Pattern
import java.lang.reflect.Type
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
                    com.google.gson.JsonSerializer<Class<*>> { src, _, _ ->
                        com.google.gson.JsonPrimitive(src.name)
                    })
                .registerTypeAdapter(Pattern::class.java, PatternAdapter())
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

            val effectDataList = mutableListOf<EffectData>()
            for (effect in effects) {
                val effectData = EffectData(effect)
                effectDataList.add(effectData)
            }
            FileUtils.writeToFile("effects.json", gson.toJson(effectDataList))

            val expressionDataList = mutableListOf<ExpressionData>()
            for (expression in expressions) {
                val expressionData = ExpressionData(expression)
                expressionDataList.add(expressionData)
            }
            FileUtils.writeToFile("expressions.json", gson.toJson(expressionDataList))

            val typeDataList = mutableListOf<TypeData>()
            for (type in types) {
                val typeData = TypeData(type)
                typeDataList.add(typeData)
            }
            FileUtils.writeToFile("types.json", gson.toJson(typeDataList))

            // functions

            val sectionDataList = mutableListOf<SectionData>()
            for (section in sections) {
                val sectionData = SectionData(section)
                sectionDataList.add(sectionData)
            }
            FileUtils.writeToFile("sections.json", gson.toJson(sectionDataList))

            val structureDataList = mutableListOf<StructureData>()
            for (structure in structures) {
                val structureData = StructureData(structure)
                structureDataList.add(structureData)
            }
            FileUtils.writeToFile("structures.json", gson.toJson(structureDataList))


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

class PatternAdapter : JsonSerializer<Pattern>, JsonDeserializer<Pattern> {

    override fun serialize(
        src: Pattern,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.pattern())
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Pattern {
        return Pattern.compile(json.asString)
    }
}

inline fun <reified T : Annotation> Class<*>.anno(): T? =
    getAnnotation(T::class.java)

inline fun <reified T : Annotation> Class<*>.hasAnno(): Boolean =
    isAnnotationPresent(T::class.java)

inline fun <reified T : Annotation, reified V> Class<*>.annoValue(method: String = "value"): V? =
    anno<T>()?.let { ann ->
        try {
            T::class.java.getMethod(method).invoke(ann) as? V
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
            null
        }
    }

inline fun <reified T : Annotation, reified V> Class<*>.annoValues(method: String = "value"): List<V>? =
    annoValue<T, Array<V>>(method)?.toList()

fun Class<*>.getTypeStr(): String = when {
    isAnnotation -> "Annotation"
    isEnum -> "Enum"
    isInterface -> "Interface"
    isArray -> "Array"
    isPrimitive -> "Primitive"
    isRecord -> "Record"
    isSealed -> "Sealed"
    isSynthetic -> "Synthetic"
    isMemberClass -> "MemberClass"
    isLocalClass -> "LocalClass"
    isAnonymousClass -> "AnonymousClass"
    else -> "Class"
}

fun Class<*>.toStringListSafe(): List<String> {
    if (!isEnum) return emptyList()
    return (enumConstants as Array<Enum<*>>).map { constant ->
        constant.name
            .lowercase(Locale.ENGLISH)
            .replace('_', ' ')
    }
}

fun Priority?.toPriorityStr(): String? = when (this) {
    null -> null
    SyntaxInfo.SIMPLE -> "SyntaxInfos.SIMPLE" // = ExpressionType.SIMPLE
    SyntaxInfo.COMBINED -> "SyntaxInfos.COMBINED" // = ExpressionType.COMBINED
    SyntaxInfo.PATTERN_MATCHES_EVERYTHING -> "SyntaxInfos.PATTERN_MATCHES_EVERYTHING" // = ExpressionType.PATTERN_MATCHES_EVERYTHING
    EventValueExpression.DEFAULT_PRIORITY -> "EventValueExpression.DEFAULT_PRIORITY" // = ExpressionType.EVENT
    PropertyExpression.DEFAULT_PRIORITY -> "PropertyExpression.DEFAULT_PRIORITY" // = ExpressionType.PROPERTY
    else -> "CUSTOM"
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
    var priorityStr: String? = null
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
        this.priorityStr = priority.toPriorityStr()
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
            noDoc = s.type().hasAnno<NoDoc>(),
            // todo 特定のeventの中でしか使えなくするものだが、eventそのものなのでnullにしている。
            // 将来的には設計変えるかも、Commonにいらないかも...
            events = null,
            deprecated = s.type().hasAnno<Deprecated>(),
            priority = s.priority(),
            patterns = s.patterns().toList()
        )
    }

    constructor(s: SyntaxInfo<*>) {
        val type = s.type()

        val name: String? = type.annoValue<Name, String>()

        val examples: List<String> = type.anno<Example>()?.let { listOf(it.value) }
            ?: type.anno<Example.Examples>()?.let { it.value.map { ex -> ex.value } }
            ?: type.anno<Examples>()?.value?.toList()
            ?: emptyList()

        initCommon(
            name = name,
            id = name?.lowercase(Locale.ROOT)?.replace(" ", "_"),
            documentationId = type.annoValue<DocumentationId, String>(),
            elementClass = type,
            since = type.annoValues<Since, String>(),
            description = type.annoValues<Description, String>(),
            examples = examples,
            keywords = type.annoValues<Keywords, String>(),
            requiredPlugins = type.annoValues<RequiredPlugins, String>(),
            noDoc = type.hasAnno<NoDoc>(),
            events = type.annoValues<Events, String>(),
            deprecated = type.hasAnno<Deprecated>(),
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

class ConditionData : Common {
    constructor(s: SyntaxInfo<out Condition>) : super(s)
}

class EffectData : Common {
    constructor(s: SyntaxInfo<out Effect>) : super(s)
}

class ExpressionData : Common {
    var returnType: Class<*>? = null

    constructor(s: DefaultSyntaxInfos.Expression<*, *>) : super(s) {
        this.returnType = s.returnType()
    }
}

class TypeData {
    var originalClass: Class<*>? = null
    var classType: String? = null
    var codeName: String? = null
    var superClass: Class<*>? = null
    var name: Noun? = null

    //var defaultExpression: DefaultExpression<*>? = null
    var defaultExpressionClass: Class<out DefaultExpression<*>>? = null

    //var parser: Parser<*>? = null
    var parserClass: Class<*>? = null
    //var cloner: Cloner<*>? = null

    //var userInputPatterns: List<String>? = null
    var userInputPatterns: List<Pattern>? = null

    //var changer: Changer<*>? = null
    var changerClass: Class<out Changer<*>>? = null
    var supplier: Supplier<*>? = null
    var supplierClass: Class<*>? = null

    //var serializer: Serializer<*>? = null // stackoverflow
    var serializerClass: Class<*>? = null
    var serializeAs: Class<*>? = null

//    var before: List<String>? = emptyList()
//    var after: List<String>? = emptyList()

    //var mathRelativeType: Class<*>? = null
    var docName: String? = null
    var description: List<String?>? = null
    var usage: List<String?>? = null
    var examples: List<String?>? = null
    var since: String? = null
    var requiredPlugins: List<String?>? = null
    //var properties: List<Property<*>>? = null // 不安定 stackoverflow
    //var propertyInfos: Map<Property<*>, Property.PropertyInfo<*>>? = null // 不安定
    //var propertyDocumentation: Map<Property<*>, ClassInfo.PropertyDocs>? = null // 不安定
    //var propertyDocumentation: ClassInfo.PropertyDocs? = null

    constructor(s: ClassInfo<*>) {
        this.originalClass = s.c
        this.classType = s.c.getTypeStr()
        this.codeName = s.codeName
        this.superClass = s.c.superclass
        this.name = s.name
        //this.defaultExpression = s.defaultExpressin
        this.defaultExpressionClass = s.defaultExpression?.javaClass
        //this.parser = s.parser
        this.parserClass = s.parser?.javaClass
        //this.cloner = s.cloner
        this.userInputPatterns = s.userInputPatterns?.toList()
        /*val userInputPatterns = s.userInputPatterns
        val userInputPatternsStr: List<String>? = userInputPatterns?.map { it.pattern().toString() }
        this.userInputPatterns = userInputPatternsStr*/
        //this.changer = s.changer
        this.changerClass = s.changer?.javaClass
        //this.supplier = s.supplier
        this.supplierClass = s.supplier?.javaClass
        //this.serializer = s.serializer
        this.serializerClass = s.serializer?.javaClass
        this.serializeAs = s.serializeAs
        //this.before = s.before()?.toList()
        //this.after = s.after()?.toList()
        /*val field = s.javaClass.getDeclaredField("mathRelativeType")
        field.isAccessible = true
        val mathRelativeTypeValue = field.get(s) as? Class<*>
        this.mathRelativeType = mathRelativeTypeValue*/
        this.docName = s.docName
        this.description = s.description?.toList()
        //this.usage = s.usage?.toList()
        //if (s.c.getTypeStr() == "Enum") {
        this.usage = s.c.toStringListSafe().ifEmpty { s.usage?.toList() }
        this.examples = s.examples?.toList()
        this.since = s.since
        this.requiredPlugins = s.requiredPlugins?.toList()
        //this.properties = s.allProperties.toList()
        //this.propertyInfos = s.getPropertyInfos()
        //this.propertyDocumentation = s.getPropertyDocumentation()

    }
}

// todo function

class SectionData : Common {
    constructor(s: SyntaxInfo<out Section>) : super(s)
}

class StructureData : Common {
    var entryValidator: EntryValidator? = null
    var nodeType: DefaultSyntaxInfos.Structure.NodeType? = null

    constructor(s: DefaultSyntaxInfos.Structure<*>) : super(s) {
        this.entryValidator = s.entryValidator()
        this.nodeType = s.nodeType()
    }
}

// todo addon別
// todo json順序を逆に
// todo Expressionに、get可能やset可能などの情報も追加する
// usageを文字連結でのリストではなく配列に
// ""しかないusageはnullに
// typeには解析順序がある