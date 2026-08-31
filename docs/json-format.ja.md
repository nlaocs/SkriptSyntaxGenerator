# スナップショットJSON形式

[English](json-format.md) | 日本語

この文書は、`/skgen` が出力するschema version `5`の20ファイルと、各ファイルが表すSkriptの概念を説明します。SkriptのJava APIを知らなくても生成物を利用できることを目的としています。

## 形式の読み方

### フィールドの有無とnull

generatorはJacksonの`NON_NULL`設定でDTOを直列化します。後述する不透明なpayloadを除き、次の規則になります。

- **必須**: 常に存在し、JSONの`null`にはなりません。
- **省略可**: 値がない場合はフィールド自体が省略されます。JSONの`null`としては出力されません。
- 必須フィールドの空配列・空objectには意味があります。例えば`eventValues: []`は「既知のevent valueがない」、state付きフィールドの省略と`"unresolved"`は「安全に取得できなかった」を表します。
- `description`や`examples`などのdocumentation配列は、空なら省略されます。

最初に`Manifest.json`を読んでください。Skriptのversion文字列だけで対応状況を推測しないでください。また、対応するregistryにaddonの登録が0件という場合もあるため、空ファイルだけを見て「未対応」とは判断できません。

### 表記

| 表記 | 意味 |
| --- | --- |
| `array<T>` | 要素が`T`であるJSON配列。 |
| `object<string, T>` | 文字列をkeyとするmap形式のJSON object。 |
| `class-name` | `java.lang.String`のような安定したJava class名。配列は`java.lang.String[]`形式。 |
| `int` | より狭い範囲を明記しない限り、Java/Kotlinの32-bit符号付き整数。 |
| `sha256` | 小文字16進数64文字。 |

### IDと順序

`definitionId`、`registrationId`、`snapshotId`は決定的に生成されるIDです。現在は可読なprefixとSHA-256由来の部分を含みますが、利用側では文字列全体を不透明なIDとして扱ってください。

- `definitionId`: 同じsyntax実装による登録をまとめるID。
- `registrationId`: patternやfunction signatureなど登録内容のID。共通syntaxでは重複出現回数も含みますが、一部の非syntax registryでは同一内容の重複登録が同じIDになり得るため、重複処理なしで配列の唯一keyにはしないでください。
- `registrationOrder`: 各registry内の0始まりの登録順。解析や候補解決に影響するため、alphabetical sortで置き換えてはいけません。
- `typeParseOrder`と`resolutionOrder`も、それぞれのregistry内の0始まりの順序です。

## ファイル一覧

| ファイル | root | 内容 |
| --- | --- | --- |
| `Manifest.json` | object | snapshot、server、plugin、capability、ファイル一覧。 |
| `Conditions.json` | array | `if`や`while`などで使う真偽判定。 |
| `Effects.json` | array | 何らかの処理を実行する文。 |
| `Events.json` | array | code blockを開始するevent triggerの見出し。 |
| `Expressions.json` | array | 1個以上の値を返す構文。 |
| `Sections.json` | array | 実行コード内でインデントされたblockを持つ構文。 |
| `Structures.json` | array | top-level宣言やconfig nodeとして解析される構文。 |
| `Types.json` | array | Skript上の値型と対応するJava class。 |
| `Functions.json` | array | 登録済みfunctionとsignature。 |
| `Converters.json` | array | ある値型から別の値型への自動変換規則。 |
| `Comparators.json` | array | 2つの値型を比較する規則。 |
| `EventValues.json` | array | Bukkit eventから取得できる値。 |
| `Properties.json` | array | 「locationを持つ」など、複数の型に付与される名前付き能力。 |
| `Operators.json` | array | 算術記号と優先順位。 |
| `Operations.json` | object | operatorごとのleft/right型と結果型の組み合わせ。 |
| `Differences.json` | array | 2値間の距離・差を求める規則。 |
| `ClassHierarchy.json` | array | snapshotで使われるJava classの継承関係。 |
| `Aliases.json` | object | global登録されたitem/block名と解決結果。 |
| `Language.json` | object | Skriptのglobal language registryにロードされた実効key/value。 |
| `PluralRules.json` | object | runtime優先順に並んだ英語の単数形・複数形変換rule。 |

全ファイルが常に出力されます。空rootは、配列ファイルが`[]`、`Operations.json`と`Language.json`が`{}`、`Aliases.json`が`{"aliases":{},"targets":[]}`、`PluralRules.json`が`{"algorithm":"unresolved","pluralOverrideSupported":false,"rules":[]}`です。

## 共通object

### `AddonInfo`

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `name` | string | 必須 | 登録元Bukkit plugin名。所有元を特定できない場合は`unknown`。 |
| `version` | string | 必須 | plugin version。所有元未解決時は`unknown`。 |

### `Priority`

Skriptのpriorityは単一の数値ではなく、他priorityとの前後関係です。参照先も`Priority`なので再帰構造になります。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `after` | `array<Priority>` | 必須 | このpriorityより先に試すpriority。`[]`は制約なし。 |
| `before` | `array<Priority>` | 必須 | このpriorityより後に試すpriority。`[]`は制約なし。 |

### ChangeMode map

値の変更方法は`object<string, array<class-name>>`で表します。

| key | Skript上の意味 |
| --- | --- |
| `ADD` | 値を加える、または末尾へ追加する。 |
| `SET` | 現在値を置き換える。 |
| `REMOVE` | 一致する値を取り除く。 |
| `REMOVE_ALL` | 一致するすべての値を取り除く。 |
| `DELETE` | 値を削除・clearする。引数を渡さないため型配列は`[]`になり得る。 |
| `RESET` | default値へ戻す。引数を渡さないため型配列は`[]`になり得る。 |

keyがないmodeは未対応です。keyがある場合、その配列が受け取れる引数classです。map全体が`{}`なら「取得済みで変更不可」です。`...State`と組になるフィールドで、mapが省略されstateが`"unresolved"`なら、安全に判定できなかったことを表します。

