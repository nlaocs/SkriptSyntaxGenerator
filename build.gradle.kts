import org.gradle.api.tasks.compile.JavaCompile
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "2.3.20-RC"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "jp.nlaocs"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://repo.skriptlang.org/releases/") {
        name = "skriptlang-repo"
    }
}

dependencies {
    implementation(project(":snapshot-contract"))
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.14.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("net.bytebuddy:byte-buddy:1.14.10")
    implementation("net.bytebuddy:byte-buddy-agent:1.14.10")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.github.SkriptLang:Skript:2.14.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        jvmArgs("-XX:+EnableDynamicAgentLoading")
        // jvmArgs("-DskriptSyntaxGenerator.hookLog=true")
        args("--port", "25595")
        downloadPlugins {
            //https://github.com/SkriptLang/Skript
            github("SkriptLang", "Skript", "2.15.4", "Skript-2.15.4.jar")
            // github("SkQuery", "SkQuery", "4.3.2", "SkQuery-4.3.2.jar")
            github("sovdeeth", "skript-particle", "v1.4.1", "skript-particle-1.4.1.jar")
            modrinth("skbee", "3.25.2")
            modrinth("lusk", "1.3.14")
            github("cooffeeRequired", "skJson", "5.6.0", "skjson.jar")

            // 特殊な作り方をしているaddon群
            github("SkriptLang", "skript-reflect", "v2.6.3", "skript-reflect-2.6.3.jar")
            github("Pesekjak", "Hippo", "1.3.1", "Hippo.jar")

            github("nlaocs", "SkriptDummyAddon", "1.0.1", "SkriptDummyAddon-1.0.1-skript-2.15.4.jar")

        }
    }

    shadowJar {
        relocate("com.fasterxml.jackson", "jp.nlaocs.skriptSyntaxGenerator.libs.jackson")
    }
}

//val targetJavaVersion = 8
val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

enum class GeneratorAdapter { MODERN, REFLECTIVE }

data class ServerJarRequirement(
    val gradleProperty: String,
    val environmentVariable: String
)

val paper1122 = ServerJarRequirement("skriptSyntaxGenerator.paper1122Jar", "PAPER_1122_JAR")
val paper1202 = ServerJarRequirement("skriptSyntaxGenerator.paper1202Jar", "PAPER_1202_JAR")
val paper121 = ServerJarRequirement("skriptSyntaxGenerator.paper121Jar", "PAPER_121_JAR")
val dummyAddonVersion = providers.gradleProperty("skriptSyntaxGenerator.dummyAddonVersion")
    .orElse("1.0.1")
    .get()

data class IntegrationProfile(
    val id: String,
    val taskSuffix: String,
    val minecraft: String,
    val skript: String,
    val java: Int,
    val adapter: GeneratorAdapter,
    val syntaxApi: String,
    val eventValueShape: String,
    val port: Int? = null,
    val serverJar: ServerJarRequirement? = null,
    val skriptAssetOverride: String? = null,
    val status: String = "active",
    val blocker: String? = null
) {
    val skriptAsset: String
        get() = skriptAssetOverride ?: "Skript-$skript.jar"

    val requiredNonEmptyFiles: Set<String>
        get() {
            val files = mutableSetOf(
                "Aliases.json",
                "Conditions.json",
                "Effects.json",
                "Events.json",
                "Expressions.json",
                "Sections.json",
                "Types.json",
                "Functions.json",
                "Converters.json",
                "Comparators.json",
                "EventValues.json"
            )
            val minor = skript.split(".").getOrNull(1)?.toIntOrNull() ?: return files
            if (minor >= 7) files += "Structures.json"
            if (minor >= 8) {
                files += "Operators.json"
                files += "Operations.json"
                files += "Differences.json"
            }
            if (minor >= 13) files += "Properties.json"
            return files
        }
}

