import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension

plugins {
    `java-library`
    id("fabric-loom") version "1.18.2" apply false
    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.5.10"
}

val minecraft_version: String = property("minecraft_version").toString()
val modernFabric = !minecraft_version.startsWith("1.")
val clientAcceptance = modernFabric && providers.gradleProperty("fabric.clientTests").orNull == "true"
val javaVersion = property("java_version").toString().toInt()
// Both plugin implementations ship in the same Loom artifact. Legacy Minecraft needs
// intermediary remapping; 26.x ships official names and uses the non-remapping plugin.
apply(plugin = if (modernFabric) "net.fabricmc.fabric-loom" else "fabric-loom")
val loomExtension = extensions.getByType<LoomGradleExtensionAPI>()
java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
java.withSourcesJar()

// fabric.mod.json expands ${version}; without an explicit version the module keeps Gradle's
// default "unspecified", which is what the published jar would then advertise as the mod version.
// This repeats the version computation of the root build script, because the Fabric node is
// configured in its own build of the Stonecutter tree, where rootProject.version is still the
// Gradle default when this script is evaluated.
fun fabricGitRef(): String = try {
    ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim()
} catch (ignored: Throwable) {
    "unknown"
}

val fabricSemver: String = providers.gradleProperty("semver").get()
version = if (System.getenv("RELEASE_TYPE") == "release") {
    fabricSemver.trimStart('v')
} else {
    "${fabricSemver.trimStart('v')}+${fabricGitRef()}"
}

val fabric_loader_version: String = property("fabric_loader_version").toString()
val team_reborn_energy_version: String = property("team_reborn_energy_version").toString()
val forge_config_api_port_version: String = property("forge_config_api_port_version").toString()
val includeSharedCode = providers.gradleProperty("fabric.common").orNull != "false"
val night_config_version: String = property("night_config_version").toString()
val ceres_version: String = providers.gradleProperty("ceres_version").get()
val sedna_version: String = providers.gradleProperty("sedna_version").get()
val sedna_buildroot_version: String = providers.gradleProperty("sedna_buildroot_version").get()
val fabric_api_version: String = property("fabric_api_version").toString()

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
    add("minecraft", "com.mojang:minecraft:$minecraft_version")
    // Same Mojang names as the NeoForm-based core and neoforge modules.
    if (!modernFabric) add("mappings", loomExtension.officialMojangMappings())
    add(if (modernFabric) "implementation" else "modImplementation", "net.fabricmc:fabric-loader:$fabric_loader_version")
    add(if (modernFabric) "implementation" else "modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabric_api_version")
    // Energy standard on Fabric (the counterpart of NeoForge's FE capability).
    add(if (modernFabric) "implementation" else "modImplementation", "maven.modrinth:forge-config-api-port:$forge_config_api_port_version")
    add("include", "maven.modrinth:forge-config-api-port:$forge_config_api_port_version")
    // The Modrinth artifact carries no transitive dependencies; Forge Config API Port needs these.
    implementation("com.electronwill.night-config:core:$night_config_version")
    implementation("com.electronwill.night-config:toml:$night_config_version")
    add("include", "com.electronwill.night-config:core:$night_config_version")
    add("include", "com.electronwill.night-config:toml:$night_config_version")
    add(if (modernFabric) "implementation" else "modImplementation", "teamreborn:energy:$team_reborn_energy_version")
    add("include", "teamreborn:energy:$team_reborn_energy_version")

    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    // @OnlyIn/Dist annotations used by shared code; missing annotation classes are ignored at runtime.
    // Non-transitive: only the annotation classes of the "api" artifact are needed, and the
    // transitive dependencies of mergetool (asm, jopt-simple, srgutils) are not resolvable here --
    // srgutils only lives on the Forge maven, which this module does not use. The lint tasks
    // resolve an auxiliary classpath that includes this dependency, so a transitive mergetool
    // breaks pmdMain (and every other task that resolves the runtime classpath) with
    // "Could not find net.minecraftforge:srgutils".
    compileOnly("net.neoforged:mergetool:2.0.0:api") { isTransitive = false }
    // Emulator and serialization libraries, shipped inside the mod jar like on NeoForge.
    implementation("li.cil.ceres:ceres:$ceres_version")
    implementation("li.cil.sedna:sedna:$sedna_version")
    implementation("li.cil.sedna:sedna-buildroot:$sedna_buildroot_version")
    add("include", "li.cil.ceres:ceres:$ceres_version")
    add("include", "li.cil.sedna:sedna:$sedna_version")
    add("include", "li.cil.sedna:sedna-buildroot:$sedna_buildroot_version")
}

