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
    val minecraft: String,
    val skript: String,
    val java: Int,
    val status: String,
    val blocker: String? = null
)

val integrationProfiles = listOf(
    IntegrationProfile("modern-2.14.3", "1.21.11", "2.14.3", 21, "active"),
    IntegrationProfile(
        "modern-2.15.x",
        "current Paper",
        "2.15.x",
        21,
        "planned",
        "Requires an adapter for Skript's EventValue registry API introduced after 2.14."
    ),
    IntegrationProfile(
        "legacy-2.6.4",
        "1.12.2",
        "2.6.4",
        8,
        "planned",
        "Requires a Java 8 artifact and adapters for pre-SyntaxRegistry registration APIs."
    ),
    IntegrationProfile(
        "legacy-1.8.8",
        "1.8.8",
        "final-for-1.8",
        8,
        "planned",
        "Requires a Java 8 artifact, a legacy Skript adapter, and a Spigot-compatible runner."
    )
)

val modernIntegrationOutput = layout.buildDirectory.dir("integration/modern-2.14.3/snapshot")
val modernIntegrationServer = layout.buildDirectory.dir("integration/modern-2.14.3/server")

val runIntegrationModern2143 by tasks.registering(RunServer::class) {
    group = "verification"
    description = "Runs Paper 1.21.11 with Skript 2.14.3 and generates a syntax snapshot."
    dependsOn(tasks.shadowJar)

    minecraftVersion("1.21.11")
    runDirectory.set(modernIntegrationServer)
    pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
    javaLauncher = project.javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    systemProperty("skriptSyntaxGenerator.integration", "true")
    systemProperty(
        "skriptSyntaxGenerator.outputDirectory",
        modernIntegrationOutput.get().asFile.absolutePath
    )
    args("--port", "25596")
    downloadPlugins {
        github("SkriptLang", "Skript", "2.14.3", "Skript-2.14.3.jar")
    }

    doFirst {
        delete(modernIntegrationOutput)
        delete(modernIntegrationServer)
        modernIntegrationServer.get().asFile.mkdirs()
        modernIntegrationServer.get().file("eula.txt").asFile.writeText("eula=true\n")
    }
}

val validateIntegrationModern2143 by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Validates the generated Skript 2.14.3 integration snapshot."
    dependsOn(runIntegrationModern2143, tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    javaLauncher.set(project.javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    mainClass.set("jp.nlaocs.skriptSyntaxGenerator.integration.SnapshotValidatorMain")
    args(modernIntegrationOutput.get().asFile.absolutePath)
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs unit tests and all active server integration profiles."
    dependsOn(tasks.test, validateIntegrationModern2143)
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
