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
    implementation("com.google.code.gson:gson:2.13.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        args("--port", "25595")
        downloadPlugins {
            //https://github.com/SkriptLang/Skript
            github("SkriptLang", "Skript", "2.14.2", "Skript-2.14.2.jar")

        }
    }

    shadowJar {
        relocate("com.google.gson", "jp.nlaocs.skriptSyntaxGenerator.libs.gson")
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