### 解決状態

stateは`"resolved"`または`"unresolved"`です。`resolved`は空配列・空objectを含めて値を取得できた状態、`unresolved`は評価が危険または失敗したため対応する値を省略した状態です。値とstateが両方ない場合、そのrecordには適用されないか、そのadapterでは取得対象外です。

## Syntaxファイル

`Conditions.json`、`Effects.json`、`Events.json`、`Expressions.json`、`Sections.json`、`Structures.json`は次の共通フィールドを持ちます。

### 共通syntax record

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `kind` | string enum | 必須 | `condition`、`effect`、`event`、`expression`、`section`、`structure`のいずれか。 |
| `registrationOrder` | int, `>= 0` | 必須 | そのsyntax registry内の0始まりの順序。 |
| `name` | string | 省略可 | documentation用の表示名。 |
| `id` | string | 省略可 | registry ID。現在は主にevent登録で使用。 |
| `documentationId` | string | 省略可 | Skript documentationの安定ID。 |
| `elementClass` | class-name | 必須 | syntaxを実装するJava class。 |
| `superClass` | class-name | 省略可 | 直接のJava superclass。superclassがない場合や旧adapterで取れない場合は省略。 |
| `since` | `array<string>` | 省略可 | version情報。空なら省略。 |
| `description` | `array<string>` | 省略可 | 説明文。空なら省略。 |
| `examples` | `array<string>` | 省略可 | Skript source例。空なら省略。 |
| `keywords` | `array<string>` | 省略可 | documentation検索語。主にcurrent registry adapterで取得。 |
| `requires` | `array<string>` | 省略可 | documentationに書かれた必要plugin・platform機能。解決済み依存graphではない。 |
| `noDoc` | boolean | 必須 | Skriptの生成documentationから意図的に隠すsyntaxなら`true`。 |
| `events` | `array<string>` | 省略可 | `@Events` annotation由来の人間向けevent名。`supportedEvents`とは別物。 |
| `deprecated` | boolean | 通常必須 | 実装classにJavaのdeprecated指定があるか。互換性のため省略も許容すること。 |
| `priorityStr` | string enum風 | 省略可 | `SyntaxInfos.SIMPLE`、`SyntaxInfos.COMBINED`、`SyntaxInfos.PATTERN_MATCHES_EVERYTHING`、`EventValueExpression.DEFAULT_PRIORITY`、`PropertyExpression.DEFAULT_PRIORITY`、`CUSTOM`。 |
| `priority` | `Priority` | 省略可 | 完全な解析順制約。`priorityStr`が既知でも保持する。 |
| `patterns` | `array<string>` | 必須 | 登録されたSkript parser pattern。schema上は`[]`も可。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `definitionId` | string | 必須 | 実装定義の不透明な決定的ID。 |
| `registrationId` | string | 必須 | exact patternと重複回数を含む登録ID。 |
| `relatedProperty` | string | 省略可 | `@RelatedProperty` annotationのproperty名。 |
| `supportedEvents` | `array<class-name>` | state依存 | `EventRestrictedSyntax`が利用可能なBukkit event class。`resolved`かつ`[]`なら対応eventなし。 |
| `supportedEventsState` | resolution state | 省略可 | `supportedEvents`の取得状態。 |
| `experimentalSyntax` | `ExperimentalSyntaxData` | state依存 | script単位のexperiment制約。 |
| `experimentalSyntaxState` | resolution state | 省略可 | `experimentalSyntax`の取得状態。 |
| `returnHandler` | `ReturnHandlerData` | state依存 | body内の`return`を受け取るsyntaxのmetadata。 |
| `returnHandlerState` | resolution state | 省略可 | `returnHandler`の取得状態。 |

`events`はdocumentation用文字列です。`supportedEvents`は実装から返されたJava event classなので、機械的なcontext検証には後者を使います。

### `ExperimentalSyntaxData`

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `required` | `array<ExperimentData>` | 必須 | 有効でなければならないexperiment。`[]`も有効。 |
| `disallowed` | `array<ExperimentData>` | 必須 | 有効であってはならないexperiment。`[]`も有効。 |
| `errorMessage` | string | 必須 | 制約不一致時のmessage。 |

`ExperimentData`は、必須の`codeName`（experiment識別token）、`phase`（小文字のSkript lifecycle phase）、`known`（Skriptが既知のexperimentとして認識するか）を持ちます。

### `ReturnHandlerData`

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `returnValueType` | class-name | 省略可 | `return`で受け取れるJava型。型指定がない場合は省略。 |
| `singleReturnValue` | boolean | 必須 | collectionではなく単一値を返す必要があるか。 |

### `Conditions.json`

rootは`kind: "condition"`の`array<CommonSyntaxRecord>`です。conditionはtrue/falseを返し、制御構文などで使われます。固有フィールドはありません。

### `Effects.json`

rootは`kind: "effect"`の`array<CommonSyntaxRecord>`です。effectは処理を実行し、それ自体は値を返しません。固有フィールドはありません。

### `Events.json`

| 追加フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `referenceEvents` | `array<class-name>` | 必須 | このSkript eventを発火させるBukkit event class。 |
| `eventValues` | `array<EventValueRecord>` | 必須 | 継承・除外判定後にこのeventで使えるevent value。`[]`は明確に0件。 |
| `cancellable` | boolean | 必須 | 参照Bukkit eventの少なくとも1つが`Cancellable`を実装するか、`BlockCanBuildEvent`なら`true`。Skript本家のevent documentation判定と同じ。 |
| `prioritySupported` | boolean | 省略可 | 登録Skript event instanceの`isEventPrioritySupported()`結果。runtimeでinstanceを生成・検査できない場合は省略。 |
| `hasOnPrefix` | boolean | 必須 | 登録表示名が`On `で始まるか。event documentation名の正規化に利用できる。 |

inlineのevent valueは`EventValues.json`と同じ形です。

### `Expressions.json`

