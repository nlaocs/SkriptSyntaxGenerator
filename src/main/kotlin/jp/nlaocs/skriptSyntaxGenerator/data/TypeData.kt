package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.DefaultExpression
import ch.njol.skript.localization.Noun
import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.data.common.Documentable
import jp.nlaocs.skriptSyntaxGenerator.util.cleaning
import jp.nlaocs.skriptSyntaxGenerator.util.getTypeStr
import jp.nlaocs.skriptSyntaxGenerator.util.toStringListSafe
import org.bukkit.plugin.java.JavaPlugin
import java.util.function.Supplier
import java.util.regex.Pattern

class TypeData(s: ClassInfo<*>) : Documentable, Addon {
    override val name: String? = s.docName
    override val description: List<String>? = s.description?.filterNotNull()?.toList()
    override val since: List<String>? = s.since?.let { listOf(it) }
    override val examples: List<String>? = s.examples?.filterNotNull()?.toList()
    override val keywords: List<String>? = null // ClassInfoにkeywordsは実装されていない
    override val requires: List<String>? = s.requiredPlugins?.filterNotNull()?.toList()

    override val addon: AddonInfo = (s.parser ?: s.serializer ?: s.changer ?: s)
        .let { JavaPlugin.getProvidingPlugin(it::class.java) }
        .let { AddonInfo(it.name, it.description.version) }

    val changer: Map<Changer.ChangeMode, List<Class<*>>>? =
        Changer.ChangeMode.entries
            .mapNotNull { cm ->
                val list = s.changer?.acceptChange(cm)?.toList()
                if (list.isNullOrEmpty()) null else cm to list
            }
            .toMap()
            .takeIf { it.isNotEmpty() }
    val originalClass: Class<*> = s.c
    val classType: String = s.c.getTypeStr()
    val codeName: String? = s.codeName
    val superClass: Class<*>? = s.c.superclass
    val userInputPatterns: List<Pattern>? = s.userInputPatterns?.toList()
    val noun: Noun? = s.name
    val serializeAs: Class<*>? = s.serializeAs
    val usage = s.c.toStringListSafe().ifEmpty { s.usage?.toList() }.cleaning()

    val defaultExpressionClass: Class<out DefaultExpression<*>>? = s.defaultExpression?.javaClass

    val supplierClass: Class<out Supplier<out Iterator<*>>>? = s.supplier?.javaClass // いらないかも、殆どがlambda
    // val parserClass: Class<out Parser<*>>? = s.parser?.javaClass
    // val serializerClass: Class<out Serializer<*>>? = s.serializer?.javaClass
    // todo before/after
}
