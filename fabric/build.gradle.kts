plugins {
    `java-library`
    id("fabric-loom") version "1.14.10"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val minecraft_version: String = providers.gradleProperty("minecraft_version").get()
val fabric_loader_version: String = providers.gradleProperty("fabric_loader_version").get()
val team_reborn_energy_version: String = providers.gradleProperty("team_reborn_energy_version").get()
val forge_config_api_port_version: String = providers.gradleProperty("forge_config_api_port_version").get()
val includeSharedCode = providers.gradleProperty("fabric.common").orNull != "false"
val night_config_version: String = providers.gradleProperty("night_config_version").get()
val fabric_api_version: String = providers.gradleProperty("fabric_api_version").get()

repositories {
    mavenCentral()
    // Only for the dist-marker annotations (published inside mergetool).
    // Forge Config API Port: ships NeoForge's ModConfigSpec so the shared config code compiles on Fabric.
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
    maven("https://maven.neoforged.net/releases") {
        content { includeModule("net.neoforged", "mergetool") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // Same Mojang names as the NeoForm-based core and neoforge modules.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
    // Energy standard on Fabric (the counterpart of NeoForge's FE capability).
    modImplementation("maven.modrinth:forge-config-api-port:$forge_config_api_port_version")
    include("maven.modrinth:forge-config-api-port:$forge_config_api_port_version")
    // The Modrinth artifact carries no transitive dependencies; Forge Config API Port needs these.
    implementation("com.electronwill.night-config:core:$night_config_version")
    implementation("com.electronwill.night-config:toml:$night_config_version")
    include("com.electronwill.night-config:core:$night_config_version")
    include("com.electronwill.night-config:toml:$night_config_version")
    modImplementation("teamreborn:energy:$team_reborn_energy_version")
    include("teamreborn:energy:$team_reborn_energy_version")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // @OnlyIn/Dist annotations used by shared code; missing annotation classes are ignored at runtime.
    compileOnly("net.neoforged:mergetool:2.0.0:api")
    if (includeSharedCode) {
        compileOnly(fileTree(rootProject.file("libs")) { include("**/*.jar") })
    }
}

// core is built against NeoForm, not Loom, so its jar cannot be remapped for production. Compile
// its sources as part of this module instead, so Loom remaps them together with the Fabric code.
sourceSets.main {
    java.srcDir(project(":core").file("src/main/java"))
    // Compiles the shared mod code (src/main/java of the root project) too, see docs/roadmap/multiloader.md §42.
    // -Pfabric.common=false compiles the Fabric module alone (it then no longer builds, the Fabric glue uses
    // shared classes).
    if (includeSharedCode) {
        // A separate source set so the excludes apply to the shared sources only: some excluded NeoForge
        // classes have a Fabric counterpart with the same path under fabric/src/main/java.
        val shared = objects.sourceDirectorySet("sharedMod", "Shared mod code")
        shared.srcDir(rootProject.file("src/main/java"))
        shared.exclude(
            "li/cil/oc2/client/**",
            "li/cil/oc2/gametest/**",
            "li/cil/oc2/data/**",
            "li/cil/oc2/platform/NeoForge*",
            "li/cil/oc2/common/Main.java",
            // NeoForge implementations of classes that this module provides itself (same name and API).
            // PlatformBlockEntity: NeoForge's patched BlockEntity hooks vs. Fabric glue.
            "li/cil/oc2/common/blockentity/PlatformBlockEntity.java",
            // ManualItem is built on the NeoForge-only Markdown Manual library; Fabric has a plain item.
            "li/cil/oc2/common/item/tool/ManualItem.java",
            // Mixins for the NeoForge mixin config (client rendering of the projector, server chunk
            // cache); the Fabric module has its own mixin config with counterparts.
            "li/cil/oc2/common/mixin/**",
            // JEI/ProjectRed/Create integrations are NeoForge-specific for now.
            "li/cil/oc2/common/integration/jei/**",
            "li/cil/oc2/common/integration/projectred/**")
        java.source(shared)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "5000"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

loom {
    accessWidenerPath = file("src/main/resources/oc2r.accesswidener")
}

// Runs the Fabric GameTests (`./gradlew :fabric:runGametest`): they exercise the Fabric bridges on a
// real dedicated server, since the shared mod code does not build for Fabric yet.
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "oc2r_gametest"
        enableGameTests = true
        enableClientGameTests = false
        eula = true
    }
}