expressionはplayer、location、number、listなどの値を生成する構文です。

| 追加フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `returnType` | class-name | 省略可 | 宣言されたJava結果型。Skriptが公開しない場合は省略。 |
| `returnTypeState` | string enum | 必須 | 宣言型が確定値なら`static`、初期化やcaptureで変わるなら`dynamic`、bytecode解析で判定不能なら`unresolved`。 |
| `possibleReturnTypes` | class-name配列 | state依存 | bytecode解析で到達可能と証明できた返り値型。具体型を証明できない場合は省略。 |
| `possibleReturnTypesState` | string enum | 必須 | `possibleReturnTypes`が網羅的かを`complete`、`partial`、`unresolved`で示す。 |
| `sectionExpression` | boolean | 必須 | 値を返しながらindented sectionも持てるexpressionか。 |
| `returnTypeMultiplicity` | string enum | state依存 | `SINGLE`、`MULTIPLE`、`BOTH`。`BOTH`は委譲先やruntime入力によって個数が変わる。 |
| `returnTypeMultiplicityState` | resolution state | 必須 | bytecodeと継承関係からmultiplicityを安全に判定できたか。 |
| `acceptedChangers` | ChangeMode map | state依存 | 対応する変更方法と引数型。`{}`は取得済みでread-only。 |
| `acceptedChangersState` | resolution state | 必須 | changer metadataを安全に判定できたか。 |

Multiplicityは「1回の評価で返す値の個数」であり、「候補Java型の個数」ではありません。`partial`は下限なので、期待型が配列にないことだけを理由にdynamic expressionを除外できません。legacy adapterは実装解析系stateを意図的に`unresolved`とし、例外的な構文はLSP側で上書きできます。

### `Sections.json`

sectionは実行コード中でindented blockを持ちます。別categoryにも登録されることがあるため、以下は排他的分類ではなくJava継承関係です。

| 追加フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `loopSection` | boolean | 必須 | `LoopSection`であり、loop valueなどloop固有機能を持つか。 |
| `effectSection` | boolean | 必須 | block形式のeffectである`EffectSection`か。 |

### `Structures.json`

structureはtop-level/configuration syntaxです。該当versionではcommandやfunction宣言もstructureとして登録されます。

| 追加フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `entryValidator` | `EntryValidatorData` | 省略可 | structure内で認める名前付きentryのschema。current adapterのみ。 |
| `nodeType` | string enum | 省略可 | `SIMPLE`（1行）、`SECTION`（indented block）、`BOTH`（両方）。current adapterのみ。 |

`EntryValidatorData`は必須フィールド`entryData`を持ち、その型は`array<EntryDataInfo>`です。`[]`は名前付きentryがないか、再帰cycleの収集を打ち切ったことを示します。

| `EntryDataInfo`フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `key` | string | 必須 | structure内で期待するentry名。 |
| `defaultValue` | JSON化可能な任意値 | 省略可 | scalar、enum名、regex、list、map、fallback文字列へ正規化したdefault。 |
| `optional` | boolean | 必須 | 省略できるentryか。 |
| `multiple` | boolean | 必須 | 同じentryを複数回書けるか。 |
| `entryDataClass` | class-name | 必須 | entry ruleのJava実装class。 |
| `kind` | string enum | 必須 | `literal`、`variableString`、`expression`、`trigger`、`container`、`section`、`keyValue`、`unknown`。 |
| `separator` | string | 省略可 | keyとvalueの区切り。 |
| `valueType` | class-name | 省略可 | literal entryが要求するJava型。 |
| `stringMode` | string | 省略可 | variable stringの解析modeをJava文字列表現で保存。 |
| `returnTypes` | `array<class-name>` | 省略可 | expression entryが受け入れる結果型。 |
| `flags` | int bitmask | 省略可 | expression parser flag。現行Skriptは`1`がexpression、`2`がliteralなのでbuilt-in組み合わせは`1..3`。未知bitも保持する。 |
| `nestedValidator` | `EntryValidatorData` | 省略可 | nested container用validator。存在しない場合やcycle検出時は省略。 |

addon独自の`EntryData` subclassは`kind: "unknown"`になり得ます。generatorは各addonのprivate modelをハードコードせず、共通fieldを保持する方針です。

## Registryファイル

### `Types.json`