val integrationProfiles = listOf(
    IntegrationProfile(
        "skript-2.6.4", "Legacy264", "1.12.2", "2.6.4", 8,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25600,
        paper1122
    ),
    IntegrationProfile(
        "skript-2.7.3", "Legacy273", "1.20.2", "2.7.3", 17,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25601, paper1202,
        skriptAssetOverride = "Skript.jar"
    ),
    IntegrationProfile(
        "skript-2.8.7", "Legacy287", "1.20.2", "2.8.7", 17,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25602, paper1202
    ),
    IntegrationProfile(
        "skript-2.9.5", "Legacy295", "1.21", "2.9.5", 21,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25603, paper121
    ),
    IntegrationProfile(
        "skript-2.10.2", "Legacy2102", "1.21", "2.10.2", 21,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25604, paper121
    ),
    IntegrationProfile(
        "skript-2.11.2", "Legacy2112", "1.21", "2.11.2", 21,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25605, paper121
    ),
    IntegrationProfile(
        "skript-2.12.2", "Legacy2122", "1.21", "2.12.2", 21,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25606, paper121
    ),
    IntegrationProfile(
        "skript-2.13.2", "Legacy2132", "1.21", "2.13.2", 21,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25607, paper121
    ),
    IntegrationProfile(
        "skript-2.14.3", "Modern2143", "1.21.11", "2.14.3", 21,
        GeneratorAdapter.MODERN, "registry", "legacy", 25608
    ),
    IntegrationProfile(
        "skript-2.15.4", "Modern2154", "1.21.11", "2.15.4", 21,
        GeneratorAdapter.MODERN, "registry", "modern-2.16", 25609
    ),
    IntegrationProfile(
        "skript-2.16.0", "Modern2160", "1.21.11", "2.16.0", 21,
        GeneratorAdapter.MODERN, "registry", "modern-2.16", 25610
    ),
    IntegrationProfile(
        "minecraft-1.16.5", "Minecraft1165", "1.16.5", "2.6.4", 16,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25611
    ),
    IntegrationProfile(
        "minecraft-1.17.1", "Minecraft1171", "1.17.1", "2.6.4", 17,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25612
    ),
    IntegrationProfile(
        "minecraft-1.18.2", "Minecraft1182", "1.18.2", "2.6.4", 17,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static", 25613
    ),
    IntegrationProfile(
        "minecraft-26.1.2", "Minecraft2612", "26.1.2", "2.15.4", 25,
        GeneratorAdapter.MODERN, "registry", "modern-2.16", 25614
    ),
    IntegrationProfile(
        "minecraft-26.2", "Minecraft262", "26.2", "2.16.0", 25,
        GeneratorAdapter.MODERN, "registry", "modern-2.16", 25615
    ),
    IntegrationProfile(
        "legacy-1.8.8", "Legacy188", "1.8.8", "final-for-1.8", 8,
        GeneratorAdapter.REFLECTIVE, "legacy-static", "legacy-static",
        status = "planned",
        blocker = "Requires a Java 8 artifact, a legacy Skript adapter, and a Spigot-compatible runner."
    )
)

