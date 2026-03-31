package jp.nlaocs.skriptSyntaxGenerator

import ch.njol.skript.Skript
import ch.njol.skript.classes.Changer
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.function.Functions
import ch.njol.skript.registrations.Classes
import ch.njol.skript.registrations.EventValues
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
import ch.njol.skript.lang.Condition
import ch.njol.skript.lang.DefaultExpression
import ch.njol.skript.lang.Effect
import ch.njol.skript.lang.Expression
import ch.njol.skript.lang.Section
import ch.njol.skript.localization.Noun
import ch.njol.skript.util.Contract
import com.google.gson.TypeAdapter
import jp.nlaocs.gson.RuntimeTypeAdapterFactory
import jp.nlaocs.skriptSyntaxGenerator.generator.SyntaxDataGenerator
import jp.nlaocs.skriptSyntaxGenerator.serializer.GsonFactory
import jp.nlaocs.skriptSyntaxGenerator.util.*
import org.bukkit.Bukkit
import org.skriptlang.skript.lang.entry.EntryData

import org.skriptlang.skript.lang.entry.EntryValidator
import org.skriptlang.skript.lang.entry.util.LiteralEntryData
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData
import org.skriptlang.skript.registration.DefaultSyntaxInfos
import org.skriptlang.skript.common.function.Function
import org.skriptlang.skript.common.function.Parameter
import org.skriptlang.skript.lang.entry.ContainerEntryData
import org.skriptlang.skript.lang.entry.KeyValueEntryData
import org.skriptlang.skript.lang.entry.SectionEntryData
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.TriggerEntryData

import java.util.Locale
import java.util.function.Supplier
import java.util.regex.Pattern

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

            val entryDataAdapter = RuntimeTypeAdapterFactory
                .of(EntryData::class.java, "entryDataClass")
                .registerSubtype(LiteralEntryData::class.java)
                .registerSubtype(VariableStringEntryData::class.java)
                .registerSubtype(ExpressionEntryData::class.java)
                .registerSubtype(TriggerEntryData::class.java)
                .registerSubtype(ContainerEntryData::class.java)
                .registerSubtype(KeyValueEntryData::class.java)
                .registerSubtype(SectionEntryData::class.java)


            val gson = GsonFactory.create()

            // todo getAddonProviderは、引数にかかわらず同じproviderを返してくるので今はこうしている。将来的にaddonごとのproviderが返されるようになった場合変更する
            //val aliases = Aliases.getAddonProvider(Skript.getAddonInstance())

            val syntaxDataGenerator = SyntaxDataGenerator()
            syntaxDataGenerator.generate()

            /*val aliasesData = mutableListOf<AliasesData>()
            if (aliases != null) {
                val aliasData = AliasesData(aliases)
                aliasesData.add(aliasData)

            }
            FileUtils.writeToFile("aliases.json", gson.toJson(aliasesData))*/


            sender.sendMessage("Skript syntax data generation completed!")
            return true
        }

        return false
    }
}

class ColorAdapter : TypeAdapter<java.awt.Color>() {
    override fun write(out: com.google.gson.stream.JsonWriter, value: java.awt.Color) {
        out.value(String.format("#%02x%02x%02x", value.red, value.green, value.blue))
    }

    override fun read(`in`: com.google.gson.stream.JsonReader): java.awt.Color {
        val colorStr = `in`.nextString()
        return java.awt.Color.decode(colorStr)
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
        this.usage = s.c.toStringListSafe().ifEmpty { s.usage?.toList() }.cleaning()
        this.examples = s.examples?.toList()
        this.since = s.since
        this.requiredPlugins = s.requiredPlugins?.toList()
        //this.properties = s.allProperties.toList()
        //this.propertyInfos = s.getPropertyInfos()
        //this.propertyDocumentation = s.getPropertyDocumentation()

    }
}

class FunctionData {
    var name: String? = null

    //var returnsKeys: List<String>? = null
    var returnType: Class<*>? = null
    var returnTypeIsSingle: Boolean? = null

    //var originalClass: Class<*>? = null
    var addon: Common.AddonInfo? = null