typeはnumber、player、locationなどのSkript型を、Javaでの解析・変換・保存・変更処理へ結びつけます。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `typeParseOrder` | int, `>= 0` | 必須 | 曖昧なtextを型として解析するときの0始まりの試行順。 |
| `name` | string | 省略可 | documentation表示名。docs無効時に空文字の場合もある。 |
| `description`, `since`, `examples`, `keywords`, `requires` | `array<string>` | 省略可 | documentation情報。`keywords`は現在typeでは通常省略。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `definitionId` | string | 必須 | type定義の不透明な決定的ID。 |
| `registrationId` | string | 必須 | 登録ID。現在は`definitionId`と同じ。 |
| `documentationId` | string | 省略可 | Skript documentation ID。 |
| `hasDocs` | boolean | 必須 | Skriptがdocumentableな型とみなすか。 |
| `changer` | ChangeMode map | 省略可 | type全体に登録されたchanger。省略はchangerなし。 |
| `originalClass` | class-name | 必須 | このSkript型が表すJava class。 |
| `classType` | string enum | 必須 | `Annotation`、`Enum`、`Interface`、`Array`、`Primitive`、`Record`、`Sealed`、`Synthetic`、`MemberClass`、`LocalClass`、`AnonymousClass`、`Class`。Java 8のlegacy出力では`Record`/`Sealed`を分類できない。 |
| `codeName` | string | 必須 | patternやregistryで使うSkript内部型名。 |
| `superClass` | class-name | 省略可 | 直接のJava superclass。 |
| `interfaces` | `array<class-name>` | 必須 | 直接実装するJava interface。`[]`も有効。 |
| `assignableTo` | `array<string>` | 必須 | Java上でこの型を受け取れる、他の登録型の`codeName`。selfは含まない。 |
| `userInputPatterns` | `array<string>` | 省略可 | この型のparserが受け入れるregex。直接text解析patternがなければ省略。 |
| `noun` | `NounData` | 必須 | localized単数・複数名と文法gender。 |
| `serializeAs` | class-name | 省略可 | serializerを借用する別Java class。 |
| `usage` | `array<string>` | 省略可 | 型登録が提供するドキュメント用の説明。機械的にparseできるliteral一覧とは限らない。 |
| `enumValues` | `array<string>` | 省略可 | 正規化したJava enum constant名。このフィールドがあってもSkript parserがあるとは限らないため、`hasParser`も確認する。 |
| `parserPatterns` | `array<string>` | 省略可 | patternを公開する型parserが受理する完全一致の表記。ローカライズされたenumの別名も含む。 |
| `literalValues` | `array<string>` | 省略可 | parserと有限supplierの両方を持つ型について、supplierの全値をparserで文字列化した標準表記。 |
| `typeLiterals` | `array<TypeLiteralData>` | 省略可 | 有限supplier値の構造化情報。型固有の手動補正をせず、parser表記とruntime identityを保持する。 |
| `parserClass` | class-name | 省略可 | この型のSkript parserを実装するruntime class。 |
| `parseContexts` | `array<string>` | 省略可 | parserが入力を受理すると報告した`ParseContext`名。 |
| `defaultExpressionClass` | class-name | 省略可 | context依存のdefault値を供給するclass。 |
| `hasParser` | boolean | 必須 | textからこの型を直接parseできるか。 |
| `hasSerializer` | boolean | 必須 | Skriptが値を永続化できるか。 |
| `hasSupplier` | boolean | 必須 | 有限値iteratorを供給するsupplierを持つか。 |
| `properties` | `array<string>` | 必須 | この型に登録されたproperty名。legacy adapterでは現在`[]`なので、利用可能なら`Properties.json`を見る。 |
| `before` | `array<string>` | 省略可 | この型より後にparseするよう要求したtype code name。つまりこの型が先。current adapterのみで、空なら省略。 |
| `after` | `array<string>` | 省略可 | この型より先にparseするよう要求したtype code name。つまりこの型が後。current adapterのみで、空なら省略。 |

`TypeLiteralData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `text` | string | 必須 | `Parser.toString(value, 0)`が返す標準message表記。 |
| `pluralText` | string | 省略可 | Skriptのplural flagで得た表記。`text`と同じなら省略。 |
| `variableName` | string | 省略可 | 値をSkript変数名へ埋め込む際の安定表記。 |
| `debugText` | string | 省略可 | parserが返すdebug表記。`text`と同じなら省略。 |
| `valueClass` | class-name | 必須 | supplier値のruntime Java class。登録typeのclassより具体的な場合がある。 |
| `representedClass` | class-name | 省略可 | supplier値が公開する引数なし`getType(): Class`の結果。例えばEntityData値なら、表しているBukkit entity classを保持できる。 |
| `enumConstant` | string | 省略可 | supplier値がenumなら正確なJava enum constant名。 |

`NounData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `key` | string | 必須 | localization key。 |
| `value` | string | 省略可 | 複数形・gender markerを分解する前のraw localized value。 |
| `singular` | string | 必須 | 単数表示。 |
| `plural` | string | 必須 | 複数表示。 |
| `gender` | int | 必須 | active language内のgender index。`-3`はgenderなし、`-2`はplural-only、0以上はlanguage定義index。共通の上限はない。 |
| `genderId` | string | 必須 | language上のgender token。languageをまたぐ場合は数値よりこちらを優先する。 |

### `Functions.json`

functionは、型付きparameterと任意の返り値を持つ名前付き呼び出しです。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `registrationOrder` | int, `>= 0` | 必須 | function registry順。 |
| `name` | string | 通常必須 | 呼び出し名。破損・互換dataに備えて省略も許容すること。 |
| `description`, `since`, `examples`, `keywords`, `requires` | `array<string>` | 省略可 | documentation。空なら省略。 |
| `returnType` | class-name | 省略可 | Java結果型。省略は返り値なし。 |
| `returnTypeIsSingle` | boolean | 必須 | array/listではなく単一値を返すか。 |
| `parameters` | `array<ParameterInfo>` | 必須 | 順序付きparameter。`[]`は引数なしfunction。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `definitionId` | string | 必須 | function名による定義ID。 |
| `registrationId` | string | 必須 | 完全なsignatureのID。 |

`ParameterInfo`は必須の`name`、`type`（class-name）、`single`（単一値を要求するか）、`modifiers`（空可）を持ちます。Java型が配列なら通常`single: false`です。

modifierの必須`type`は`optional`、`keyed`、`range`、`unknown`です。`optional`は省略/default使用を許可します。`keyed`はlistのkeyを値と一緒に保持します。`range`はinclusiveな`min`と`max`を持ち、JSON型と値域はparameterのComparable型次第ですが`min <= max`です。それ以外では`min`/`max`を省略します。legacy adapterが復元するのは`optional`だけです。

### `Converters.json`

converterは、構文やfunctionが期待する型に合わせて値を別Java型へ自動変換します。複数converterをchainできるため、runtimeの便利機能だけでなく、expressionの型互換性やoverload選択にも関わります。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `from` | class-name | 必須 | 変換元Java型。 |
| `to` | class-name | 必須 | 変換先Java型。 |
| `flags` | int bitmask | 必須 | chain制約。未知bitも保持する。標準bitは`1`: left chain禁止、`2`: right chain禁止、`4`: unsafe cast許可、`8`: command argumentで使用禁止。`0`は通常chain可。 |
| `registrationOrder` | int, `>= 0` | 必須 | 直接登録されたconverterの順序。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `registrationId` | string | 必須 | 不透明な決定的登録ID。 |

