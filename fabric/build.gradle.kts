plugins {
    `java-library`
    id("fabric-loom") version "1.14.10"
}

java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

val minecraft_version: String = providers.gradleProperty("minecraft_version").get()
val fabric_loader_version: String = providers.gradleProperty("fabric_loader_version").get()
val team_reborn_energy_version: String = providers.gradleProperty("team_reborn_energy_version").get()
val forge_config_api_port_version: String = providers.gradleProperty("forge_config_api_port_version").get()
val includeSharedCode = providers.gradleProperty("fabric.common").orNull != "false"
val night_config_version: String = providers.gradleProperty("night_config_version").get()
val ceres_version: String = providers.gradleProperty("ceres_version").get()
val sedna_version: String = providers.gradleProperty("sedna_version").get()
val sedna_buildroot_version: String = providers.gradleProperty("sedna_buildroot_version").get()
val markdown_manual_fabric_version: String = providers.gradleProperty("markdown_manual_fabric_version").get()
val architectury_fabric_version: String = providers.gradleProperty("architectury_fabric_version").get()
val fabric_api_version: String = providers.gradleProperty("fabric_api_version").get()

repositories {
    mavenCentral()
    // ceres/sedna/sedna-buildroot are fetched into libs/ by scripts/download-libs.sh.
    maven(rootProject.file("libs")) {
        content { includeGroup("li.cil.ceres"); includeGroup("li.cil.sedna") }
    }
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
    // The in-game manual: the original Markdown Manual has Fabric builds (it needs Architectury API).
    modImplementation("maven.modrinth:markdownmanual:$markdown_manual_fabric_version")
    modImplementation("maven.modrinth:architectury-api:$architectury_fabric_version")
    modImplementation("teamreborn:energy:$team_reborn_energy_version")
    include("teamreborn:energy:$team_reborn_energy_version")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // @OnlyIn/Dist annotations used by shared code; missing annotation classes are ignored at runtime.
    compileOnly("net.neoforged:mergetool:2.0.0:api")
    // Emulator and serialization libraries, shipped inside the mod jar like on NeoForge.
    implementation("li.cil.ceres:ceres:$ceres_version")
    implementation("li.cil.sedna:sedna:$sedna_version")
    implementation("li.cil.sedna:sedna-buildroot:$sedna_buildroot_version")
    include("li.cil.ceres:ceres:$ceres_version")
    include("li.cil.sedna:sedna:$sedna_version")
    include("li.cil.sedna:sedna-buildroot:$sedna_buildroot_version")
}

// core is built against NeoForm, not Loom, so its jar cannot be remapped for production. Compile
// its sources as part of this module instead, so Loom remaps them together with the Fabric code.
sourceSets.main {
    java.srcDir(rootProject.file("core/src/main/java"))
    // Compiles the shared mod code (src/main/java of the root project) too, see docs/roadmap/multiloader.md §42.
    // -Pfabric.common=false compiles the Fabric module alone (it then no longer builds, the Fabric glue uses
    // shared classes).
    if (includeSharedCode) {
        // A separate source set so the excludes apply to the shared sources only: some excluded NeoForge
        // classes have a Fabric counterpart with the same path under fabric/src/main/java.
        val shared = objects.sourceDirectorySet("sharedMod", "Shared mod code")
        shared.srcDir(rootProject.file("src/main/java"))
        shared.exclude(
            "li/cil/oc2/gametest/**",
            // The (unused) NeoForge menu screen registry.
            "li/cil/oc2/client/gui/ScreenRegistry.java",
            // NeoForge geometry loaders and IDynamicBakedModel/ModelData based models; Fabric has its own
            // (fabric/src/main/java/li/cil/oc2/client/model).
            "li/cil/oc2/client/model/BusCableModelLoader.java",
            "li/cil/oc2/client/model/BusCableModel.java",
            "li/cil/oc2/client/model/BusCableBakedModel.java",
            "li/cil/oc2/client/model/BusCableModelTypes.java",
            "li/cil/oc2/client/model/monitor/MonitorModelLoader.java",
            "li/cil/oc2/client/model/monitor/MonitorModel.java",
            "li/cil/oc2/client/model/monitor/MonitorBakedModel.java",
            "li/cil/oc2/client/model/monitor/MonitorModelData.java",
            "li/cil/oc2/client/hooks/BusCableModelHooks.java",
            "li/cil/oc2/client/hooks/MonitorModelHooks.java",
            // Same name and API as the Fabric counterparts (see PlatformBlockEntity).
            "li/cil/oc2/client/ClientCompat.java",
            "li/cil/oc2/data/**",
            "li/cil/oc2/platform/NeoForge*",
            "li/cil/oc2/common/Main.java",
            // NeoForge implementations of classes that this module provides itself (same name and API).
            // PlatformBlockEntity: NeoForge's patched BlockEntity hooks vs. Fabric glue.
            "li/cil/oc2/common/blockentity/PlatformBlockEntity.java",
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

val packageScripts = tasks.register<Zip>("packageScripts") {
    archiveFileName = "scripts.zip"
    destinationDirectory = layout.buildDirectory.dir("generated/scripts")
    from(rootProject.file("src/main/scripts"))
    from(rootProject.file("src/main/resources/onyxos")) {
        include("fw_jump.bin")
        into("firmware_files")
    }
}

// Assets, data packs, natives and the OnyxOS images live in the root project's resources. Only the
// loader-independent parts are shared: META-INF (NeoForge service files, mods.toml) must not leak in.
// src/generated is a gitignored datagen workdir whose content is synced into src/main/resources by
// copyGeneratedResources (CI never has it): including it here only ever produced duplicates.
tasks.processResources {
    from(rootProject.file("src/main/resources")) {
        include("assets/**", "data/**", "natives/**", "onyxos/**", "pack.mcmeta")
    }
    // The guest scripts archive (same content as the root project's packageScripts task).
    from(packageScripts) {
        into("data/oc2r/file_systems")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

loom {
    accessWidenerPath = rootProject.file("fabric/src/main/resources/oc2r.accesswidener")
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

// The NeoForge game tests (src/main/java/.../gametest) also run on Fabric. They use two NeoForge-only
// annotations and a NeoForge-patched @GameTest attribute, so the sources are copied with those
// lines rewritten into vanilla form before the gametest source set compiles them.
val sharedGameTestsDir = layout.buildDirectory.dir("generated/sharedGameTests")
val sharedGameTestSources = tasks.register<Sync>("sharedGameTestSources") {
    from(rootProject.file("src/main/java/li/cil/oc2/gametest")) {
        exclude("GameTestResultReporter.java")
        filter { line: String ->
            when {
                line.contains("GameTestHolder") || line.contains("PrefixGameTestTemplate") -> ""
                else -> line
                    .replace(
                        "template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE",
                        "template = TestSupport.TEMPLATE_NAMESPACE + \":\" + TestSupport.TEMPLATE")
                    .replace(Regex("private (\\w+Tests)\\(\\)"), "public \$1()")
            }
        }
    }
    into(sharedGameTestsDir.map { it.dir("li/cil/oc2/gametest") })
}

sourceSets.named("gametest") {
    java.srcDir(sharedGameTestsDir)
}

tasks.named("compileGametestJava") {
    dependsOn(sharedGameTestSources)
}
