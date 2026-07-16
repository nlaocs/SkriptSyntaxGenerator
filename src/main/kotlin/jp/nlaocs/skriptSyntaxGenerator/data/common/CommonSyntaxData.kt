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
import ch.njol.skript.doc.RelatedProperty
import ch.njol.skript.doc.Since
import ch.njol.skript.lang.EventRestrictedSyntax
import ch.njol.skript.lang.ReturnHandler
import com.fasterxml.jackson.annotation.JsonValue
import jp.nlaocs.skriptSyntaxGenerator.util.*
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos
import org.skriptlang.skript.lang.experiment.Experiment
import org.skriptlang.skript.lang.experiment.ExperimentalSyntax
import org.skriptlang.skript.lang.experiment.SimpleExperimentalSyntax
import org.skriptlang.skript.registration.SyntaxInfo
import org.skriptlang.skript.util.Priority
import java.util.Locale

open class CommonSyntaxData(
    val kind: SyntaxKind,
    val registrationOrder: Int,
    @Transient private val registrationOccurrence: Int,
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
    override val addon: AddonInfo,
    @Transient private val metadata: SyntaxMetadataBundle? = null
) : Documentable, Addon {

    val definitionId: String = StableIds.definition(kind.value, addon, elementClass)
    val registrationId: String =
        StableIds.registration(definitionId, patterns, registrationOccurrence)
    val relatedProperty: String? = elementClass.annoValue<RelatedProperty, String>()

    val supportedEvents: List<Class<out Event>>? = metadata?.supportedEvents?.value
    val supportedEventsState: SyntaxMetadataState? = metadata?.supportedEvents?.state
    val experimentalSyntax: ExperimentalSyntaxData? = metadata?.experimentalSyntax?.value
    val experimentalSyntaxState: SyntaxMetadataState? = metadata?.experimentalSyntax?.state
    val returnHandler: ReturnHandlerData? = metadata?.returnHandler?.value
    val returnHandlerState: SyntaxMetadataState? = metadata?.returnHandler?.state

    constructor(
        s: BukkitSyntaxInfos.Event<*>,
        registrationOrder: Int,
        registrationOccurrence: Int,
        addonOverride: AddonInfo? = null
    ) : this(
        kind = SyntaxKind.EVENT,
        registrationOrder = registrationOrder,
        registrationOccurrence = registrationOccurrence,
        name = s.name(),
        id = s.id(),
        documentationId = s.documentationId(),
        elementClass = s.type(),
        since = s.since().toList().nullIfEmpty(),
        description = s.description().toList().nullIfEmpty(),
        examples = s.examples().toList().nullIfEmpty(),
        keywords = s.keywords().toList().nullIfEmpty(),
        requires = s.requiredPlugins().toList().nullIfEmpty(),
        noDoc = s.type().hasAnno<NoDoc>(),
        events = null,
        deprecated = s.type().hasAnno<Deprecated>(),
        priorityStr = s.priority().toPriorityStr(),
        priority = s.priority(),
        patterns = s.patterns().toList(),
        addon = addonOverride ?: resolveEventAddon(s),
        metadata = resolveSyntaxMetadata(s)
    )

    constructor(
        s: SyntaxInfo<*>,
        kind: SyntaxKind,
        registrationOrder: Int,
        registrationOccurrence: Int
    ) : this(
        kind = kind,
        registrationOrder = registrationOrder,
        registrationOccurrence = registrationOccurrence,
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
        addon = requireNotNull(AddonResolver.fromClass(s.type())) {
            "Unable to resolve addon for syntax ${s.type().name}"
        },
        metadata = resolveSyntaxMetadata(s)
    ) {
        val type = s.type()

        examples =
            type.anno<Example>()?.let { listOf(it.value) }
                ?: type.anno<Example.Examples>()?.let { it.value.map { ex -> ex.value }.nullIfEmpty() }
                        ?: type.anno<Examples>()?.value.toListOrNullIfEmpty()
    }

    companion object {
        private fun resolveSyntaxMetadata(s: SyntaxInfo<*>): SyntaxMetadataBundle {
            val type = s.type()
            val needsSupportedEvents = EventRestrictedSyntax::class.java.isAssignableFrom(type)
            val needsExperimentalSyntax = ExperimentalSyntax::class.java.isAssignableFrom(type)
            val needsReturnHandler = ReturnHandler::class.java.isAssignableFrom(type)
            if (!needsSupportedEvents && !needsExperimentalSyntax && !needsReturnHandler) {
                return SyntaxMetadataBundle()
            }

            val instanceResult = runCatching { s.instance() }

            return SyntaxMetadataBundle(
                supportedEvents = if (needsSupportedEvents) {
                    instanceResult.fold(
                        onSuccess = { instance -> resolveSupportedEvents(instance, s) },
                        onFailure = { error -> unresolvedMetadata(s, "supported events", error) }
                    )
                } else {
                    MetadataResult.notApplicable()
                },
                experimentalSyntax = if (needsExperimentalSyntax) {
                    instanceResult.fold(
                        onSuccess = { instance -> resolveExperimentalSyntax(instance) },
                        onFailure = { error -> unresolvedMetadata(s, "experimental syntax data", error) }
                    )
                } else {
                    MetadataResult.notApplicable()
                },
                returnHandler = if (needsReturnHandler) {
                    instanceResult.fold(
                        onSuccess = { instance -> resolveReturnHandler(instance, s) },
                        onFailure = { error -> unresolvedMetadata(s, "return handler data", error) }
                    )
                } else {
                    MetadataResult.notApplicable()
                }
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun resolveSupportedEvents(
            instance: Any,
            s: SyntaxInfo<*>
        ): MetadataResult<List<Class<out Event>>> = try {
            val supportedEvents = (instance as EventRestrictedSyntax)
                .supportedEvents()
                .filterNotNull()
                .map { it as Class<out Event> }
            MetadataResult.resolved(supportedEvents)
        } catch (e: Exception) {
            unresolvedMetadata(s, "supported events", e)
        }

        private fun resolveExperimentalSyntax(instance: Any): MetadataResult<ExperimentalSyntaxData> {
            if (instance !is SimpleExperimentalSyntax) {
                return MetadataResult.unresolved()
            }

            return try {
                MetadataResult.resolved(ExperimentalSyntaxData(instance.experimentData))
            } catch (e: Exception) {
                MetadataResult.unresolved()
            }
        }

        private fun resolveReturnHandler(
            instance: Any,
            s: SyntaxInfo<*>
        ): MetadataResult<ReturnHandlerData> = try {
            val handler = instance as ReturnHandler<*>
            MetadataResult.resolved(
                ReturnHandlerData(
                    returnValueType = handler.returnValueType(),
                    singleReturnValue = handler.isSingleReturnValue
                )
            )
        } catch (e: Exception) {
            unresolvedMetadata(s, "return handler data", e)
        }

        private fun <T> unresolvedMetadata(
            s: SyntaxInfo<*>,
            label: String,
            error: Throwable
        ): MetadataResult<T> {
            Bukkit.getLogger().warning(
                "Failed to retrieve $label for syntax: ${syntaxName(s)}. Setting value to null. Error: ${error.message}"
            )
            return MetadataResult.unresolved()
        }

        private fun syntaxName(s: SyntaxInfo<*>): String =
            s.type().annoValue<Name, String>() ?: s.type().name

        private fun resolveEventAddon(s: BukkitSyntaxInfos.Event<*>): AddonInfo =
            AddonResolver.fromOrigin(s.origin())
                ?: requireNotNull(AddonResolver.fromClass(s.type())) {
                    "Unable to resolve addon for event syntax ${s.type().name}"
                }
    }
} // todo 実装が汚い気がする..

data class SyntaxMetadataBundle(
    val supportedEvents: MetadataResult<List<Class<out Event>>> = MetadataResult.notApplicable(),
    val experimentalSyntax: MetadataResult<ExperimentalSyntaxData> = MetadataResult.notApplicable(),
    val returnHandler: MetadataResult<ReturnHandlerData> = MetadataResult.notApplicable()
)

data class MetadataResult<T>(
    val value: T?,
    val state: SyntaxMetadataState?
) {
    companion object {
        fun <T> resolved(value: T): MetadataResult<T> =
            MetadataResult(value, SyntaxMetadataState.RESOLVED)

        fun <T> unresolved(): MetadataResult<T> =
            MetadataResult(null, SyntaxMetadataState.UNRESOLVED)

        fun <T> notApplicable(): MetadataResult<T> =
            MetadataResult(null, null)
    }
}

enum class SyntaxMetadataState(@get:JsonValue val value: String) {
    RESOLVED("resolved"),
    UNRESOLVED("unresolved")
}

data class ReturnHandlerData(
    val returnValueType: Class<*>?,
    val singleReturnValue: Boolean
)

data class ExperimentalSyntaxData(
    val required: List<ExperimentData>,
    val disallowed: List<ExperimentData>,
    val errorMessage: String
) {
    constructor(data: org.skriptlang.skript.lang.experiment.ExperimentData) : this(
        required = data.required.map(::ExperimentData),
        disallowed = data.disallowed.map(::ExperimentData),
        errorMessage = data.errorMessage
    )
}

data class ExperimentData(
    val codeName: String,
    val phase: String,
    val known: Boolean
) {
    constructor(experiment: Experiment) : this(
        codeName = experiment.codeName(),
        phase = experiment.phase().name.lowercase(Locale.ENGLISH),
        known = experiment.isKnown()
    )
}