Skriptが内部生成した`ChainedConverter`は除外し、chainを再構築できる直接登録だけを保存します。

### `Comparators.json`

comparatorは2型の値の関係を判定します。等値conditionで使われ、ordering対応なら大小比較やsortにも利用できます。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `registrationOrder` | int, `>= 0` | 必須 | comparator registry順。 |
| `firstType` | class-name | 必須 | left operand型。 |
| `secondType` | class-name | 必須 | right operand型。 |
| `supportsOrdering` | boolean | 省略可 | equalだけでなくless/greaterも区別できるか。 |
| `supportsInversion` | boolean | 省略可 | operand順を逆にして再利用できるか。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `registrationId` | string | 必須 | 不透明な決定的登録ID。 |

旧APIがboolean metadataを公開しない場合のみ省略されます。さらに古いcomparator APIは、当時の暗黙動作に合わせてinversion対応として扱います。

### `EventValues.json`

event valueは、対応するBukkit event処理中だけ使える型付き値です。例えばeventに関係するplayerを提供します。同じrecordが継承・除外判定後に`Events.json.eventValues`へ埋め込まれます。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `eventClass` | class-name | 必須 | 登録先Bukkit event class。除外されない限りsubclassへ継承される。 |
| `valueClass` | class-name | 必須 | Skriptへ返すJava型。 |
| `time` | int enum | 必須 | `-1`=event前/past、`0`=現在/default、`1`=event後/futureのいずれか。 |
| `excludeErrorMessage` | string | 省略可 | 除外eventで使った場合のerror。 |
| `excludes` | `array<class-name>` | 省略可/version依存 | 継承させないevent subclass。新APIでは`[]`が明示的に除外なし。 |
| `resolutionOrder` | int, `>= 0` | 必須 | time-state listをまたいでevent-value検索時に考慮する順序。 |
| `registrationOrder` | int, `>= 0` | 省略可 | hookで取得した直接登録順。旧APIやhook前登録では省略。 |
| `patterns` | `array<string>` | modern APIのみ | event valueの追加text pattern・qualifier。`[]`は追加指定なし。 |
| `acceptedChangers` | ChangeMode map | modern APIのみ | event valueの変更方法。`{}`はread-only。 |
| `contextDependent` | boolean | modern APIのみ | event/value class以外のcontextにも利用可否が依存するか。 |
| `hasCustomInputValidator` | boolean | modern APIのみ | 出力済みpattern以外に、識別子へaddon独自の検証も実行するか。実行できないreaderは結果をunresolvedとして扱う。 |
| `hasCustomEventValidator` | boolean | modern APIのみ | 現在のevent classへaddon独自の検証も実行するか。実行できないreaderは結果をunresolvedとして扱う。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。特定不能なら`unknown`。 |
| `registrationId` | string | 必須 | 不透明な決定的登録ID。 |

`Manifest.json.capabilities.eventValueApi`を確認してください。`legacy`はmodern fieldなし、`modern-2.15`はmodern registryだが`contextDependent`なし、`modern-2.16`はそれも含みます。version名ではなく実際のAPI shapeを検出しています。

### `Properties.json`

propertyは複数の登録型へ付けられる名前付き能力です。多数の無関係なexpression/conditionとして実装する代わりに、値の取得、boolean判定、内包element型、`x/y/z/w`座標などを共通化します。

property record:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `name` | string | 必須 | property registry名。 |
| `documentationId` | string | 必須 | documentation ID。 |
| `description` | string | 必須 | property全体の説明。 |
| `since` | `array<string>` | 省略可 | version情報。 |
| `handlerClass` | class-name | 必須 | 基本handler contract。 |
| `relatedTypes` | `array<TypePropertyData>` | 必須 | type code name順の型別実装。 |
| `addon` | `AddonInfo` | 必須 | property登録元plugin。 |
| `registrationId` | string | 必須 | 不透明な決定的登録ID。 |

