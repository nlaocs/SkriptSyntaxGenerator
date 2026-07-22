# Snapshot JSON format

English | [Japanese](json-format.ja.md)

This document describes schema version `3`, the 19 files emitted by `/skgen`, and the Skript concepts represented by those files. It is written for consumers that do not already know Skript's Java API.

## Reading the format

### Presence and nullability

The generator serializes DTOs with Jackson's `NON_NULL` inclusion. Unless an opaque payload is explicitly described below:

- **Required** means the property is present and is never JSON `null`.
- **Optional** means the property is omitted when no value is available. It is not written as JSON `null`.
- An empty array or object is meaningful when the field is required. For example, `eventValues: []` means that the event has no known event values, while an omitted state-dependent field means that the generator could not resolve it or that it does not apply.
- Documentation arrays such as `description` and `examples` are omitted when empty.

Consumers should read `Manifest.json` first. Do not infer support only from a Skript version string, and do not treat an empty file as proof that a feature is unsupported; a supported registry may legitimately contain no addon records.

### Common notation

| Notation | Meaning |
| --- | --- |
| `array<T>` | JSON array whose elements have type `T`. |
| `object<string, T>` | JSON object used as a string-keyed map. |
| `class-name` | Stable Java class name such as `java.lang.String`; arrays use source form such as `java.lang.String[]`. |
| `int` | JSON integer produced from a Java/Kotlin 32-bit signed integer unless a narrower range is stated. |
| `sha256` | 64 lowercase hexadecimal characters. |

### IDs and order

`definitionId`, `registrationId`, and `snapshotId` are deterministic identifiers. Their current strings contain readable prefixes and SHA-256-derived components, but consumers must treat the complete value as opaque.

- `definitionId` groups registrations that use the same syntax implementation.
- `registrationId` identifies registration content, such as patterns or a signature. Common syntax IDs include duplicate occurrence, but some non-syntax registries can currently give identical registrations the same ID; do not use it as the only array key without duplicate handling.
- `registrationOrder` is a zero-based registry order. It can affect parsing and resolution, so consumers must not replace it with alphabetical order.
- `typeParseOrder` and `resolutionOrder` are also zero-based orders in their own registries.

## File index

| File | Root | What it describes |
| --- | --- | --- |
| `Manifest.json` | object | Snapshot identity, server, plugins, capabilities, and file list. |
| `Conditions.json` | array | Boolean checks used in `if`, `while`, and similar clauses. |
| `Effects.json` | array | Statements that perform actions. |
| `Events.json` | array | Trigger headers such as an event that starts a block of code. |
| `Expressions.json` | array | Syntax that produces one or more values. |
| `Sections.json` | array | Syntax inside executable code that owns an indented block. |
| `Structures.json` | array | Top-level declarations and other configuration-node syntax. |
| `Types.json` | array | Skript-visible value types and their Java representations. |
| `Functions.json` | array | Registered callable functions and their signatures. |
| `Converters.json` | array | Allowed automatic conversions from one value type to another. |
| `Comparators.json` | array | Rules for comparing two value types. |
| `EventValues.json` | array | Values made available by Bukkit events. |
| `Properties.json` | array | Named capabilities shared by types, such as a type having a location. |
| `Operators.json` | array | Arithmetic symbols and precedence. |
| `Operations.json` | object | Valid left/right type combinations for each arithmetic operator. |
| `Differences.json` | array | Rules for calculating the distance or difference between values. |
| `ClassHierarchy.json` | array | Java inheritance graph for every class referenced by the snapshot. |
| `Aliases.json` | object | Globally registered item/block names and their resolved targets. |
| `PluralRules.json` | object | Effective English singular/plural conversion rules in runtime priority order. |

Every file is always emitted. The empty root is `[]` for array files, `{}` for `Operations.json`, `{"aliases":{},"targets":[]}` for `Aliases.json`, and `{"algorithm":"unresolved","pluralOverrideSupported":false,"rules":[]}` for `PluralRules.json`.

## Shared objects

### `AddonInfo`

This object identifies the plugin that registered a record.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `name` | string | Required | Bukkit plugin name. May be `unknown` when ownership cannot be attributed. |
| `version` | string | Required | Plugin version. May be `unknown` with unresolved ownership. |

### `Priority`

Skript uses a partial ordering rather than one universal numeric priority. Priorities may recursively refer to other priorities.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `after` | `array<Priority>` | Required | Priorities that must be tried before this priority. Empty means no such constraint. |
| `before` | `array<Priority>` | Required | Priorities that must be tried after this priority. Empty means no such constraint. |

### Change-mode map

Several records use `object<string, array<class-name>>` to describe how a value can be changed. Keys are:

