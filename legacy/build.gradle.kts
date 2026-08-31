plugins {
    java
    id("com.gradleup.shadow") version "8.3.10"
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":snapshot-contract"))
    implementation("org.ow2.asm:asm:9.7.1")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    testImplementation("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("SkriptSyntaxGenerator-legacy")
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "jp.nlaocs.skriptSyntaxGenerator.legacy.libs.asm")
    relocate("com.fasterxml.jackson", "jp.nlaocs.skriptSyntaxGenerator.legacy.libs.jackson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