`TypePropertyData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `typeCodeName` | string | 必須 | 対象typeのSkript code name。 |
| `typeClass` | class-name | 必須 | 対象Java class。 |
| `description` | string | 省略可 | type固有のproperty説明。 |
| `provider` | `AddonInfo` | 省略可 | 型固有documentation/実装のprovider。 |
| `handlerClass` | class-name | 必須 | 具体handler class。hidden lambdaはpublic base classへ正規化。 |
| `handlerKind` | string enum | 必須 | `expression`、`condition`、`contains`、`typedValue`、`wxyz`、`custom`。 |
| `returnType` | class-name | expressionのみ | 主結果型。 |
| `possibleReturnTypes` | `array<class-name>` | expressionのみ | 宣言された全結果型。`[]`も有効。 |
| `acceptedChangers` | ChangeMode map | expressionのみ | property値の変更方法。 |
| `requiresSourceExpressionChange` | boolean | expressionのみ | property変更時にsource expression側のchange対応も必要か。 |
| `expressionMetadataState` | resolution state | expressionのみ | `resolved`/`unresolved`。非expressionでは省略。 |
| `elementTypes` | `array<class-name>` | containsのみ | 内包できる値型。 |
| `supportedAxes` | `array<string>` | wxyzのみ | `w`、`x`、`y`、`z`のうち対応するaxis。 |

`typedValue`は別の型付き値を関連付ける特殊handlerです。`custom`はaddon独自handlerを、private API用fieldを捏造せず保持します。

### Arithmeticファイル

Arithmeticは3つのregistryに分かれます。

- **operator**: 記号と優先順位を定義。
- **operation**: そのoperatorが受け取れるleft型・right型と結果型を定義。
- **difference**: 同種2値の相対的な距離を計算。例として2つのdateからtimespanを得る処理。

#### `Operators.json`

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `sign` | string | 必須 | `+`、`-`、`*`、`/`、`^`などの記号。addonによる追加も可能。 |
| `priority` | `Priority` | 必須 | 他operatorとの優先順位。 |
| `key` | string | 省略可 | localization/registry key。 |
| `registrationOrder` | int, `>= 0` | 必須 | operator順。 |
| `addon` | `AddonInfo` | 必須 | 登録元plugin。 |
| `registrationId` | string | 必須 | 不透明な決定的ID。 |

#### `Operations.json`

rootは`object<string, array<OperationData>>`で、keyはoperator signです。各objectは必須の`operatorSign`（同じsign）、`left`、`right`、`returnType`（すべてclass-name）、`registrationOrder >= 0`、`addon`、`registrationId`を持ちます。operatorにoperationがなければvalueは`[]`になり得ます。

#### `Differences.json`

各recordは必須の`type`（入力class）、`returnType`（差の結果class）、`registrationOrder >= 0`、`addon`、`registrationId`を持ちます。

### `ClassHierarchy.json`

他のcatalog fileから参照された全classについて、superclass、interface、array componentまで辿ったJava型graphです。LSP processがserver classをloadしなくてもassignabilityを判断できます。methodのparameter/return名はexactなreflection metadataであり、このgraph外のclassを指す場合があります。JDK・pluginの全method signatureを再帰展開するとgraphが無制限に広がり、exactな`methodExists`判定には不要なためです。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `name` | class-name | 必須 | 他ファイルと共通のsource風安定名。 |
| `binaryName` | string | 必須 | JVM binary name。nested classは`$`、配列は`[Ljava.lang.String;`など。 |
| `kind` | class-type enum | 必須 | `Types.json.classType`と同じ。 |
| `superClass` | class-name | 省略可 | 直接superclass。Javaがnoneを返すroot/interfaceでは省略。 |
| `interfaces` | `array<class-name>` | 必須 | sort済みの直接interface。`[]`も有効。 |
| `componentType` | class-name | arrayのみ | 配列要素class。 |
| `methods` | `array<ClassMethodData>` | 必須 | このclassに直接declareされた`Class.getDeclaredMethods()`相当のmethod。visibilityで絞らず、synthetic/bridge methodも保持し、完全signatureで重複排除・sortする。reflection不能時は不完全なschema 5 recordを出さず、生成を失敗させる。 |
| `containerElementType` | class-name | 省略可 | Skriptのruntime `@ContainerType` annotationで宣言された要素class。単一の`Container`値をloop内で複数要素として扱う際に使用する。 |
| `provider` | `AddonInfo` | 省略可 | classloader/code sourceを所有するplugin。JDK/coreや未解決classでは省略可。 |

`ClassHierarchy.json`の各recordには`methods`を必須で含めます。これはそのclassに直接declareされた`Class.getDeclaredMethods()`相当の全methodです。visibilityで絞り込まず、inherited methodはそのclass recordへ追加しません。synthetic/bridge methodも保持します。

`ClassMethodData`:

| field | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `name` | string | 必須 | Java method名。 |
| `parameterTypes` | `array<class-name>` | 必須 | `Method.getParameterTypes()`のexactなraw parameter class名を宣言順で保存します。引数なしは`[]`です。 |
| `returnType` | class-name | 必須 | `Method.getReturnType()`のexactなraw return class名。 |
| `static` | boolean | 必須 | `Modifier.isStatic(method.getModifiers())`の結果。Skriptの`methodExists`相当判定ではvisibilityとstatic flagは条件にせず、name、順序付きparameterTypes、必要ならreturnTypeをexact matchします。 |

signature identityは`name + exact parameterTypes + returnType + static`です。同じclass record内でこの4項目が同じentryだけを1件に正規化し、canonical signature順にsortします。LSP/WASMが`Skript.methodExists`相当を再現する場合は、対象classのrecordだけを参照し、superclassやinterfaceを探索しません。

### `Aliases.json`

aliasは人間向けitem/block textをBukkit itemへ解決します。legacy material名やdurability/data値が多い古いMinecraft/Skript環境で特に重要です。

root:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `aliases` | `object<string, int>` | 必須 | exact認識textから`targets` indexへのmap。全indexは`0 <= index < targets.length`。keyはsort済み。 |
| `targets` | `array<AliasTargetData>` | 必須 | deduplicateされた解決先。参照されないtargetは生成しない。 |

`AliasTargetData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `amount` | int | 必須 | aliasが持つstack/item個数。通常は正数だが、addon・旧API差があるため共通上限は設けない。 |
| `all` | boolean | 必須 | 1つのexact variantではなく、matching variant全体を表すか。 |
| `types` | `array<AliasItemData>` | 必須 | 解決されたitem候補。schema上は`[]`も可。 |

`AliasItemData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `material` | string | 必須 | Bukkit `Material` enum名。 |
| `minecraftId` | string | 省略可 | Skriptが公開できる場合のnamespaced Minecraft ID。 |
| `durability` | int | 必須 | legacy durability/data値。意味と有効範囲はMinecraft/Bukkit version依存。modern固定範囲を強制しない。 |
| `plain` | boolean | 必須 | 無変更/plain item matchを表すSkript flag。 |
| `alias` | boolean | 必須 | item data自体がalias経由で作られたか。 |
| `blockValues` | 正規化済み任意JSON値 | 省略可 | version依存block state。未知objectは`{ "type": "...", "state": "unresolved" }`、cycleは`state: "cycle"`。 |
| `itemMeta` | object | 省略可 | Bukkit serialization後に再帰正規化したitem metadata。 |

読むのはglobal providerだけです。Skript built-in aliasとaddonがglobal登録したaliasは含みますが、各scriptの`aliases:` sectionはscript-local providerに保存されるため意図的に除外します。ユーザーscript内容はsnapshotへ入りません。

### `PluralRules.json`

rootはobjectです。Skriptは、type pattern内の単語が複数形かを判定するときと、英語の複数形を生成するときにこのtableを使います。ruleはruntimeで実際に評価される優先順で出力されるため、利用側は`ruleOrder`の小さい順に処理してください。

| field | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `algorithm` | string enum | 必須 | `legacy-first-match`、`singular-aware`、またはcontractが補う空rootだけで使う`unresolved`。実際の生成物では最初の2値のどちらか。 |
| `pluralOverrideSupported` | boolean | 必須 | このSkript runtimeが`Utils.addPluralOverride(String, String)`を公開しているか。 |
| `rules` | `array<PluralRuleData>` | 必須 | 実効変換table。実際の生成物には最低でもbuilt-in fallback ruleが存在する。 |

`PluralRuleData`:

| field | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `ruleOrder` | int, `>= 0` | 必須 | 0始まりで連続する評価順。小さいほど優先度が高い。 |
| `singular` | string | 必須 | 単数形suffixまたは完全な単語。built-in fallbackでは意図的に空文字列。 |
| `plural` | string | 必須 | 複数形suffixまたは完全な単語。 |
| `completeWord` | boolean | version依存 | `singular-aware`で必須。`true`ならsuffixだけでなく入力単語全体の一致を要求する。metadataが存在しないlegacy pair tableでは省略。 |
| `origin` | string enum | 必須 | Skript同梱ruleの`built-in`、または成功したruntime `addPluralOverride`呼び出しの`override`。 |
| `overrideRegistrationOrder` | int, `>= 0` | overrideのみ | 捕捉したoverride呼び出し間の時系列順。overrideは先頭へ追加されるため、通常は実効`ruleOrder`と逆順になる。 |
| `addon` | object | 必須 | ruleの所有plugin。built-in ruleはSkript、override ruleは呼び出し元addon。 |

`legacy-first-match`は複数形endingを直接走査し、最初の一致を単数形へ戻します。`singular-aware`は、入力が既知の単数形endingに一致するかを先に調べ、その後で複数形endingを走査します。同じrule pairでも結果が変わり得るため、algorithmもデータ契約に含めています。

current adapterはSkriptのload前に`Utils.addPluralOverride`をinstrumentationします。これにより、最終tableから所有者を推測せず、重複override、呼び出し順、呼び出し元addonを保持します。legacy adapterは古いstatic `String[][]` tableをreflectionで読みます。対応legacy profileにはoverride APIがありません。

Skript source: [2.6.4のlegacy `Utils.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/util/Utils.java)、[2.14.3のsingular-aware `Utils.java`](https://github.com/SkriptLang/Skript/blob/2.14.3/src/main/java/ch/njol/skript/util/Utils.java)。Generator側path: `snapshot-contract/src/main/java/jp/nlaocs/skriptSyntaxGenerator/generator/PluralRulesReader.java`、`src/main/java/jp/nlaocs/skriptSyntaxGenerator/hook/RegisterPluralOverrideHook.java`。

### `Language.json`

rootはobjectです。各property名がロード済みlanguage key、property valueがSkriptのlanguage lookupから返される実効文字列です。決定的な出力にするためkey順にsortされます。

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `<language-key>` | string | entry依存 | parser messageやlocalized noun keyなど、runtimeに存在するlanguage key。 |
| `<language-value>` | string | entry依存 | そのkeyにロードされた文字列。空文字列も「未定義」と異なるため保持されます。 |

readerは`localizedLanguage`を先に入れ、`defaultLanguage`を後から入れます。これはcollision時にdefault mapを優先するSkriptの`Language.get_i`と同じです。コピー対象はglobal runtime mapだけで、script fileやscript-local providerは調べません。歴史的な`Language` class、private field、またはstring map shapeを読み取れない場合は、曖昧な空・部分dataを出さずsnapshot生成を失敗させます。

Skript source: [2.6.4の`Language.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/localization/Language.java)、[2.16.0の`Language.java`](https://github.com/SkriptLang/Skript/blob/2.16.0/src/main/java/ch/njol/skript/localization/Language.java)。Generator側path: `snapshot-contract/src/main/java/jp/nlaocs/skriptSyntaxGenerator/generator/LanguageReader.java`。

## `Manifest.json`

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `schemaVersion` | int | 必須 | この文書ではexact `5`。未知のmajor schemaは拒否または別処理する。 |
| `snapshotId` | sha256 | 必須 | schema、content、server、language、plugin list、capability、file list由来のidentity。 |
| `contentDigest` | sha256 | 必須 | Manifestを除く19 data fileのserialized content digest。 |
| `generatedAt` | ISO-8601 string | 必須 | UTC `Instant`。`snapshotId`には含まれない。 |
| `server` | `ServerManifestData` | 必須 | 実行server identity。 |
| `language` | string | 必須 | active Skript language。legacyで取得不能なら`unknown`。type nounにも影響する。 |
| `plugins` | `array<PluginManifestData>` | 必須 | Bukkit load orderのplugin一覧。 |
| `capabilities` | `SnapshotCapabilitiesData` | 必須 | API shapeと対応registry。 |
| `files` | `array<string>` | 必須 | `Manifest.json`を含む20ファイル名のsort済み一覧。 |

`ServerManifestData`は必須stringの`name`、`version`、`bukkitVersion`、`minecraftVersion`、`javaVersion`を持ちます。

`PluginManifestData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `loadOrder` | int, `>= 0` | 必須 | Bukkit pluginの0始まりの順序。 |
| `name`, `version`, `main` | string | 必須 | plugin identityとmain class。 |
| `enabled` | boolean | 必須 | 生成時にBukkitがenabledと報告するか。 |
| `depend`, `softDepend`, `loadBefore` | `array<string>` | 必須 | Bukkitの依存・load制約。空配列も保持。 |
| `jarSha256` | sha256 | 省略可 | plugin JAR hash。directory/classpath loadや読めないcode sourceでは省略。 |

`SnapshotCapabilitiesData`:

| フィールド | 型 | 有無 | 意味 |
| --- | --- | --- | --- |
| `syntaxApi` | string enum | 必須 | reflective adapterの`legacy-static`、またはcurrent registry APIの`registry`。 |
| `eventValueApi` | string enum | 必須 | `legacy`、`modern-2.15`、`modern-2.16`。 |
| `syntaxKinds` | object | 必須 | 必須boolean: `conditions`、`effects`、`events`、`expressions`、`types`、`functions`、`sections`、`structures`、`properties`、`arithmetic`、`converters`、`comparators`、`eventValues`。 |
| `aliases` | object | 必須 | 必須boolean `supported`と`collected`。`collected: true`なら必ず`supported: true`。 |

`syntaxKinds`がfalseでも、対応ファイルは安定した空rootで存在します。

## Version差と当時のsource

検証済みの完全なcompatibility matrixはmain READMEにあります。形式上重要な境界は次の通りです。

| data | 対応状況・shape |
| --- | --- |
| Conditions、Effects、Events、Expressions、Sections、Types、Functions、Converters、Comparators、EventValues | 検証済み2.6.4から2.16.0まで収集。 |
| Structures | 2.6.4には列挙可能なregistryがなく空。2.7.xから登録recordあり。詳細`entryValidator`/`nodeType`はcurrent adapter（2.14+）。 |
| Arithmetic registries | 2.6.4/2.7.3では空。列挙可能な`Arithmetics` registryは2.8.0から。 |
| Properties | 2.13.0より前は空。 |
| Expression multiplicity/changerと実装metadata | legacy adapterはmultiplicity/changerをunresolvedとし、current実装metadataを省略。current adapterはbytecode/instance解析が安全な場合に解決。 |
| EventValues | 2.14.xまではlegacy shape、2.15.xからmodern field。必ず`eventValueApi`を使う。 |
| Global aliases | 全検証versionで収集。script-local aliasは常に除外。 |
| Language registry | 全検証versionでprivate runtime mapから実効key/valueを収集。reflectionできない場合は曖昧な空registryを出力せず、生成を失敗させる。 |

### 2.7より前のStructure

Skript 2.6.4はcommand、function、options、variables、aliases、eventなどのtop-level nodeを、[`src/main/java/ch/njol/skript/ScriptLoader.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/ScriptLoader.java)のハードコードされた分岐で処理します。列挙できるregistryがないため、generatorはScriptLoaderの制御flowからrecordを合成しません。2.7.3の登録modelは[`Structure.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/org/skriptlang/skript/lang/structure/Structure.java)と[`StructCommand.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/structures/StructCommand.java)で確認できます。

generator側path: `legacy/src/main/java/jp/nlaocs/skriptSyntaxGenerator/legacy/LegacySnapshotGenerator.java`が`Skript.getStructures()`を確認し、`LegacySyntaxCollector.java`は実際のregistry entryだけを保存します。

### 2.8より前のArithmetic

Skript 2.7.3にも算術機能はありますが、operatorとexpression解析は[`Operator.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/Operator.java)、[`ExprArithmetic.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/ExprArithmetic.java)、[`ArithmeticChain.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/expressions/arithmetic/ArithmeticChain.java)にハードコードされています。型別の旧math hookは[`ClassInfo.java`](https://github.com/SkriptLang/Skript/blob/2.7.3/src/main/java/ch/njol/skript/classes/ClassInfo.java)にあります。そのため2.6/2.7の空Arithmetic snapshotは「Skriptに算術がない」ではなく「対応する列挙可能registryがない」という意味です。

このprojectが収集するregistryは2.8.0で導入されました。[`Arithmetics.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/Arithmetics.java)、[`Operator.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/Operator.java)、[`OperationInfo.java`](https://github.com/SkriptLang/Skript/blob/2.8.0/src/main/java/org/skriptlang/skript/lang/arithmetic/OperationInfo.java)を参照してください。

generator側path: `legacy/src/main/java/jp/nlaocs/skriptSyntaxGenerator/legacy/LegacyArithmeticCollector.java`は`org.skriptlang.skript.lang.arithmetic.Arithmetics`を要求し、`LegacySnapshotGenerator.java`も同じclassで`arithmetic` capabilityを判定します。

### PropertyとEventValue API

Property registryの境界は[Skript 2.13.0の`Property.java`](https://github.com/SkriptLang/Skript/blob/2.13.0/src/main/java/org/skriptlang/skript/lang/properties/Property.java)で確認できます。modern event-value modelは[`EventValue.java`](https://github.com/SkriptLang/Skript/blob/2.15.4/src/main/java/org/skriptlang/skript/bukkit/lang/eventvalue/EventValue.java)と[`EventValueRegistry.java`](https://github.com/SkriptLang/Skript/blob/2.15.4/src/main/java/org/skriptlang/skript/bukkit/lang/eventvalue/EventValueRegistry.java)を参照してください。

### Aliasの範囲

旧global alias storeは[`Aliases.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/Aliases.java)と[`AliasesProvider.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/AliasesProvider.java)で確認できます。script-local処理は別の[`ScriptAliases.java`](https://github.com/SkriptLang/Skript/blob/2.6.4/src/main/java/ch/njol/skript/aliases/ScriptAliases.java)にあるため、`Aliases.json`の対象外です。

## 利用側checklist

1. 最初に`Manifest.json`をparseし、`schemaVersion`を検証する。
2. 空rootとversion依存fieldは`capabilities`を使って解釈する。
3. registration/resolution orderを保持する。
4. stable IDを不透明な文字列として扱い、表示fieldから再生成しない。
5. 省略・unresolvedと、解決済みの空配列・空objectを区別する。
6. `Aliases.json.aliases[text]`を`targets[index]`へ解決し、index範囲を検証する。
7. LSP processにserver classがあると仮定せず、assignabilityには`ClassHierarchy.json`と`Types.json.assignableTo`を使う。
8. 一般的な英語inflectorで代用せず、`PluralRules.json.rules`を`ruleOrder`順に、`algorithm`の動作で適用する。
9. runtime-localized key/valueには`Language.json`を使い、別のhardcode済みlanguage tableを同梱しない。
10. server、Skript、addon、addon version、language、plugin load orderが変わったらsnapshotを再生成する。