    //var contract: Contract? = null
    var contractClass: Class<out Contract>? = null // todo いるのか？
    var since: List<String>? = null
    var description: List<String>? = null
    var examples: List<String>? = null
    var keywords: List<String>? = null
    var requires: List<String>? = null

    //var parameters: Parameter<*>? = null
    //var parameters: Parameters? = null
//    var parameters: List<Parameter<*>>? = null
    var parameters: List<ParameterInfo>? = null

    data class ParameterInfo(
        var name: String,
        var type: Class<*>,
        var modifiers: List<ModifierInfo>,
        //var hasModifier: Boolean,
        var isSingle: Boolean,
        //var defaultExpressionClass: Class<out DefaultExpression<*>>?,
    ) {
        /*constructor(param: Parameter<*>) : this({
            /*name = param.name(),
            type = param.type(),
            modifiers = param.modifiers().map { ModifierInfo.from(it) }.toList(),
            //hasModifier = param.modifiers().isNotEmpty(),
            isSingle = param.isSingle*/
            if (param is ch.njol.skript.lang.function.Parameter<*>) {
                name = param.name()
                type = param.type()
                modifiers = param.modifiers().map { ModifierInfo.from(it) }.toList()
                isSingle = param.isSingle
            } else {
                // ありえないはずだが一応
                name = "unknown"
                type = Any::class.java
                modifiers = emptyList()
                isSingle = true
            }


        })*/
        constructor(param: Parameter<*>) : this(
            name = param.name(),
            type = param.type(),
            //modifiers = param.modifiers().map { ModifierInfo.from(it) }.toList(),
            modifiers =
                if (param is ch.njol.skript.lang.function.Parameter<*>) {
                    param.modifiers().map { ModifierInfo.from(it) }.toList()
                } else {
                    param.modifiers().map { ModifierInfo.from(it) }.toList()
                },
            isSingle = param.isSingle
        )

        data class ModifierInfo(
            val type: String,
            val min: Any? = null,
            val max: Any? = null
        ) {
            companion object {
                fun from(mod: Parameter.Modifier): ModifierInfo {
                    return when (mod) {
                        Parameter.Modifier.OPTIONAL ->
                            ModifierInfo("optional")

                        Parameter.Modifier.KEYED ->
                            ModifierInfo("keyed")

                        is Parameter.Modifier.RangedModifier<*> ->
                            ModifierInfo(
                                "range",
                                mod.min,
                                mod.max
                            )

                        else ->
                            ModifierInfo("unknown")
                    }
                }
            }
        }

    }

    constructor(s: Function<*>) {
        // todo
        // sを出力
        //Bukkit.getLogger().info("Function: $s in ${s.javaClass.name}")
        /*originalClass = s.javaClass
        signature = s.signature()
        returnsKeys = s.returnedKeys().toList()*/
        /*if (f instanceof ch.njol.skript.lang.function.Function<?> func) {
        func.getSignature().getName(); // ✅
        func.getName();                // ✅
    }*/
        if (s is ch.njol.skript.lang.function.Function<*>) {
            //this.name = s.
            /*this.originalClass = s.javaClass
            this.signature = s.signature()
            this.returnsKeys = s.returnedKeys().toList()*/
            //Bukkit.getLogger().info("Function: ${s.signature()} in ${s.javaClass.name}")
            this.name = s.name
            this.returnType = s.type()
            this.returnTypeIsSingle = s.isSingle
//            this.contract = s.signature.contract
            this.contractClass = s.signature.contract?.javaClass // todo いるのか?
            //this.parameters = s.signature.parameters().all().toList()
            this.parameters = s.signature.parameters().all().map { ParameterInfo(it) }.toList()

            /*val providerPlugin = JavaPlugin.getProvidingPlugin(s.javaClass)
            this.addon = Common.AddonInfo(
                name = providerPlugin.name,
                version = providerPlugin.description.version
            )*/


            when (s) {
                is org.skriptlang.skript.common.function.DefaultFunction<*> -> {
                    /*this.name = s.name
                    this.signature = s.signature
                    this.returnType = s.returnType?.c
                    this.originalClass = s.javaClass*/

                    // not null
                    this.since = s.since()
                    this.description = s.description()
                    this.examples = s.examples()
                    this.keywords = s.keywords()
                    this.requires = s.requires()
                    val providerPlugin = JavaPlugin.getProvidingPlugin(s.source().source())
                    this.addon = Common.AddonInfo(
                        name = providerPlugin.name,
                        version = providerPlugin.description.version
                    )


                }

                is ch.njol.skript.lang.function.SimpleJavaFunction<*> -> {
                    /*this.name = s.name
                    this.signature = s.signature
                    this.returnType = s.returnType?.c
                    this.originalClass = s.javaClass*/

                    this.since = s.since()
                    this.description = s.description()
                    this.examples = s.examples()
                    this.keywords = s.keywords()
                    this.requires = s.requires()
                    val providerPlugin = JavaPlugin.getProvidingPlugin(Class.forName(s.signature.originClassPath))
                    this.addon = Common.AddonInfo(
                        name = providerPlugin.name,
                        version = providerPlugin.description.version
                    )
                }

                is ch.njol.skript.lang.function.JavaFunction<*> -> {
                    /*this.name = s.name
                    this.signature = s.signature
                    this.returnType = s.returnType?.c
                    this.originalClass = s.javaClass*/

                    this.since = s.since()
                    this.description = s.description()
                    this.examples = s.examples()
                    this.keywords = s.keywords()
                    this.requires = s.requires()
                    val providerPlugin = JavaPlugin.getProvidingPlugin(Class.forName(s.signature.originClassPath))
                    this.addon = Common.AddonInfo(
                        name = providerPlugin.name,
                        version = providerPlugin.description.version
                    )
                }

                else ->
                    Bukkit.getLogger().warning("Unknown Function implementation: ${s.javaClass.name}. Skipping.")
            }
        } else {
            Bukkit.getLogger().warning("Unknown Function implementation: ${s.javaClass.name}. Skipping.")
        }
        //this.name = sig.
    }
}