| Key | Script-level meaning |
| --- | --- |
| `ADD` | Add or append a value. |
| `SET` | Replace the current value. |
| `REMOVE` | Remove matching values. |
| `REMOVE_ALL` | Remove every occurrence or all matching values. |
| `DELETE` | Delete/clear the value; its type array may be empty because no argument is passed. |
| `RESET` | Restore a default value; its type array may be empty because no argument is passed. |

A missing key means that mode is unsupported. A present key maps to accepted argument classes. A whole map equal to `{}` means the metadata was resolved and the value is read-only. For fields paired with a `...State`, an omitted map plus `"unresolved"` means that it could not be determined safely.

### Resolution state

State fields use `"resolved"` or `"unresolved"`. `resolved` means the paired value was obtained, including an empty array/object. `unresolved` means the generator deliberately omitted the paired value because evaluating it was unsafe or failed. If both the value and state are absent, the metadata does not apply to that record or is unavailable in that adapter.

## Syntax files

`Conditions.json`, `Effects.json`, `Events.json`, `Expressions.json`, `Sections.json`, and `Structures.json` share the following record fields.

### Common syntax record

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `kind` | string enum | Required | One of `condition`, `effect`, `event`, `expression`, `section`, or `structure`. |
| `registrationOrder` | int, `>= 0` | Required | Zero-based order in that syntax registry. |
| `name` | string | Optional | Human-readable documentation name. |
| `id` | string | Optional | Registry ID. Currently used by event registrations. |
| `documentationId` | string | Optional | Stable ID used by Skript's documentation system. |
| `elementClass` | class-name | Required | Java class implementing the syntax. |
| `superClass` | class-name | Optional | Direct Java superclass. Omitted only when no superclass exists or an old adapter cannot expose it. |
| `since` | `array<string>` | Optional | Version notes from documentation metadata; omitted when empty. |
| `description` | `array<string>` | Optional | Documentation paragraphs; omitted when empty. |
| `examples` | `array<string>` | Optional | Example Skript source snippets; omitted when empty. |
| `keywords` | `array<string>` | Optional | Documentation search terms; primarily available through the current registry adapter. |
| `requires` | `array<string>` | Optional | Required plugins or platform features named by documentation. This is descriptive, not a resolved dependency graph. |
| `noDoc` | boolean | Required | `true` when the syntax is intentionally hidden from generated Skript documentation. |
| `events` | `array<string>` | Optional | Human-readable event restrictions from the `@Events` documentation annotation. This is different from `supportedEvents`. |
| `deprecated` | boolean | Normally required | Whether the implementation class is Java-deprecated. Consumers should allow omission for compatibility. |
| `priorityStr` | string enum-like | Optional | Friendly priority name: `SyntaxInfos.SIMPLE`, `SyntaxInfos.COMBINED`, `SyntaxInfos.PATTERN_MATCHES_EVERYTHING`, `EventValueExpression.DEFAULT_PRIORITY`, `PropertyExpression.DEFAULT_PRIORITY`, or `CUSTOM`. |
| `priority` | `Priority` | Optional | Full parse-order constraints. Preserve this graph even when `priorityStr` is known. |
| `patterns` | `array<string>` | Required | Skript parser patterns registered for this record. Empty is schema-valid, though normal syntax registrations have at least one pattern. |
| `addon` | `AddonInfo` | Required | Plugin that registered the syntax. |
| `definitionId` | string | Required | Opaque deterministic ID for the implementation definition. |
| `registrationId` | string | Required | Opaque deterministic ID for this exact registration and duplicate occurrence. |
| `relatedProperty` | string | Optional | Property name from Skript's `@RelatedProperty` annotation. |
| `supportedEvents` | `array<class-name>` | State-dependent | Bukkit event classes in which an `EventRestrictedSyntax` implementation is valid. Empty with `resolved` means no event is accepted. |
| `supportedEventsState` | resolution state | Optional | Resolution result for `supportedEvents`. |
| `experimentalSyntax` | `ExperimentalSyntaxData` | State-dependent | Per-script experiment requirements. |
| `experimentalSyntaxState` | resolution state | Optional | Resolution result for `experimentalSyntax`. |
| `returnHandler` | `ReturnHandlerData` | State-dependent | Metadata for a syntax that owns a body able to return a value. |
| `returnHandlerState` | resolution state | Optional | Resolution result for `returnHandler`. |

`events` is documentation text such as an event name. `supportedEvents` contains actual Java event classes returned by the implementation and is suitable for machine validation.

### `ExperimentalSyntaxData`

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `required` | `array<ExperimentData>` | Required | Experiments that must be enabled. Empty is valid. |
| `disallowed` | `array<ExperimentData>` | Required | Experiments that must not be enabled. Empty is valid. |
| `errorMessage` | string | Required | Message used when the experiment constraints are not met. |

`ExperimentData` has required `codeName` (the token used to identify the experiment), `phase` (lowercase Skript lifecycle phase), and `known` (whether Skript recognizes it as a built-in/known experiment).

