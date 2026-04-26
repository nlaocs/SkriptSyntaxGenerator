package jp.nlaocs.skriptSyntaxGenerator.data.common

import ch.njol.skript.doc.Description
import ch.njol.skript.doc.DocumentationId
import ch.njol.skript.doc.Events
import ch.njol.skript.doc.Example
import ch.njol.skript.doc.Examples
import ch.njol.skript.doc.Keywords
import ch.njol.skript.doc.Name
import ch.njol.skript.doc.NoDoc
import ch.njol.skript.doc.RequiredPlugins
import ch.njol.skript.doc.Since
import jp.nlaocs.skriptSyntaxGenerator.util.*
import org.bukkit.plugin.java.JavaPlugin
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.util.Priority
import java.util.Locale

open class CommonSyntaxData(
    override val name: String?,
    val id: String?,
    val documentationId: String?,
    val elementClass: Class<*>,
    val superClass: Class<*> = elementClass.superclass,
    override val since: List<String>?,
    override val description: List<String>?,
    override var examples: List<String>?,
    override val keywords: List<String>?,
    override val requires: List<String>?,
    val noDoc: Boolean,
    val events: List<String>?, // todo 特定のイベントのみで使用可能にするためのものだが、CommonにあるとEventにもこのプロパティがある。いるのか？
    val deprecated: Boolean?,
    val priorityStr: String? = null, // todo Enum
    val priority: Priority? = null,
    var patterns: List<String>,
    override val addon: AddonInfo
) : Documentable, Addon {

    constructor(s: BukkitSyntaxInfos.Event<*>) : this(
        name = s.name(),
        id = s.id(),
        documentationId = s.documentationId(),
        elementClass = s.type(),
        since = s.since().toList(),
        description = s.description().toList(),
        examples = s.examples().toList(),
        keywords = s.keywords().toList(),
        requires = s.requiredPlugins().toList(),
        noDoc = s.type().hasAnno<NoDoc>(),
        events = null,
        deprecated = s.type().hasAnno<Deprecated>(),
        priorityStr = s.priority().toPriorityStr(),
        priority = s.priority(),
        patterns = s.patterns().toList(),
        addon = JavaPlugin.getProvidingPlugin(s.type()).let {
            AddonInfo(
                name = it.name,
                version = it.description.version
            )
        }
    )

    constructor(s: SyntaxInfo<*>) : this(
        name = s.type().annoValue<Name, String>(),
        id = null,
        documentationId = s.type().annoValue<DocumentationId, String>(),
        elementClass = s.type(),
        since = s.type().annoValues<Since, String>(),
        description = s.type().annoValues<Description, String>(),
        examples = null,
        keywords = s.type().annoValues<Keywords, String>(),
        requires = s.type().annoValues<RequiredPlugins, String>(),
        noDoc = s.type().hasAnno<NoDoc>(),
        events = s.type().annoValues<Events, String>(),
        deprecated = s.type().hasAnno<Deprecated>(),
        priorityStr = s.priority().toPriorityStr(),
        priority = s.priority(),
        patterns = s.patterns().toList(),
        addon = JavaPlugin.getProvidingPlugin(s.type()).let {
            AddonInfo(
                name = it.name,
                version = it.description.version
            )
        }
    ) {
        val type = s.type()

        examples =
            type.anno<Example>()?.let { listOf(it.value) }
                ?: type.anno<Example.Examples>()?.let { it.value.map { ex -> ex.value } }
                        ?: type.anno<Examples>()?.value?.toList()
    }
} // todo 実装が汚い気がする..
