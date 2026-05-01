package jp.nlaocs.skriptSyntaxGenerator

import org.bukkit.plugin.java.JavaPlugin

import com.google.gson.TypeAdapter
import jp.nlaocs.gson.RuntimeTypeAdapterFactory
import jp.nlaocs.skriptSyntaxGenerator.generator.SyntaxDataGenerator
import jp.nlaocs.skriptSyntaxGenerator.hook.HookManager
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.HookCollectorRegistry
import jp.nlaocs.skriptSyntaxGenerator.serializer.GsonFactory

import org.skriptlang.skript.lang.entry.EntryData
import org.skriptlang.skript.lang.entry.util.LiteralEntryData
import org.skriptlang.skript.lang.entry.util.VariableStringEntryData
import org.skriptlang.skript.lang.entry.ContainerEntryData
import org.skriptlang.skript.lang.entry.KeyValueEntryData
import org.skriptlang.skript.lang.entry.SectionEntryData
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData
import org.skriptlang.skript.lang.entry.util.TriggerEntryData

class SkriptSyntaxGenerator : JavaPlugin() {

    override fun onLoad() {
        HookManager.getInstance().setLogger(logger)
        HookManager.getInstance().init()
    }

    override fun onEnable() {
        this.getCommand("skgen")?.setExecutor(SkriptSyntaxCommandExecutor())
        logger.info("SkriptSyntaxGenerator has been enabled!")
    }

    override fun onDisable() {
        HookCollectorRegistry.clearAll()
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

            val syntaxDataGenerator = SyntaxDataGenerator()
            syntaxDataGenerator.generate()

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
// todo tagというものがある、用途まだ不明
// todo DefaultValue
// todo EventValueとArithmeticsは動的に生成されている
