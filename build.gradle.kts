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

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
