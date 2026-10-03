plugins {
    `java-library`
    id("fabric-loom") version "1.14.10"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val minecraft_version: String = providers.gradleProperty("minecraft_version").get()
val fabric_loader_version: String = providers.gradleProperty("fabric_loader_version").get()
val fabric_api_version: String = providers.gradleProperty("fabric_api_version").get()

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // Same Mojang names as the NeoForm-based core and neoforge modules.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
}

// core is built against NeoForm, not Loom, so its jar cannot be remapped for production. Compile
// its sources as part of this module instead, so Loom remaps them together with the Fabric code.
sourceSets.main {
    java.srcDir(project(":core").file("src/main/java"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
    options.release = 21
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
