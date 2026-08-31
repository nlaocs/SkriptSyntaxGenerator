# SkriptSyntaxGenerator

[English](README.md) | 日本語

LSPや各種ツールで利用するために、サーバー固有のSkript構文スナップショットを生成します。スナップショットには、安定したスキーマのもとで、使用中のSkriptバージョン、サーバー、プラグイン、登録順序、capability、および19個のデータファイルが記録されます。

生成される全ファイルについて、各フィールド、null・省略、値域、概念、バージョン差を確認するには、[スナップショットJSON形式リファレンス](docs/json-format.ja.md)を参照してください。

## Generatorの成果物

2種類のadapterが、同じ20ファイルのスナップショット契約に従って出力します。

| Skript | 成果物 | 実行環境 |
| --- | --- | --- |
| 2.6.4-2.13.x | `legacy/build/libs/SkriptSyntaxGenerator-legacy-1.0.jar` | Java 8互換、reflectionを使用するSkript API adapter |
| 2.14.x-2.16.x | `build/libs/SkriptSyntaxGenerator-1.0-all.jar` | Java 21、現在のregistry API adapter |

対応する成果物をサーバーの`plugins`ディレクトリに配置し、サーバーを起動して`/skgen`を実行してください。デフォルトでは、ファイルは`plugins/SkriptSyntaxGenerator`に出力されます。サーバー、Skript、導入addon、またはaddonの読み込み順序が変わった場合は、サーバースナップショットを再生成してください。

どちらのadapterも常に同じファイルを出力します。古いSkriptバージョンで利用できない機能は、契約で定めた空のroot（`[]`、`Operations.json`では`{}`、`Aliases.json`、`Language.json`、`PluralRules.json`では文書化されたobject root）として出力され、利用可否は`Manifest.json.capabilities`に記録されます。

## Manifest capabilities

`Manifest.json`はschema version 5を使用し、次の情報を記録します。

- `syntaxApi`: `legacy-static`または`registry`
- `eventValueApi`: `legacy`、`modern-2.15`、または`modern-2.16`
- `syntaxKinds`: 各registryを取得できるかどうか
- `aliases.supported`および`aliases.collected`

`Aliases.json`には、Skriptとaddonによってグローバルに登録されたaliasが保存されます。各スクリプトの`aliases:` sectionで宣言されたaliasはscript-localなchild providerに保存されるため、Generatorのデータモデルには含まれません。したがって、ユーザーが記述したスクリプトの内容がスナップショットに混入することはありません。

`Language.json`には、Skriptのグローバルな`Language` registryにロードされた実効key/value mapが保存されます。Skriptとaddonのruntime mapだけを読み取り、ユーザーの`.sk`ファイルやscript-local providerは読みません。runtime registryを検査できない場合は生成を失敗させるため、空objectはロード済みregistry自体が空だったことだけを表します。

`PluralRules.json`には、runtimeで実際に使われる英語の単数形・複数形table、algorithm、評価順、完全一致の扱い、各runtime overrideの登録元addonを保存します。これによりLSPはruleをhardcodeせず、対象serverのSkriptと同じ解析を再現できます。

Aliasは、認識される文字列から`targets`内のindexへの対応を、ソート済みの`aliases` mapとして欠損なく出力します。重複を除いた`targets`配列には、`amount`、`all`、および解決済みitem typeのMaterial、Minecraft ID、durability、plain/alias flag、block value、存在する場合はitem metaが保存されます。これにより、完全な対応関係を維持しながら、非常に大きなlegacy alias tableをコンパクトに保持できます。

## Skriptバージョンごとの生成データ

`Yes`は、integration profileで1件以上のデータを生成し、検証できたことを表します。`No`は、そのSkriptバージョンにregistryまたは概念が存在しないため、ファイルが契約で定めた空のrootとして出力されることを表します。

主要な構文データ:

| Skript | Conditions | Effects | Events | Expressions | Sections | Structure registry | Types | Functions |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2.6.4 | Yes | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.7.3 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.8.7 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.9.5 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.10.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.11.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.12.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.13.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.14.3 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.15.4 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.16.0 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |

補助registryと関係データ:

| Skript | Arithmetic | Converters | Comparators | Event values | Properties | Class hierarchy | Global aliases | Plural rules |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2.6.4 | No | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.7.3 | No | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.8.7 | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.9.5 | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.10.2 | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.11.2 | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.12.2 | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| 2.13.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.14.3 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.15.4 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.16.0 | Yes | Yes | Yes | Yes | Yes | Yes | Yes | Yes |

`Arithmetic`は、`Operators.json`、`Operations.json`、`Differences.json`を1つのcapabilityとして表します。 `Plural rules`は`PluralRules.json`を表し、対応する全Skript versionにbuilt-in変換tableがあります。addonがruntime overrideを先頭追加できるかは`pluralOverrideSupported`で判定します。Event valueはテスト済みの全バージョンで取得できますが、metadataの形状が異なります。2.6.4-2.14.3では`eventValueApi: legacy`、2.15.4と2.16.0では`modern-2.16`です。Skriptバージョンだけから推測せず、Manifestから実際に検出された形状を参照してください。

Skript 2.6.4には列挙可能なStructure registryがありません。command、function、options、variables、aliases、eventのトップレベル構造は、`ScriptLoader`内の専用分岐で処理されます。このGeneratorは構文宣言を再構築するのではなく、登録された生データを保存するため、これらを`Structures.json`へ擬似的に追加しません。

## 互換性マトリクス

integration suiteでは、現在次の境界をテストしています。

| Skript | Minecraft | Java | Adapter |
| --- | --- | --- | --- |
| 2.6.4 | 1.12.2 | 8 | reflective |
| 2.6.4 | 1.16.5 | 16 | reflective |
| 2.6.4 | 1.17.1 | 17 | reflective |
| 2.6.4 | 1.18.2 | 17 | reflective |
| 2.7.3 | 1.20.2 | 17 | reflective |
| 2.8.7 | 1.20.2 | 17 | reflective |
| 2.9.5-2.13.2 | 1.21 | 21 | reflective |
| 2.14.3-2.16.0 | 1.21.11 | 21 | current API |
| 2.15.4 | 26.1.2 | 25 | experimental current API |
| 2.16.0 | 26.2 | 25 | experimental current API |

`2.6.4 + Minecraft 1.12.2 + Java 8`のprofileは、推測上の対応範囲ではなく、必須の互換性テストです。

Minecraft 1.16.5、1.17.1、1.18.2、26.1.2、26.2は、[SkriptのbStats Minecraft Version chart](https://bstats.org/plugin/bukkit/Skript/722)を基に選んだ代表patchです。chartの値は時間とともに変動します。Paper 26ではplugin class loaderの分離により登録hookを完全には取得できないため、該当profileは明示実行できるexperimental taskとして残し、`integrationTest`からは除外します。対話的な`runServer` taskでは、Paper 1.21.11上でSkript 2.15.4を使用します。

マトリクスを表示するには、次を実行します。

```powershell
.\gradlew.bat integrationMatrix
```

単体テストを実行するには、次を実行します。

```powershell
.\gradlew.bat test
```

すべてのintegration profileを実行するには、次を実行します。

```powershell
.\gradlew.bat integrationTest
```

すべてのactive profileで、対応するPaper buildをPaper Downloads Serviceから自動取得します。experimental profileを明示実行した場合も同様です。26.1.2および26.2 profileはJava 25を使用します。Java 25がインストールされていない場合は、Foojay toolchain resolverが対応するJDKを自動取得します。

各profileは対応するSkriptDummyAddonのRelease artifactをダウンロードして実サーバーを起動し、スナップショットを生成して全JSONファイルをparseします。その後、Manifestとprofile capability、global alias target、必須registryに加え、addon JAR内のfixture catalogで対象バージョンに適用されるすべてのassertionを検証します。デフォルトのfixture releaseは`1.1.0`で、`-PskriptSyntaxGenerator.dummyAddonVersion=<version>`により変更できます。catalogは各versionにおけるfixtureの意味を記述します。legacy adapterでは、取得不能と文書化されているfield（expression implementation metadataとtype registration ordering）だけを値比較から除外しますが、fixture record自体の存在は必須です。current adapterではそれらのfieldも比較します。