### `ReturnHandlerData`

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `returnValueType` | class-name | Optional | Java type accepted from a `return` statement. Omitted when the handler declares no type. |
| `singleReturnValue` | boolean | Required | Whether one value, rather than a collection, may be returned. |

### `Conditions.json`

Root: `array<CommonSyntaxRecord>` with `kind: "condition"`. A condition evaluates to true or false and is used by control flow and condition sections. It has no file-specific fields.

### `Effects.json`

Root: `array<CommonSyntaxRecord>` with `kind: "effect"`. An effect performs an action and does not itself produce a value. It has no file-specific fields.

### `Events.json`

An event record adds:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `referenceEvents` | `array<class-name>` | Required | Bukkit event classes that can cause this Skript event to run. |
| `eventValues` | `array<EventValueRecord>` | Required | Event values available for this event after inheritance/exclusion checks. Empty explicitly means none. |
| `cancellable` | boolean | Required | `true` only when `referenceEvents` is nonempty and every referenced Bukkit event implements `Cancellable`. |
| `hasOnPrefix` | boolean | Required | Whether the registered display name starts with `On `. Useful when normalizing event documentation names. |

The inline event-value objects use the same shape as `EventValues.json`.

### `Expressions.json`

An expression is syntax that produces a value, such as a player, location, number, or list.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `returnType` | class-name | Optional | Declared Java result type. Omitted when Skript does not expose one. |
| `sectionExpression` | boolean | Required | Whether the expression can own an indented section while also producing a value. |
| `returnTypeMultiplicity` | string enum | State-dependent | `SINGLE`, `MULTIPLE`, or `BOTH`. `BOTH` means the amount depends on delegated/runtime input. |
| `returnTypeMultiplicityState` | resolution state | Required | Whether multiplicity was safely inferred from bytecode and inheritance. |
| `acceptedChangers` | change-mode map | State-dependent | Supported mutations and their argument types. `{}` means resolved and read-only. |
| `acceptedChangersState` | resolution state | Required | Whether changer metadata was safely inferred. |

Multiplicity is about the number of values produced by one evaluation, not the number of possible Java return types. Legacy-adapter expressions intentionally report both state fields as `unresolved`; the LSP may override exceptional cases.

### `Sections.json`

A section owns an indented block inside executable code. It may also be registered elsewhere; these booleans describe Java inheritance, not exclusive categories.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `loopSection` | boolean | Required | Whether it is a `LoopSection`, allowing loop-specific behavior such as loop values. |
| `effectSection` | boolean | Required | Whether it is an `EffectSection`, a block-form effect. |

### `Structures.json`

A structure is top-level/configuration syntax, such as a command or function declaration in versions that register those constructs as structures.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `entryValidator` | `EntryValidatorData` | Optional | Schema for named child entries accepted inside the structure. Current adapter only. |
| `nodeType` | string enum | Optional | `SIMPLE`, `SECTION`, or `BOTH`: whether the structure is represented by one line, an indented block, or either. Current adapter only. |

`EntryValidatorData` has one required field, `entryData`, with type `array<EntryDataInfo>`. An empty array means the validator has no named entries or recursive collection was cut off.

| `EntryDataInfo` field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `key` | string | Required | Entry name expected in the structure. |
| `defaultValue` | any JSON-safe value | Optional | Default normalized as a scalar, enum name, regex string, list, map, or fallback string. |
| `optional` | boolean | Required | Whether the entry may be omitted. |
| `multiple` | boolean | Required | Whether the same entry may occur more than once. |
| `entryDataClass` | class-name | Required | Java class implementing this entry rule. |
| `kind` | string enum | Required | `literal`, `variableString`, `expression`, `trigger`, `container`, `section`, `keyValue`, or `unknown`. |
| `separator` | string | Optional | Delimiter between the key and value for key/value-like entries. |
| `valueType` | class-name | Optional | Required Java type for a literal entry. |
| `stringMode` | string | Optional | Skript variable-string parsing mode, serialized using its Java text form. |
| `returnTypes` | `array<class-name>` | Optional | Accepted result classes for an expression entry. |
| `flags` | int bitmask | Optional | Expression parser flags. Current Skript uses `1` for expressions and `2` for literals, so built-in combinations are `1..3`. Preserve unknown bits. |
| `nestedValidator` | `EntryValidatorData` | Optional | Validator for a nested container. Omitted when none exists or a cycle is detected. |

`kind: "unknown"` is expected for addon-specific `EntryData` subclasses. The generator intentionally preserves their base fields without hard-coding every addon's private model.

## Registry files

### `Types.json`

