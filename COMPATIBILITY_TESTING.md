# Compatibility testing

The long-term target is to validate generated snapshots from Minecraft 1.8.8 through the newest
Minecraft version supported by Skript. A passing snapshot validator is shared by every profile;
server startup and Skript data access are version-specific.

## Commands

- `./gradlew test` runs fast unit tests without starting a server.
- `./gradlew integrationTest` runs all active server profiles and validates their snapshots.
- `./gradlew integrationMatrix` prints active and planned profiles with their blockers.

The integration server and generated snapshot are isolated under `build/integration/<profile>`.
They do not modify the normal `run` directory.

## Compatibility profiles

| Profile | Minecraft | Skript | Java | State |
| --- | --- | --- | --- | --- |
| `modern-2.14.3` | 1.21.11 | 2.14.3 | 21 | Active |
| `modern-2.15.2` | 1.21.11 | 2.15.2 | 21 | Active |
| `modern-2.16.0` | 1.21.11 | 2.16.0 | 21 | Active |
| `legacy-2.6.4` | 1.12.2 | 2.6.4 | 8 | Planned: Java 8 artifact and legacy registration adapter |
| `legacy-1.8.8` | 1.8.8 | `final-for-1.8` | 8 | Planned: Java 8 artifact, legacy adapter, and Spigot runner |

These profiles are API-family boundaries, not a claim that one successful version proves every
intermediate version. Once adapters exist, each family should test its oldest and newest supported
Minecraft/Skript pair, plus versions where a registry contract changed.

## Shared validation contract

Every profile must produce the same schema-level guarantees:

- all expected JSON files exist and parse;
- `contentDigest` and `snapshotId` can be reproduced;
- plugin and syntax registration orders are contiguous;
- registration IDs are present and unique;
- addon/provider metadata is resolved;
- type, property, and event-value references point to emitted records;
- the class hierarchy is sorted and contains no duplicate names.

## Version boundaries

The current plugin is intentionally a modern artifact: it targets Java 21, declares Bukkit API
1.21, and directly uses modern Skript registry classes. It cannot be made compatible with Java 8
servers by reflection alone because the JVM must first be able to load the plugin classes.

Supporting older families therefore requires a multi-artifact design:

1. Keep snapshot DTOs, stable IDs, digesting, and validation version-neutral.
2. Move Skript registry access behind a small adapter contract.
3. Keep the typed 2.14 EventValue adapter and reflective 2.15+ adapter behind one DTO contract.
4. Build a Java 8 legacy artifact for Skript 2.6.4 and pre-registry APIs.
5. Add a dedicated 1.8.8 adapter and Spigot-compatible server runner for the archived 1.8 fork.

Planned profiles remain visible but do not silently pass or fail the normal build. They become
active only after their artifact and adapter can execute the shared validator end to end.
