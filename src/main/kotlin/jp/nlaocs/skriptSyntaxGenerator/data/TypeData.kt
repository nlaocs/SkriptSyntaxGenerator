package jp.nlaocs.skriptSyntaxGenerator.data

import ch.njol.skript.classes.Changer
import ch.njol.skript.classes.ClassInfo
import ch.njol.skript.lang.DefaultExpression
import ch.njol.skript.localization.Noun
import ch.njol.skript.registrations.Classes
import jp.nlaocs.skriptSyntaxGenerator.data.common.Addon
import jp.nlaocs.skriptSyntaxGenerator.data.common.AddonInfo
import jp.nlaocs.skriptSyntaxGenerator.data.common.Documentable
import jp.nlaocs.skriptSyntaxGenerator.hook.collector.RegisterClassCollector
import jp.nlaocs.skriptSyntaxGenerator.util.StableIds
import jp.nlaocs.skriptSyntaxGenerator.util.cleaning
import jp.nlaocs.skriptSyntaxGenerator.util.getTypeStr
import jp.nlaocs.skriptSyntaxGenerator.util.stableName
import jp.nlaocs.skriptSyntaxGenerator.util.toStringListSafe
import java.util.regex.Pattern

class TypeData(
    s: ClassInfo<*>,
    val typeParseOrder: Int
) : Documentable, Addon {
    override val name: String? = s.docName
    override val description: List<String>? = s.description?.filterNotNull()?.toList()
    override val since: List<String>? = s.since?.let { listOf(it) }
    override val examples: List<String>? = s.examples?.filterNotNull()?.toList()
    override val keywords: List<String>? = null
    override val requires: List<String>? = s.requiredPlugins?.filterNotNull()?.toList()

    @Transient
    val snapshot = requireNotNull(RegisterClassCollector.getInstance().snapshotMap()[s.codeName]) {
        "registerClass snapshot was not found for ${s.codeName ?: s.c}"
    }

    override val addon: AddonInfo = requireNotNull(snapshot.addon) {
        "registerClass snapshot addon was not found for ${s.codeName ?: s.c}"
    }

    val definitionId: String =
        StableIds.record("type", addon, s.codeName, s.c.stableName())
    val registrationId: String = definitionId
    val documentationId: String? = s.documentationID
    val hasDocs: Boolean = s.hasDocs()

    val changer: Map<Changer.ChangeMode, List<Class<*>>>? =
        Changer.ChangeMode.entries
            .mapNotNull { mode ->
                s.changer?.acceptChange(mode)?.toList()?.let { mode to it }
            }
            .toMap()
            .takeIf { it.isNotEmpty() }

    val originalClass: Class<*> = s.c
    val classType: String = s.c.getTypeStr()
    val codeName: String = s.codeName
    val superClass: Class<*>? = s.c.superclass
    val interfaces: List<Class<*>> = s.c.interfaces.toList()
    val assignableTo: List<String> = Classes.getClassInfos()
        .asSequence()
        .filter { it !== s && it.c.isAssignableFrom(s.c) }
        .map { it.codeName }
        .toList()
    val userInputPatterns: List<Pattern>? = s.userInputPatterns?.toList()
    val noun: Noun = s.name
    val serializeAs: Class<*>? = s.serializeAs
    val usage = s.c.toStringListSafe().ifEmpty { s.usage?.toList() }.cleaning()

    val defaultExpressionClass: Class<out DefaultExpression<*>>? = s.defaultExpression?.javaClass
    val hasParser: Boolean = s.parser != null
    val hasSerializer: Boolean = s.serializer != null
    val hasSupplier: Boolean = s.supplier != null

    val properties: List<String> = s.allProperties.map { it.name() }.sorted()
    val before = snapshot.before.toList().cleaning()
    val after = snapshot.after.toList().cleaning()
}