A type connects a Skript type name such as a number, player, or location to Java parsing, conversion, serialization, and change behavior.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `typeParseOrder` | int, `>= 0` | Required | Zero-based order in which Skript tries registered types for ambiguous text. |
| `name` | string | Optional | Documentation display name. It may be an empty string when docs are disabled. |
| `description` | `array<string>` | Optional | Type documentation. |
| `since` | `array<string>` | Optional | Version notes. |
| `examples` | `array<string>` | Optional | Usage examples. |
| `keywords` | `array<string>` | Optional | Reserved by the shared documentation model; currently normally omitted for types. |
| `requires` | `array<string>` | Optional | Documented plugin requirements. |
| `addon` | `AddonInfo` | Required | Registering plugin. |
| `definitionId` | string | Required | Opaque deterministic type definition ID. |
| `registrationId` | string | Required | Opaque deterministic registration ID; currently equal to `definitionId`. |
| `documentationId` | string | Optional | Skript documentation ID. |
| `hasDocs` | boolean | Required | Whether Skript considers this type documentable. |
| `changer` | change-mode map | Optional | Type-wide changer, if one is registered. Absence means no type changer. |
| `originalClass` | class-name | Required | Java class represented by this Skript type. |
| `classType` | string enum | Required | `Annotation`, `Enum`, `Interface`, `Array`, `Primitive`, `Record`, `Sealed`, `Synthetic`, `MemberClass`, `LocalClass`, `AnonymousClass`, or `Class`. Legacy Java 8 output cannot classify `Record`/`Sealed`. |
| `codeName` | string | Required | Internal Skript type identifier used in patterns and registrations. |
| `superClass` | class-name | Optional | Direct Java superclass. |
| `interfaces` | `array<class-name>` | Required | Direct Java interfaces. Empty is valid. |
| `assignableTo` | `array<string>` | Required | Other registered type `codeName`s whose Java classes can accept this type. Used for type compatibility; self is excluded. |
| `userInputPatterns` | `array<string>` | Optional | Regexes accepted by this type's parser. Omitted when no direct text parser patterns exist. |
| `noun` | `NounData` | Required | Localized singular/plural name and grammatical gender. |
| `serializeAs` | class-name | Optional | Another registered Java class whose serializer is reused. |
| `usage` | `array<string>` | Optional | Human-readable accepted values. Enum types usually list normalized enum constants. |
| `defaultExpressionClass` | class-name | Optional | Java class providing a context-dependent default value of this type. |
| `hasParser` | boolean | Required | Whether text can be parsed directly into this type. |
| `hasSerializer` | boolean | Required | Whether values can be persisted by Skript. |
| `hasSupplier` | boolean | Required | Whether the type supplies values through the newer supplier API. Legacy adapter reports `false`. |
| `properties` | `array<string>` | Required | Property names registered for this type. Legacy adapter currently emits `[]`; use `Properties.json` when available. |
| `before` | `array<string>` | Optional | Type code names this type explicitly requests to be parsed before. Current adapter only; omitted when empty. |
| `after` | `array<string>` | Optional | Type code names this type explicitly requests to be parsed after. Current adapter only; omitted when empty. |

`NounData` fields:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `key` | string | Required | Localization key. |
| `value` | string | Optional | Raw localized value before Skript splits plural/gender markers. |
| `singular` | string | Required | Singular display form. |
| `plural` | string | Required | Plural display form. |
| `gender` | int | Required | Language-local gender index. `-3` means no gender, `-2` means plural-only, and nonnegative values index the active language's genders; do not assume a universal upper bound. |
| `genderId` | string | Required | Stable language-facing gender token. Prefer this over interpreting the numeric index across languages. |

### `Functions.json`

A function is a named callable with typed parameters and an optional return value.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `registrationOrder` | int, `>= 0` | Required | Zero-based function registry order. |
| `name` | string | Normally required | Callable name. Consumers should allow omission only for malformed/compatibility data. |
| `description`, `since`, `examples`, `keywords`, `requires` | `array<string>` | Optional | Documentation arrays, omitted when empty. |
| `returnType` | class-name | Optional | Java result type; omission means no returned value. |
| `returnTypeIsSingle` | boolean | Required | Whether one value rather than an array/list is returned. |
| `parameters` | `array<ParameterInfo>` | Required | Ordered parameter list. `[]` means a no-argument function. |
| `addon` | `AddonInfo` | Required | Registering plugin. |
| `definitionId` | string | Required | Opaque ID grouping the named function definition. |
| `registrationId` | string | Required | Opaque ID for the complete signature. |

`ParameterInfo` has required `name`, `type` (class-name), `single` (whether one value is expected), and `modifiers` (possibly empty). A parameter whose Java type is an array normally has `single: false`.

Each modifier has required `type`: `optional`, `keyed`, `range`, or `unknown`. `optional` allows omission/defaulting. `keyed` preserves list keys alongside values. `range` adds inclusive `min` and `max` values; their JSON type and valid range depend on the parameter's comparable type, with the invariant `min <= max`. `min` and `max` are omitted for other modifier kinds. The legacy adapter only reconstructs `optional`.

