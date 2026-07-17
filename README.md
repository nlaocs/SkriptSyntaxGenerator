# SkriptSyntaxGenerator

English | [日本語](README_JA.md)

Generates a server-specific Skript syntax snapshot for LSP and tooling use. The snapshot records the active Skript version, server, plugins, registration order, capabilities, and 17 data files behind a stable schema.

For a field-by-field description of every generated file, including nullability, value ranges, concepts, and version differences, see the [snapshot JSON format reference](docs/json-format.md).

## Generator artifacts

Two adapters write the same 18-file snapshot contract:

| Skript | Artifact | Runtime |
| --- | --- | --- |
| 2.6.4-2.13.x | `legacy/build/libs/SkriptSyntaxGenerator-legacy-1.0.jar` | Java 8 compatible, reflective Skript API adapter |
| 2.14.x-2.16.x | `build/libs/SkriptSyntaxGenerator-1.0-all.jar` | Java 21, current registry API adapter |

Place the matching artifact in the server's `plugins` directory, start the server, and run `/skgen`. Files are written to `plugins/SkriptSyntaxGenerator` by default. A server snapshot should be generated again whenever the server, Skript, installed addons, or addon load order changes.

Both adapters always emit the same files. Features unavailable in an older Skript version use the contract's empty root (`[]`, `{}` for `Operations.json`, or `{"aliases":{},"targets":[]}` for `Aliases.json`) and are described by `Manifest.json.capabilities`.

## Manifest capabilities

`Manifest.json` uses schema version 2 and records:

- `syntaxApi`: `legacy-static` or `registry`
- `eventValueApi`: `legacy`, `modern-2.15`, or `modern-2.16`
- `syntaxKinds`: availability of each collected registry
- `aliases.supported` and `aliases.collected`

`Aliases.json` snapshots aliases registered globally by Skript and addons. Per-script aliases declared through an `aliases:` section are stored in script-local child providers and are outside the generator's data model, so user script contents never become part of the snapshot.

Aliases are emitted losslessly with a sorted `aliases` map from the exact recognized text to an index in `targets`. The deduplicated `targets` array stores `amount`, `all`, and the resolved item types with their Material, Minecraft ID, durability, plain/alias flags, block values, and item meta when present. This keeps the very large legacy alias tables compact while preserving the complete mapping.

## Generated data by Skript version

`Yes` means that the integration profile produced and validated at least one record. `No` means that the file is still emitted with the stable empty root because that registry or concept is unavailable in that Skript version.

Core syntax data:

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

Supporting registries and relationships:

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

`Arithmetic` covers `Operators.json`, `Operations.json`, and `Differences.json` as one capability. Event values are available for every tested version, but their metadata shape changes: 2.6.4-2.14.3 use `eventValueApi: legacy`, while 2.15.4 and 2.16.0 expose the `modern-2.16` shape. The exact detected shape must be read from the Manifest instead of inferred only from the Skript version.

Skript 2.6.4 has no enumerable Structure registry. Its command, function, options, variables, aliases, and event top-level constructs are handled by dedicated `ScriptLoader` branches. They are not synthesized into `Structures.json`, because this generator preserves registered raw data instead of reconstructing syntax declarations.

## Compatibility matrix

The integration suite currently covers these boundaries:

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
| 2.15.4 | 26.1.2 | 25 | current API |
| 2.16.0 | 26.2 | 25 | current API |

The `2.6.4 + Minecraft 1.12.2 + Java 8` profile is a required compatibility test, not an inferred target.

Minecraft 1.16.5, 1.17.1, 1.18.2, 26.1.2, and 26.2 are representative patch releases selected from [Skript's bStats Minecraft Version chart](https://bstats.org/plugin/bukkit/Skript/722). The chart changes over time; these profiles cover the currently significant version families without replacing the Skript API boundary profiles. The interactive `runServer` task uses Skript 2.15.4 on Paper 1.21.11.

Print the matrix:

```powershell
.\gradlew.bat integrationMatrix
```

Run unit tests:

```powershell
.\gradlew.bat test
```

Some archived Paper versions require executable server jars supplied by Gradle properties or the equivalent environment variables:

```powershell
.\gradlew.bat `
  '-PskriptSyntaxGenerator.paper1122Jar=C:\path\to\paper-1.12.2.jar' `
  '-PskriptSyntaxGenerator.paper1202Jar=C:\path\to\paper-1.20.2.jar' `
  '-PskriptSyntaxGenerator.paper121Jar=C:\path\to\paper-1.21.jar' `
  integrationTest
```

The environment variable alternatives are `PAPER_1122_JAR`, `PAPER_1202_JAR`, and `PAPER_121_JAR`. Paper 1.16.5, 1.17.1, and 1.18.2 are downloaded automatically through Paper's current Downloads Service. The 26.1.2 and 26.2 profiles use Java 25; the Foojay toolchain resolver downloads a matching JDK automatically when none is installed.

Each profile downloads the matching SkriptDummyAddon release artifact, starts a real server, generates the snapshot, and parses every JSON file. Validation verifies the manifest and profile capabilities, global alias targets, required non-empty registries, and every applicable assertion from the fixture catalog embedded in the addon JAR. The default fixture release is `1.0.0`; it can be changed with `-PskriptSyntaxGenerator.dummyAddonVersion=<version>`. The catalog describes fixture semantics across versions. For the legacy adapter, fields documented as unavailable (expression implementation metadata and type registration ordering) are excluded from value comparison while the fixture records themselves remain required; the current adapter validates those fields as well.