/*
class AliasesData {
    /*var name: String? = null
    var originalClass: Class<*>? = null
    var aliasCount: Int? = null
    var aliases: Map<String, ItemType>? = null*/
    //var datas: List<AliasData>? = null
    var data: MutableMap<String, AliasData> = mutableMapOf()


    data class AliasData(
        val types: List<ItemData>,
        //val all: Boolean,
        val amount: Int,
        /*val item: ItemType,
        val block: ItemType,*/
        //val globalMeta: ItemMeta
    )

    constructor(provider: AliasesProvider?) {
        if (provider == null) {
            Bukkit.getLogger().warning("AliasesProvider is null. No aliases data will be generated.")
            return
        }

        try {
            val field = provider.javaClass.getDeclaredField("aliases")
            field.isAccessible = true
            val aliasesMap = field.get(provider) as? Map<String, ItemType>
            if (aliasesMap == null) {
                Bukkit.getLogger()
                    .warning("Failed to cast AliasesProvider.aliases to Map<String, ItemType>. No aliases data will be generated.")
                return
            }

            for ((aliasName, itemType) in aliasesMap) {
                val types = itemType.types.toList()
                //val all = itemType.all
                val amount = itemType.amount
                //val item = itemType.item
                //val block = itemType.block
                //val globalMeta = itemType.globalMeta

                data[aliasName] = AliasData(
                    types = types,
                    //all = all,
                    amount = amount,
                    //item = item,
                    //block = block,
                    //globalMeta = globalMeta
                )
            }
        } catch (e: Exception) {
            Bukkit.getLogger()
                .warning("Failed to access AliasesProvider.aliases: ${e.javaClass.simpleName}: ${e.message}. No aliases data will be generated.")
        }
    }
}*/

// todo addon別
// todo json順序を逆に
// todo typeには解析順序がある
// todo typeのnameのgenderなどはソースコード解析時に設定されるものと思われるので構文リストに載せる必要はない
// todo typeのcolorなどの、interfaceだがenumっぽい動きをするものは別途usageを用意しなければならない
// todo typeのbeforeやafter、またaliasesの定義も取得する
// todo structuresのentryValidatorのentryDataのTypeなど、常に追加する
// todo 要素ゼロのListはnullにする
// todo 過去バージョンのためalias
// todo コンバーターも保持