// core is built against NeoForm, not Loom, so its jar cannot be remapped for production. Compile
// its sources as part of this module instead, so Loom remaps them together with the Fabric code.
sourceSets.main {
    java.srcDir(if (modernFabric) rootProject.file("core/versions/$minecraft_version/build/generated/stonecutter/main/java")
        else rootProject.file("core/src/main/java"))
    // Compiles the shared mod code (src/main/java of the root project) too, see docs/roadmap/multiloader.md §42.
    // -Pfabric.common=false compiles the Fabric module alone (it then no longer builds, the Fabric glue uses
    // shared classes).
    if (includeSharedCode) {
        // A separate source set so the excludes apply to the shared sources only: some excluded NeoForge
        // classes have a Fabric counterpart with the same path under fabric/src/main/java.
        val shared = objects.sourceDirectorySet("sharedMod", "Shared mod code")
        shared.srcDir(if (modernFabric) rootProject.file("versions/$minecraft_version/build/generated/stonecutter/main/java")
            else rootProject.file("src/main/java"))
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
            "li/cil/oc2/client/mixin/**",
            // JEI/ProjectRed/Create integrations are NeoForge-specific for now.
            "li/cil/oc2/common/integration/jei/**",
            "li/cil/oc2/common/integration/projectred/**")
        java.source(shared)
        if (modernFabric) {
            // These accessors contain vanilla-only code needed by the common projector.
            // Other shared mixins remain excluded because their hooks belong to NeoForge.
            val projectorAccessors = objects.sourceDirectorySet("projectorAccessors", "Shared vanilla projector accessors")
            projectorAccessors.srcDir(rootProject.file("versions/$minecraft_version/build/generated/stonecutter/main/java"))
            projectorAccessors.include(
                "li/cil/oc2/client/mixin/ParticleEngineAccessor.java",
                "li/cil/oc2/client/mixin/ParticleGroupAccessor.java",
                "li/cil/oc2/client/mixin/ProjectorRenderTypeAccessor.java")
            java.source(projectorAccessors)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
    options.release = javaVersion
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
    from(rootProject.file("LICENSE-MarkdownManual"))
    // Reuse the versioned vanilla resource conversion: recipe ingredients, item definitions,
    // loot tables and dynamic model IDs must match the same Minecraft version on both loaders.
    val sharedResources = if (modernFabric)
        rootProject.file("versions/$minecraft_version/build/resources/main")
        else rootProject.file("src/main/resources")
    if (modernFabric) dependsOn(":$minecraft_version:processResources")
    from(sharedResources) {
        include("assets/**", "data/**", "natives/**", "onyxos/**", "pack.mcmeta")
        // The Fabric task packages scripts below; do not copy the NeoForge copy twice.
        exclude("data/oc2r/file_systems/scripts.zip", "data/oc2r/file_systems/README.md")
    }
    // The guest scripts archive (same content as the root project's packageScripts task).
    from(packageScripts) {
        into("data/oc2r/file_systems")
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", minecraft_version)
    inputs.property("fabric_loader_version", fabric_loader_version)
    inputs.property("java_version", javaVersion)
    inputs.property("fabric_blockstate_format", "vanilla-model-v1")
    if (!modernFabric) exclude("oc2r.fabric.shared-client.mixins.json", "oc2r-26.accesswidener")
    if (modernFabric) {
        filesMatching("oc2r.fabric*.mixins.json") {
            filter { line: String -> line.replace("JAVA_21", "JAVA_25").replace("\"required\": false", "\"required\": true") }
        }
    }
    if (modernFabric) {
        // Fabric's model plugin supplies dynamic geometry; keep the fallback JSON vanilla-readable.
        filesMatching("assets/oc2r/blockstates/bus_cable.json") {
            filter { line: String -> line.replace("\"type\": \"oc2r:bus_cable\"",
                "\"model\": \"oc2r:block/cable_base\"") }
        }
        filesMatching("assets/oc2r/blockstates/monitor.json") {
            filter { line: String -> line.replace("\"type\": \"oc2r:monitor\"",
                "\"model\": \"oc2r:block/monitor\"") }
        }
    }
    filesMatching("fabric.mod.json") {
        if (!modernFabric) filter { line: String ->
            if (line.contains("oc2r.fabric.shared-client.mixins.json")) "" else line
        }
        expand("version" to project.version,
            "minecraft_version" to minecraft_version,
            "fabric_loader_version" to fabric_loader_version,
            "java_version" to javaVersion,
            "access_widener" to if (modernFabric) "oc2r-26.accesswidener" else "oc2r.accesswidener")
    }
}

loomExtension.accessWidenerPath.set(rootProject.file(
    "fabric/src/main/resources/" + if (modernFabric) "oc2r-26.accesswidener" else "oc2r.accesswidener"))

if (modernFabric) {
    // Explicit dependencies cover both compilation and source archives. These source roots
    // belong to other Stonecutter branches, so the Fabric plugin cannot infer them itself.
    tasks.named("compileJava") {
        dependsOn(":$minecraft_version:stonecutterGenerate", ":core:$minecraft_version:stonecutterGenerate")
    }
    tasks.named("sourcesJar") {
        dependsOn(":$minecraft_version:stonecutterGenerate", ":core:$minecraft_version:stonecutterGenerate")
    }
    tasks.withType<Checkstyle>().configureEach {
        dependsOn(":$minecraft_version:stonecutterGenerate", ":core:$minecraft_version:stonecutterGenerate")
    }
}

// Runs shared tests and Fabric bridge tests on a real dedicated server.
// ./gradlew :fabric:1.21.1:runGameTest
extensions.configure<FabricApiExtension> {
    configureTests {
        createSourceSet = true
        modId = "oc2r_gametest"
        enableGameTests = true
        enableClientGameTests = clientAcceptance
        eula = true
    }
}

val gameTestReport = layout.buildDirectory.file("test-results/gameTest/results.xml")
// Explicit local override for disposable test worlds on slow storage. Version suffixes keep nodes isolated.
val gameTestRunDirectory = providers.gradleProperty("fabric.gameTestRunDir")
    .map { rootProject.file(it).resolve(minecraft_version) }
    .orElse(layout.buildDirectory.dir("run/gameTest").map { it.asFile })
loomExtension.runConfigs.named("gameTest") {
    vmArg("-Dfabric.noGui=true")
    runDirectory.fileProvider(gameTestRunDirectory)
    vmArg("-Dfabric-api.gametest.report-file=${gameTestReport.get().asFile.absolutePath}")
}

if (clientAcceptance) {
    val clientTestRun = loomExtension.runConfigs.named("clientGameTest")
    clientTestRun.configure {
        vmArg("-Dfabric.noGui=true")
    }
    tasks.named<net.fabricmc.loom.task.RunGameTask>("runClientGameTest") {
        timeout.set(Duration.ofMinutes(15))
        // Test clients must stay silent, including title-screen music before the test entrypoint.
        environment("ALSOFT_DRIVERS", "null")
        doFirst {
            val options = clientTestRun.get().runDirectory.get().asFile.resolve("options.txt")
            options.parentFile.mkdirs()
            val retained = if (options.isFile) options.readLines()
                .filterNot { it.startsWith("soundCategory_master:") } else emptyList()
            options.writeText((retained + "soundCategory_master:0.0").joinToString("\n", postfix = "\n"))
        }
    }
}

// Count compiled method annotations rather than raw conditional Java text. This also catches
// annotated classes accidentally omitted from fabric.mod.json without initializing Minecraft.
fun compiledGameTestCount(): Int {
    var count = 0
    sourceSets["gametest"].output.classesDirs.files.forEach { root ->
        if (root.isDirectory) root.walkTopDown().filter { it.extension == "class" }.forEach { file ->
            ClassReader(file.readBytes()).accept(object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(access: Int, name: String?, descriptor: String?,
                                         signature: String?, exceptions: Array<out String>?): MethodVisitor {
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                            if (descriptor == "Lnet/minecraft/gametest/framework/GameTest;"
                                || descriptor == "Lnet/fabricmc/fabric/api/gametest/v1/GameTest;") count++
                            return null
                        }
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }
    }
    return count
}

// GameTests create many mock-player records. They are disposable and keeping them between runs
// makes the test server's autosave and shutdown increasingly expensive.
tasks.named("runGameTest") {
    timeout.set(Duration.ofMinutes(12))
    outputs.file(gameTestReport)
    outputs.upToDateWhen { false }
    doFirst {
        val report = gameTestReport.get().asFile
        report.parentFile.mkdirs()
        delete(report)
        val world = gameTestRunDirectory.get().resolve("world")
        val sessionLock = world.resolve("session.lock")
        if (sessionLock.exists()) {
            // Check Minecraft's lock before removing its world, including when another
            // Gradle invocation is still waiting for the previous server to flush chunks.
            FileChannel.open(sessionLock.toPath(), StandardOpenOption.WRITE).use { channel ->
                val lock = channel.tryLock()
                    ?: throw GradleException("Fabric GameTest world is still in use: $world")
                lock.release()
            }
        }
        delete(world)
    }
    doLast {
        val report = gameTestReport.get().asFile
        if (!report.isFile || report.length() == 0L) {
            throw GradleException("Fabric GameTests produced no fresh JUnit report: $report")
        }
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        val document = factory.newDocumentBuilder().parse(report)
        val cases = document.getElementsByTagName("testcase")
        // Modern vanilla contributes minecraft:always_pass; it cannot replace an OC2R test.
        val actual = if (modernFabric) (0 until cases.length).count {
            cases.item(it).attributes.getNamedItem("name").nodeValue.startsWith("oc2r_gametest:")
        } else cases.length
        val expected = compiledGameTestCount()
        val failures = document.getElementsByTagName("failure").length
        val errors = document.getElementsByTagName("error").length
        val skipped = document.getElementsByTagName("skipped").length
        if (expected == 0 || actual != expected || failures != 0 || errors != 0 || skipped != 0) {
            throw GradleException("Fabric GameTests incomplete: $actual/$expected tests, "
                + "$failures failures, $errors errors, $skipped skipped ($report)")
        }
        logger.lifecycle("Fabric $minecraft_version GameTests: $actual/$expected passed; report $report")
    }
}

// The NeoForge game tests (src/main/java/.../gametest) also run on Fabric. They use two NeoForge-only
// annotations and a NeoForge-patched @GameTest attribute, so the sources are copied with those
// lines rewritten into vanilla form before the gametest source set compiles them.
val sharedGameTestsDir = layout.buildDirectory.dir("generated/sharedGameTests")
val sharedGameTestSources = tasks.register<Sync>("sharedGameTestSources") {
    if (modernFabric) dependsOn(":$minecraft_version:stonecutterGenerate")
    from(rootProject.file(if (modernFabric)
        "versions/$minecraft_version/build/generated/stonecutter/main/java/li/cil/oc2/gametest"
        else "src/main/java/li/cil/oc2/gametest")) {
        exclude("GameTestResultReporter.java", "GameTestRegistrar.java")
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
    doLast {
        // Limit the structures and player fixtures ticking together to one shared class.
        // NeoForge consumes the original sources and keeps its native batch behavior.
        sharedGameTestsDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { source ->
                val batch = "oc2r_shared_${source.nameWithoutExtension.removeSuffix("Tests").lowercase()}"
                var text = source.readText()
                if (modernFabric) {
                    text = text.replace(
                        "import li.cil.oc2.gametest.GameTestRegistrar.GameTest;",
                        "import net.fabricmc.fabric.api.gametest.v1.GameTest;")
                    // Fabric discovers instance methods, while NeoForge's registrar uses static ones.
                    if (source.name.endsWith("Tests.java")) {
                        text = text.replace("public static void ", "public void ")
                    }
                    text = text.replace("template = TestSupport.TEMPLATE_NAMESPACE + \":\" + TestSupport.TEMPLATE",
                        "structure = TestSupport.TEMPLATE_NAMESPACE + \":\" + TestSupport.TEMPLATE")
                    text = text.replace("timeoutTicks =", "maxTicks =")
                    val environment = layout.buildDirectory.file(
                        "generated/gametestResources/data/oc2r_gametest/test_environment/$batch.json").get().asFile
                    environment.parentFile.mkdirs()
                    environment.writeText("""{"type":"minecraft:all_of","definitions":[]}""")
                }
                val batched = Regex("@GameTest\\(([^()]*)\\)").replace(text) { match ->
                    val parameters = match.groupValues[1]
                    if (modernFabric) {
                        val options = parameters.replace(Regex("batch\\s*=\\s*\"[^\"]*\"\\s*,?"), "")
                        val timing = if (options.contains("maxTicks")) "" else ", maxTicks = 100"
                        "@GameTest($options$timing, environment = \"oc2r_gametest:$batch\")"
                    } else if (Regex("\\bbatch\\s*=").containsMatchIn(parameters)) {
                        match.value
                    } else {
                        "@GameTest($parameters, batch = \"$batch\")"
                    }
                }
                if (batched != text) source.writeText(batched)
            }
    }
}

sourceSets.named("gametest") {
    java.srcDir(sharedGameTestsDir)
    if (modernFabric) resources.srcDir(layout.buildDirectory.dir("generated/gametestResources"))
    if (clientAcceptance) java.srcDir(rootProject.file("fabric/src/clientGametest/java"))
}

tasks.named("compileGametestJava") {
    dependsOn(sharedGameTestSources)
}
tasks.named<ProcessResources>("processGametestResources") {
    dependsOn(sharedGameTestSources)
    inputs.property("clientAcceptance", clientAcceptance)
    if (clientAcceptance) {
        filesMatching("fabric.mod.json") {
            filter { line: String ->
                if (line.contains("\"entrypoints\"")) line +
                    "\n    \"fabric-client-gametest\": [\"li.cil.oc2.fabric.gametest.FabricClientAcceptanceTest\"],"
                else line
            }
        }
    }
}

/* ── Static analysis: same tool version and config as the root project ──── */

checkstyle {
    toolVersion = "10.21.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

tasks.withType<Checkstyle>().configureEach {
    isEnabled = true
    exclude("**/jcodec/**", "**/generated/**", "**/gametest/**")
}

pmd {
    // First PMD release that can resolve Minecraft 26.x / Java 25 class files.
    toolVersion = "7.16.0"
    ruleSetConfig = rootProject.resources.text.fromFile(rootProject.file("config/pmd/ruleset.xml"))
    isIgnoreFailures = false
    isConsoleOutput = true
}

tasks.withType<Pmd>().configureEach {
    isEnabled = true
    exclude("**/jcodec/**", "**/generated/**", "**/gametest/**")
}

// The main source set of this module compiles three trees: the module's own sources
// (fabric/src/main/java), the loader-independent core module (core/src/main/java) and the
// shared mod code of the root project (src/main/java). The core node and the root node lint
// the latter two with the same ruleset, so PMD must only see the module's own sources here:
// analysing them again would report every finding of the shared tree a second time, and the
// lint ratchet counts violations per report, not per file.
tasks.named<Pmd>("pmdMain") {
    // Analyse the selected version, not the raw 1.21.1 branch with modern code commented out.
    val ownSources = if (modernFabric) layout.buildDirectory.dir("generated/stonecutter/main/java").get().asFile
        else rootProject.file("fabric/src/main/java")
    setSource(fileTree(ownSources) { include("**/*.java") })
    if (modernFabric) dependsOn("stonecutterGenerate")
}

/* ── Static analysis: SpotBugs ──────────────────────────────────────────────── */

// Same engine and filters as the root project, scoped to the sources of this module. The main
// source set also compiles the loader-independent core module and the shared mod code of the
// root project; both are analysed by their own nodes, and their classes carry no relation to
// this module's analysis (a baseline entry is matched by instance hash, which differs when the
// same source is compiled against different mappings). Analysing them here would therefore
// report the shared tree a second time under hashes no baseline can match.
spotbugs {
    toolVersion = "4.10.3"
    ignoreFailures = false
    showProgress = false
    excludeFilter.set(rootProject.file("config/spotbugs/exclude-filter.xml"))
}

val fabricOwnSources = rootProject.fileTree("fabric/src/main/java") { include("**/*.java") }
// Fully qualified names of the classes that this module's own sources produce (an inner class is
// covered by its outer class name, because "$" is stripped below).
val fabricOwnClassNames: Set<String> = fabricOwnSources.files.mapTo(mutableSetOf()) { source ->
    source.relativeTo(rootProject.file("fabric/src/main/java"))
        .invariantSeparatorsPath
        .removeSuffix(".java")
        .replace('/', '.')
}

fun isFabricOwnClass(file: File): Boolean {
    if (!file.name.endsWith(".class")) {
        return false
    }
    // Only classes of the java output, not resources of the same source set.
    val marker = "/classes/java/main/"
    val path = file.invariantSeparatorsPath
    val index = path.indexOf(marker)
    if (index < 0) {
        return false
    }
    val className = path.substring(index + marker.length).removeSuffix(".class").replace('/', '.')
    return className.substringBefore('$') in fabricOwnClassNames
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html")
    reports.create("xml")
    // Only the classes compiled from this module's own sources, no shared or core code.
    classes = classes?.filter(::isFabricOwnClass)
    // The excluded classes still have to be resolvable as types (auxiliary classpath entries are
    // never analysed), otherwise every reference into the shared tree is reported as missing.
    auxClassPaths.from(sourceSets["main"].output.classesDirs)
}