val activeIntegrationValidations = integrationProfiles
    .filter { it.status == "active" }
    .map { profile ->
        val output = layout.buildDirectory.dir("integration/${profile.id}/snapshot")
        val server = layout.buildDirectory.dir("integration/${profile.id}/server")
        val fixtureCatalogJar = gradle.gradleUserHomeDir.resolve(
            "caches/run-task-jars/plugins/paper/github/nlaocs/SkriptDummyAddon/" +
                    "$dummyAddonVersion/SkriptDummyAddon-$dummyAddonVersion-skript-${profile.skript}.jar"
        )
        val configuredServerJar = profile.serverJar?.let { requirement ->
            layout.file(
                providers.gradleProperty(requirement.gradleProperty)
                    .orElse(providers.environmentVariable(requirement.environmentVariable))
                    .map { project.file(it) }
            )
        }
        val runTask = tasks.register<RunServer>("runIntegration${profile.taskSuffix}") {
            group = "verification"
            description =
                "Runs Paper ${profile.minecraft} with Skript ${profile.skript} and generates a syntax snapshot."
            if (profile.adapter == GeneratorAdapter.REFLECTIVE) {
                dependsOn(":legacy:shadowJar")
            } else {
                dependsOn(tasks.shadowJar)
            }

            minecraftVersion(profile.minecraft)
            runDirectory.set(server)
            if (profile.adapter == GeneratorAdapter.REFLECTIVE) {
                pluginJars(
                    project(":legacy").layout.buildDirectory.file(
                        "libs/SkriptSyntaxGenerator-legacy-${project.version}.jar"
                    )
                )
                legacyPluginLoading()
            } else {
                pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
            }
            if (configuredServerJar?.isPresent == true) {
                serverJar(configuredServerJar)
            }
            javaLauncher = project.javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(profile.java))
            }
            minHeapSize = "256M"
            maxHeapSize = "1G"
            if (profile.java >= 21) {
                jvmArgs("-XX:+EnableDynamicAgentLoading")
            }
            systemProperty("skriptSyntaxGenerator.integration", "true")
            systemProperty(
                "skriptSyntaxGenerator.outputDirectory",
                output.get().asFile.absolutePath
            )
            args("--port", requireNotNull(profile.port).toString())
            downloadPlugins {
                github(
                    "SkriptLang",
                    "Skript",
                    profile.skript,
                    profile.skriptAsset
                )
                github(
                    "nlaocs",
                    "SkriptDummyAddon",
                    dummyAddonVersion,
                    "SkriptDummyAddon-$dummyAddonVersion-skript-${profile.skript}.jar"
                )
            }

            doFirst {
                profile.serverJar?.let { requirement ->
                    val requiredServerJar = requireNotNull(configuredServerJar)
                    check(requiredServerJar.isPresent) {
                        "Set -P${requirement.gradleProperty}=<path> or ${requirement.environmentVariable} " +
                                "to an executable Paper ${profile.minecraft} server jar."
                    }
                    check(requiredServerJar.get().asFile.isFile) {
                        "Paper ${profile.minecraft} server jar does not exist: " +
                                requiredServerJar.get().asFile.absolutePath
                    }
                }
                project.delete(output)
                project.delete(server)
                server.get().asFile.mkdirs()
                server.get().file("eula.txt").asFile.writeText("eula=true\n")
            }
        }

        tasks.register<JavaExec>("validateIntegration${profile.taskSuffix}") {
            group = "verification"
            description = "Validates the generated Skript ${profile.skript} integration snapshot."
            dependsOn(runTask, tasks.testClasses)
            classpath = sourceSets.test.get().runtimeClasspath
            javaLauncher.set(project.javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            })
            mainClass.set("jp.nlaocs.skriptSyntaxGenerator.integration.SnapshotValidatorMain")
            args(
                output.get().asFile.absolutePath,
                profile.eventValueShape,
                profile.syntaxApi,
                profile.minecraft,
                profile.skript,
                profile.requiredNonEmptyFiles.sorted().joinToString(","),
                fixtureCatalogJar.absolutePath,
                dummyAddonVersion
            )
        }
    }

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs unit tests and all active server integration profiles."
    dependsOn(tasks.test, activeIntegrationValidations)
}

tasks.register("integrationMatrix") {
    group = "verification"
    description = "Prints active and planned Skript compatibility profiles."
    doLast {
        integrationProfiles.forEach { profile ->
            val blocker = profile.blocker?.let { " - $it" }.orEmpty()
            println(
                "${profile.status.padEnd(7)} ${profile.id.padEnd(22)} " +
                        "MC=${profile.minecraft.padEnd(13)} Skript=${profile.skript.padEnd(13)} Java=${profile.java}$blocker"
            )
        }
    }
}
