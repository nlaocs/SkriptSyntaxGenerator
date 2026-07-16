import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "2.3.20-RC"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
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
            github("SkriptLang", "Skript", "2.14.3", "Skript-2.14.3.jar")
            github("SkQuery", "SkQuery", "4.3.2", "SkQuery-4.3.2.jar")
            github("sovdeeth", "skript-particle", "v1.4.1", "skript-particle-1.4.1.jar")
            modrinth("skbee", "3.17.1")
            modrinth("lusk", "1.3.13")
            github("cooffeeRequired", "skJson", "5.4.1", "skjson.jar")

            // 特殊な作り方をしているaddon群
            github("SkriptLang", "skript-reflect", "v2.6.3", "skript-reflect-2.6.3.jar")
            github("Pesekjak", "Hippo", "1.3.1", "Hippo.jar")

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

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

data class IntegrationProfile(
    val id: String,
    val taskSuffix: String,
    val minecraft: String,
    val skript: String,
    val skriptAsset: String?,
    val java: Int,
    val status: String,
    val eventValueMetadata: String,
    val port: Int? = null,
    val blocker: String? = null
)

val integrationProfiles = listOf(
    IntegrationProfile(
        "modern-2.14.3",
        "Modern2143",
        "1.21.11",
        "2.14.3",
        "Skript-2.14.3.jar",
        21,
        "active",
        "legacy",
        25596
    ),
    IntegrationProfile(
        "modern-2.15.2",
        "Modern2152",
        "1.21.11",
        "2.15.2",
        "Skript-2.15.2.jar",
        21,
        "active",
        "modern-2.15",
        25597
    ),
    IntegrationProfile(
        "modern-2.16.0",
        "Modern2160",
        "1.21.11",
        "2.16.0",
        "Skript-2.16.0.jar",
        21,
        "active",
        "modern-2.16",
        25598
    ),
    IntegrationProfile(
        "legacy-2.6.4",
        "Legacy264",
        "1.12.2",
        "2.6.4",
        null,
        8,
        "planned",
        "legacy",
        blocker = "Requires a Java 8 artifact and adapters for pre-SyntaxRegistry registration APIs."
    ),
    IntegrationProfile(
        "legacy-1.8.8",
        "Legacy188",
        "1.8.8",
        "final-for-1.8",
        null,
        8,
        "planned",
        "legacy",
        blocker = "Requires a Java 8 artifact, a legacy Skript adapter, and a Spigot-compatible runner."
    )
)

val activeIntegrationValidations = integrationProfiles
    .filter { it.status == "active" }
    .map { profile ->
        val output = layout.buildDirectory.dir("integration/${profile.id}/snapshot")
        val server = layout.buildDirectory.dir("integration/${profile.id}/server")
        val runTask = tasks.register<RunServer>("runIntegration${profile.taskSuffix}") {
            group = "verification"
            description =
                "Runs Paper ${profile.minecraft} with Skript ${profile.skript} and generates a syntax snapshot."
            dependsOn(tasks.shadowJar)

            minecraftVersion(profile.minecraft)
            runDirectory.set(server)
            pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
            javaLauncher = project.javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(profile.java))
            }
            jvmArgs("-XX:+EnableDynamicAgentLoading")
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
                    requireNotNull(profile.skriptAsset)
                )
            }

            doFirst {
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
                languageVersion.set(JavaLanguageVersion.of(profile.java))
            })
            mainClass.set("jp.nlaocs.skriptSyntaxGenerator.integration.SnapshotValidatorMain")
            args(output.get().asFile.absolutePath, profile.eventValueMetadata)
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
                "${profile.status.padEnd(7)} ${profile.id.padEnd(20)} " +
                    "MC=${profile.minecraft.padEnd(13)} Skript=${profile.skript.padEnd(13)} Java=${profile.java}$blocker"
            )
        }
    }
}
