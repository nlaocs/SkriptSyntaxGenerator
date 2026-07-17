# SkriptSyntaxGenerator

[English](README.md) | 日本語

LSPや各種ツールで利用するために、サーバー固有のSkript構文スナップショットを生成します。スナップショットには、安定したスキーマのもとで、使用中のSkriptバージョン、サーバー、プラグイン、登録順序、capability、および17個のデータファイルが記録されます。

生成される全ファイルについて、各フィールド、null・省略、値域、概念、バージョン差を確認するには、[スナップショットJSON形式リファレンス](docs/json-format.ja.md)を参照してください。

## Generatorの成果物

2種類のadapterが、同じ18ファイルのスナップショット契約に従って出力します。

| Skript | 成果物 | 実行環境 |
| --- | --- | --- |
| 2.6.4-2.13.x | `legacy/build/libs/SkriptSyntaxGenerator-legacy-1.0.jar` | Java 8互換、reflectionを使用するSkript API adapter |
| 2.14.x-2.16.x | `build/libs/SkriptSyntaxGenerator-1.0-all.jar` | Java 21、現在のregistry API adapter |

対応する成果物をサーバーの`plugins`ディレクトリに配置し、サーバーを起動して`/skgen`を実行してください。デフォルトでは、ファイルは`plugins/SkriptSyntaxGenerator`に出力されます。サーバー、Skript、導入addon、またはaddonの読み込み順序が変わった場合は、サーバースナップショットを再生成してください。

どちらのadapterも常に同じファイルを出力します。古いSkriptバージョンで利用できない機能は、契約で定めた空のroot（`[]`、`Operations.json`では`{}`、`Aliases.json`では`{"aliases":{},"targets":[]}`）として出力され、利用可否は`Manifest.json.capabilities`に記録されます。

## Manifest capabilities

`Manifest.json`はschema version 2を使用し、次の情報を記録します。

- `syntaxApi`: `legacy-static`または`registry`
- `eventValueApi`: `legacy`、`modern-2.15`、または`modern-2.16`
- `syntaxKinds`: 各registryを取得できるかどうか
- `aliases.supported`および`aliases.collected`

`Aliases.json`には、Skriptとaddonによってグローバルに登録されたaliasが保存されます。各スクリプトの`aliases:` sectionで宣言されたaliasはscript-localなchild providerに保存されるため、Generatorのデータモデルには含まれません。したがって、ユーザーが記述したスクリプトの内容がスナップショットに混入することはありません。

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

| Skript | Arithmetic | Converters | Comparators | Event values | Properties | Class hierarchy | Global aliases |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 2.6.4 | No | Yes | Yes | Yes | No | Yes | Yes |
| 2.7.3 | No | Yes | Yes | Yes | No | Yes | Yes |
| 2.8.7 | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.9.5 | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.10.2 | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.11.2 | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.12.2 | Yes | Yes | Yes | Yes | No | Yes | Yes |
| 2.13.2 | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.14.3 | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.15.4 | Yes | Yes | Yes | Yes | Yes | Yes | Yes |
| 2.16.0 | Yes | Yes | Yes | Yes | Yes | Yes | Yes |

`Arithmetic`は、`Operators.json`、`Operations.json`、`Differences.json`を1つのcapabilityとして表します。Event valueはテスト済みの全バージョンで取得できますが、metadataの形状が異なります。2.6.4-2.14.3では`eventValueApi: legacy`、2.15.4と2.16.0では`modern-2.16`です。Skriptバージョンだけから推測せず、Manifestから実際に検出された形状を参照してください。

Skript 2.6.4には列挙可能なStructure registryがありません。command、function、options、variables、aliases、eventのトップレベル構造は、`ScriptLoader`内の専用分岐で処理されます。このGeneratorは構文宣言を再構築するのではなく、登録された生データを保存するため、これらを`Structures.json`へ擬似的に追加しません。

## 互換性マトリクス

integration suiteでは、現在次の境界をテストしています。

| Skript | Minecraft | Java | Adapter |
| --- | --- | --- | --- |
| 2.6.4 | 1.12.2 | 8 | reflective |
| 2.7.3 | 1.20.2 | 17 | reflective |
| 2.8.7 | 1.20.2 | 17 | reflective |
| 2.9.5-2.13.2 | 1.21 | 21 | reflective |
| 2.14.3-2.16.0 | 1.21.11 | 21 | current API |

`2.6.4 + Minecraft 1.12.2 + Java 8`のprofileは、推測上の対応範囲ではなく、必須の互換性テストです。

マトリクスを表示するには、次を実行します。

```powershell
.\gradlew.bat integrationMatrix
```

単体テストを実行するには、次を実行します。

```powershell
.\gradlew.bat test
```

アーカイブされたPaperバージョンを使用するには、実行可能なserver jarをGradle property、または対応する環境変数で指定する必要があります。

```powershell
.\gradlew.bat '-PskriptSyntaxGenerator.paper1122Jar=C:\path\to\paper-1.12.2.jar' '-PskriptSyntaxGenerator.paper1202Jar=C:\path\to\paper-1.20.2.jar' '-PskriptSyntaxGenerator.paper121Jar=C:\path\to\paper-1.21.jar' integrationTest
```

対応する環境変数は、`PAPER_1122_JAR`、`PAPER_1202_JAR`、`PAPER_121_JAR`です。

各profileは実際のサーバーを起動し、スナップショットを生成して全JSONファイルをparseします。その後、Manifestとprofile capability、global alias targetを検証し、必須registryが予期せず空になっている場合はテストを失敗させます。