### `Converters.json`

A converter lets Skript automatically turn a value of one Java type into another when a syntax or function expects the target type. Converters may be chained, so they are part of expression compatibility and overload selection, not just runtime convenience.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `from` | class-name | Required | Input Java type. |
| `to` | class-name | Required | Output Java type. |
| `flags` | int bitmask | Required | Conversion restrictions. Preserve unknown bits. Standard bits are `1` no left chaining, `2` no right chaining, `4` allow unsafe casts, and `8` disallow use for command arguments; `0` allows normal chaining. |
| `registrationOrder` | int, `>= 0` | Required | Direct converter registration order. |
| `addon` | `AddonInfo` | Required | Registering plugin. |
| `registrationId` | string | Required | Opaque deterministic registration ID. |

Generated internal `ChainedConverter` instances are excluded; the file contains direct registrations from which chains can be reconstructed.

### `Comparators.json`

A comparator tells Skript how two types relate. Equality conditions use it, and ordering-aware comparators can also support greater/less comparisons and sorting.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `registrationOrder` | int, `>= 0` | Required | Comparator registry order. |
| `firstType` | class-name | Required | Expected left operand type. |
| `secondType` | class-name | Required | Expected right operand type. |
| `supportsOrdering` | boolean | Optional | Whether results distinguish less/equal/greater rather than equality only. |
| `supportsInversion` | boolean | Optional | Whether Skript may safely reuse the comparator with operand types reversed. |
| `addon` | `AddonInfo` | Required | Registering plugin. |
| `registrationId` | string | Required | Opaque deterministic registration ID. |

Optional booleans are omitted only when an old API cannot expose them. Older comparator APIs are treated as inversion-capable when that was their implicit behavior.

### `EventValues.json`

An event value is a typed value available only while handling a compatible Bukkit event, such as the player involved in an event. The same records are embedded into each `Events.json.eventValues` list after event inheritance and exclusion filtering.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `eventClass` | class-name | Required | Bukkit event class that owns the registration. Subclasses may inherit it unless excluded. |
| `valueClass` | class-name | Required | Java type returned to Skript. |
| `time` | int enum | Required | Exactly `-1` past/before-event state, `0` current/default state, or `1` future/after-event state. |
| `excludeErrorMessage` | string | Optional | Error shown when an excluded event tries to use this value. |
| `excludes` | `array<class-name>` | Optional/versioned | Event subclasses that must not inherit this value. Empty explicitly means none in newer APIs. |
| `resolutionOrder` | int, `>= 0` | Required | Order in which event-value lookup considers this record across time-state lists. |
| `registrationOrder` | int, `>= 0` | Optional | Captured direct registration order. Omitted when the old API was loaded before the hook could expose it. |
| `patterns` | `array<string>` | Modern API only | Registry-defined textual patterns/qualifiers for the event value. Empty means no extra qualifier. |
| `acceptedChangers` | change-mode map | Modern API only | Ways the event value can be changed. `{}` means read-only. |
| `contextDependent` | boolean | Modern API only | Whether availability depends on more context than the event/value classes alone. |
| `addon` | `AddonInfo` | Required | Registering plugin, possibly `unknown`. |
| `registrationId` | string | Required | Opaque deterministic registration ID. |

Read `Manifest.json.capabilities.eventValueApi`: `legacy` omits modern fields, `modern-2.15` has the modern registry without `contextDependent`, and `modern-2.16` includes it. Detection is by API shape, not only by the version label.

### `Properties.json`

A property is a named capability attached to one or more registered types. It centralizes behavior that would otherwise be implemented as many unrelated expressions/conditions. For example, a property handler may expose a value, answer a boolean question, describe contained element types, or provide `x/y/z/w` coordinates.

Each property record:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `name` | string | Required | Property registry name. |
| `documentationId` | string | Required | Documentation ID. |
| `description` | string | Required | Property-level explanation. |
| `since` | `array<string>` | Optional | Version notes. |
| `handlerClass` | class-name | Required | Base Java handler contract. |
| `relatedTypes` | `array<TypePropertyData>` | Required | Type-specific implementations, sorted by type code name. |
| `addon` | `AddonInfo` | Required | Plugin that registered the property. |
| `registrationId` | string | Required | Opaque deterministic registration ID. |

Each `TypePropertyData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `typeCodeName` | string | Required | Skript code name of the related type. |
| `typeClass` | class-name | Required | Related Java class. |
| `description` | string | Optional | Type-specific property documentation. |
| `provider` | `AddonInfo` | Optional | Provider of the type-specific documentation/implementation when distinct. |
| `handlerClass` | class-name | Required | Concrete handler class; hidden lambda classes are normalized to the public base class. |
| `handlerKind` | string enum | Required | `expression`, `condition`, `contains`, `typedValue`, `wxyz`, or `custom`. |
| `returnType` | class-name | Expression handler only | Primary value type returned by an expression property. |
| `possibleReturnTypes` | `array<class-name>` | Expression handler only | All declared result classes. Empty is valid. |
| `acceptedChangers` | change-mode map | Expression handler only | Mutations accepted by the property value. |
| `requiresSourceExpressionChange` | boolean | Expression handler only | Whether changing the property also requires the source expression itself to support change. |
| `expressionMetadataState` | resolution state | Expression handler only | `resolved` or `unresolved`; absent for non-expression handlers. |
| `elementTypes` | `array<class-name>` | Contains handler only | Value classes that may be contained by this property. |
| `supportedAxes` | `array<string>` | WXYZ handler only | Supported coordinate axes from `w`, `x`, `y`, and `z`. |

`typedValue` is a specialized handler that associates another typed value; `custom` preserves addon-defined handlers without inventing fields for their private API.

### Arithmetic files

Arithmetic is split into three registries:

- An **operator** defines a symbol and its precedence.
- An **operation** says that one operator accepts a particular left type and right type and returns a particular type.
- A **difference** calculates a relative distance between two values of one type, such as two dates producing a timespan.

#### `Operators.json`

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `sign` | string | Required | Operator symbol, for example `+`, `-`, `*`, `/`, or `^`. Addons may register other symbols. |
| `priority` | `Priority` | Required | Precedence relative to other operators. |
| `key` | string | Optional | Localization/registry key. |
| `registrationOrder` | int, `>= 0` | Required | Operator order. |
| `addon` | `AddonInfo` | Required | Registering plugin. |
| `registrationId` | string | Required | Opaque deterministic ID. |

#### `Operations.json`

Root: `object<string, array<OperationData>>`. Each key is an operator sign. Every operation object has required `operatorSign` (same sign), `left`, `right`, and `returnType` class names, `registrationOrder >= 0`, `addon`, and `registrationId`. An operator may map to `[]` when no operations are registered for it.

#### `Differences.json`

Each record has required `type` (input class), `returnType` (difference result class), `registrationOrder >= 0`, `addon`, and `registrationId`.

### `ClassHierarchy.json`

This file closes the Java type graph over every class referenced elsewhere, including superclasses, interfaces, and array components. It lets a consumer answer assignability questions without loading server classes.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `name` | class-name | Required | Stable source-style name used by all other files. |
| `binaryName` | string | Required | JVM binary name. Nested classes use `$`; arrays use descriptors such as `[Ljava.lang.String;`. |
| `kind` | class-type enum | Required | Same values as `Types.json.classType`. |
| `superClass` | class-name | Optional | Direct superclass. Omitted for roots/interfaces where Java reports none. |
| `interfaces` | `array<class-name>` | Required | Direct interfaces, sorted. Empty is valid. |
| `componentType` | class-name | Array only | Component class of an array. |
| `provider` | `AddonInfo` | Optional | Plugin whose classloader/code source owns the class. Core/JDK or unresolved classes may have no provider. |

### `Aliases.json`

Aliases map human item/block text to resolved Bukkit items. They are especially important on older Minecraft/Skript combinations where legacy material names and durability/data values are common.

The root has two required fields:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `aliases` | `object<string, int>` | Required | Exact recognized text to index in `targets`. Every index satisfies `0 <= index < targets.length`. Keys are sorted. |
| `targets` | `array<AliasTargetData>` | Required | Deduplicated resolved targets. Unreferenced target entries are not generated. |

`AliasTargetData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `amount` | int | Required | Stack/item amount carried by the alias. Normally positive; no universal range is imposed because addon and historical APIs differ. |
| `all` | boolean | Required | Whether the target means all matching variants rather than one exact variant. |
| `types` | `array<AliasItemData>` | Required | Resolved item alternatives. Empty is schema-valid. |

`AliasItemData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `material` | string | Required | Bukkit `Material` enum name. |
| `minecraftId` | string | Optional | Namespaced Minecraft ID when Skript can expose it. |
| `durability` | int | Required | Legacy durability/data value. Valid semantics depend on the Minecraft/Bukkit version; do not enforce a modern fixed range. |
| `plain` | boolean | Required | Skript's flag for an unmodified/plain item match. |
| `alias` | boolean | Required | Whether the item data itself was produced through alias indirection. |
| `blockValues` | any normalized JSON value | Optional | Version-specific block-state values. Unknown objects become `{ "type": "...", "state": "unresolved" }`; cycles use `state: "cycle"`. |
| `itemMeta` | object | Optional | Bukkit-serialized item metadata, recursively normalized. |

Only the global provider is read: built-in Skript aliases and aliases registered globally by addons are included. Per-script `aliases:` sections use script-local providers and are intentionally excluded, so user script contents never enter the snapshot.

### `PluralRules.json`

Root: object. Skript uses this table when it decides whether a type-pattern word is plural and when it produces an English plural. Rules are emitted in effective runtime priority order; consumers must evaluate `rules` from the lowest `ruleOrder` upward.

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `algorithm` | string enum | Required | `legacy-first-match`, `singular-aware`, or `unresolved` only for a contract-created empty root. Generated snapshots use one of the first two values. |
| `pluralOverrideSupported` | boolean | Required | Whether this Skript runtime exposes `Utils.addPluralOverride(String, String)`. |
| `rules` | `array<PluralRuleData>` | Required | Effective conversion table. A real generated snapshot contains at least the built-in fallback rule. |

`PluralRuleData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `ruleOrder` | int, `>= 0` | Required | Contiguous zero-based evaluation order. Lower values have higher priority. |
| `singular` | string | Required | Singular suffix or complete word. The built-in fallback intentionally uses an empty string. |
| `plural` | string | Required | Plural suffix or complete word. |
| `completeWord` | boolean | Version-dependent | Present for `singular-aware`; `true` requires the whole input word to match rather than only its suffix. Omitted for legacy pair tables because that metadata did not exist. |
| `origin` | string enum | Required | `built-in` for a rule shipped by Skript, or `override` for a successful runtime `addPluralOverride` call. |
| `overrideRegistrationOrder` | int, `>= 0` | Override only | Chronological order among captured override calls. Overrides are added to the front, so this order is normally the reverse of their effective `ruleOrder`. |
| `addon` | object | Required | Plugin that owns the rule. Built-in rules point to Skript; override rules point to the calling addon. |

`legacy-first-match` scans plural endings directly and converts the first match to its singular form. `singular-aware` first checks whether the input already matches a known singular ending, then scans plural endings; this avoids classifying many singular words as plural. Both algorithms are part of the data contract because identical rule pairs can behave differently under them.

The current adapter instruments `Utils.addPluralOverride` before Skript loads. This preserves duplicate overrides, call order, and the caller addon rather than guessing ownership from the final table. The legacy adapter reads the older static `String[][]` table reflectively; supported legacy profiles do not expose the override API.

Skript sources: [legacy `Utils.java` in 2.6.4](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/util/Utils.java) and [singular-aware `Utils.java` in 2.14.3](https://github.com/SkriptLang/Skript/blob/2.14.3/src/main/java/ch/njol/skript/util/Utils.java). Generator paths: `snapshot-contract/src/main/java/jp/nlaocs/skriptSyntaxGenerator/generator/PluralRulesReader.java` and `src/main/java/jp/nlaocs/skriptSyntaxGenerator/hook/RegisterPluralOverrideHook.java`.

## `Manifest.json`

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `schemaVersion` | int | Required | Exact value `3` for this document. Reject or negotiate unknown major schema values. |
| `snapshotId` | sha256 | Required | Identity derived from schema, content, server, language, plugin list, capabilities, and file list. |
| `contentDigest` | sha256 | Required | Digest of the 18 serialized data files, excluding the manifest. |
| `generatedAt` | ISO-8601 string | Required | UTC `Instant` timestamp. It is not part of `snapshotId`. |
| `server` | `ServerManifestData` | Required | Runtime server identity. |
| `language` | string | Required | Active Skript language; legacy collection may use `unknown`. Language affects localized type nouns. |
| `plugins` | `array<PluginManifestData>` | Required | Installed plugins in Bukkit load order. |
| `capabilities` | `SnapshotCapabilitiesData` | Required | API shapes and supported registries. |
| `files` | `array<string>` | Required | Sorted list of all 19 expected filenames, including `Manifest.json`. |

`ServerManifestData` has required string fields `name`, `version`, `bukkitVersion`, `minecraftVersion`, and `javaVersion`.

`PluginManifestData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `loadOrder` | int, `>= 0` | Required | Zero-based Bukkit plugin order. |
| `name`, `version`, `main` | string | Required | Plugin identity and main class. |
| `enabled` | boolean | Required | Whether Bukkit reports the plugin enabled at generation time. |
| `depend`, `softDepend`, `loadBefore` | `array<string>` | Required | Bukkit dependency/load constraints. Empty arrays are preserved. |
| `jarSha256` | sha256 | Optional | Hash of the plugin JAR. Omitted for directory/classpath loading or unreadable code sources. |

`SnapshotCapabilitiesData`:

| Field | Type | Presence | Meaning |
| --- | --- | --- | --- |
| `syntaxApi` | string enum | Required | `legacy-static` for the reflective adapter or `registry` for the current registry API. |
| `eventValueApi` | string enum | Required | `legacy`, `modern-2.15`, or `modern-2.16`. |
| `syntaxKinds` | object | Required | Required booleans: `conditions`, `effects`, `events`, `expressions`, `types`, `functions`, `sections`, `structures`, `properties`, `arithmetic`, `converters`, `comparators`, and `eventValues`. |
| `aliases` | object | Required | Required booleans `supported` and `collected`. `collected: true` implies `supported: true`. |

When a `syntaxKinds` flag is false, its file still exists with the stable empty root.

## Version and historical-source notes

The tested compatibility matrix is maintained in the main README. The important format boundaries are:

| Data | Availability/shape |
| --- | --- |
| Core conditions, effects, events, expressions, sections, types, functions, converters, comparators, event values | Collected in every tested version from 2.6.4 through 2.16.0. |
| Structures | `Structures.json` is empty on 2.6.4 because there is no enumerable structure registry. Registrations appear from 2.7.x. Rich `entryValidator`/`nodeType` fields require the current adapter (2.14+). |
| Arithmetic registries | Empty on 2.6.4 and 2.7.3. The enumerable `Arithmetics` registry appears in 2.8.0; all three arithmetic files are collected from 2.8.x onward. |
| Properties | `Properties.json` is empty before 2.13.0. |
| Expression multiplicity/changers and implementation metadata | Legacy adapter keeps multiplicity/changer state unresolved and omits current implementation metadata. Current adapter resolves it where bytecode/instance inspection is safe. |
| Event values | Legacy shape through 2.14.x; modern registry fields from 2.15.x. Always trust `eventValueApi`. |
| Global aliases | Collected in every tested version. Script-local aliases are always excluded. |

### Structures before 2.7

Skript 2.6.4 handles command, function, options, variables, aliases, and event top-level nodes through hard-coded branches in [`src/main/java/ch/njol/skript/ScriptLoader.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/ScriptLoader.java). There is no registry to enumerate, so the generator does not synthesize records from script-loader control flow. In 2.7.3, the registered model can be seen in [`Structure.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/org/skriptlang/skript/lang/structure/Structure.java) and [`StructCommand.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/structures/StructCommand.java).

Generator paths: `legacy/src/main/java/jp/nlaocs/skriptSyntaxGenerator/legacy/LegacySnapshotGenerator.java` checks for `Skript.getStructures()`, and `LegacySyntaxCollector.java` only serializes actual registry entries.

### Arithmetic before 2.8

Skript 2.7.3 has arithmetic behavior, but its operators and expression parsing are hard-coded in [`Operator.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/Operator.java), [`ExprArithmetic.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/ExprArithmetic.java), and [`ArithmeticChain.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/ArithmeticChain.java). Per-type legacy math hooks also live in [`ClassInfo.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/classes/ClassInfo.java). Therefore an empty arithmetic snapshot on 2.6/2.7 means "no supported enumerable registry", not "Skript cannot do arithmetic".

The registry collected by this project was introduced in 2.8.0; see [`Arithmetics.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/Arithmetics.java), [`Operator.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/Operator.java), and [`OperationInfo.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/OperationInfo.java).

Generator paths: `legacy/src/main/java/jp/nlaocs/skriptSyntaxGenerator/legacy/LegacyArithmeticCollector.java` requires `org.skriptlang.skript.lang.arithmetic.Arithmetics`; `LegacySnapshotGenerator.java` uses the same class for the `arithmetic` capability.

### Properties and event-value API changes

The property registry boundary is represented by [`Property.java` in Skript 2.13.0](https://github.com/SkriptLang/Skript/blob/2.13.0/src/main/java/org/skriptlang/skript/lang/properties/Property.java). The modern event-value model can be inspected in [`EventValue.java`](https://github.com/SkriptLang/Skript/blob/2.15.4/src/main/java/org/skriptlang/skript/bukkit/lang/eventvalue/EventValue.java) and [`EventValueRegistry.java`](https://github.com/SkriptLang/Skript/blob/2.15.4/src/main/java/org/skriptlang/skript/bukkit/lang/eventvalue/EventValueRegistry.java).

### Alias scope

The old global alias store is visible in [`Aliases.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/Aliases.java) and [`AliasesProvider.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/AliasesProvider.java). Script-local behavior is separate in [`ScriptAliases.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/ScriptAliases.java), which is why it is deliberately outside `Aliases.json`.

## Consumer checklist

1. Parse `Manifest.json` and validate `schemaVersion` before other files.
2. Use `capabilities` to interpret empty roots and versioned fields.
3. Preserve registration and resolution order.
4. Treat stable IDs as opaque; do not recreate them from display fields.
5. Distinguish omitted/unresolved values from resolved empty arrays or objects.
6. Resolve `Aliases.json.aliases[text]` through `targets[index]` and validate the index.
7. Use `ClassHierarchy.json` and `Types.json.assignableTo` for assignability rather than assuming Java classes are available to the LSP process.
8. Apply `PluralRules.json.rules` in `ruleOrder` and select behavior from `algorithm`; do not substitute a built-in English inflector.
9. Regenerate the snapshot whenever the server, Skript, addons, addon versions, language, or plugin load order changes.
