import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.io.IOUtils
import net.ltgt.gradle.errorprone.errorprone

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("commons-io:commons-io:2.21.0")
    }
}

plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.moddev") version "2.0.144"

    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.5.10"
    id("net.ltgt.errorprone") version "5.1.0"
    id("org.jetbrains.qodana") version "2026.2.0"
}

fun getGitRef(): String {
    return try {
        val pb = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        pb.redirectErrorStream(true)
        val process = pb.start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (ignored: Throwable) {
        "unknown"
    }
}

val semver: String get() = property("semver") as String
val modId: String get() = property("modId") as String
val neo_version: String get() = property("neo_version") as String
val ceres_version: String get() = property("ceres_version") as String
val sedna_version: String get() = property("sedna_version") as String
val sedna_buildroot_version: String get() = property("sedna_buildroot_version") as String
val parchment_mappings_version: String get() = property("parchment_mappings_version") as String
val parchment_minecraft_version: String get() = property("parchment_minecraft_version") as String
val minecraft_version: String get() = property("minecraft_version") as String
val minecraft_sdk: String get() = property("minecraft_sdk") as String
val jei_version: String get() = property("jei_version") as String
val pr_version: String get() = property("pr_version") as String
val ccl_version: String get() = property("ccl_version") as String
val cbm_version: String get() = property("cbm_version") as String
val architectury_project_id: String get() = property("architectury_project_id") as String
val architectury_file_id: String get() = property("architectury_file_id") as String
val markdownmanual_project_id: String get() = property("markdownmanual_project_id") as String
val markdownmanual_file_id: String get() = property("markdownmanual_file_id") as String
val native_networking_repo: String get() = property("native_networking_repo") as String
val network_lib_version: String get() = property("network_lib_version") as String
val embeddium_project_id: String get() = property("embeddium_project_id") as String
val embeddium_file_id: String get() = property("embeddium_file_id") as String
val embeddium_plus_plus_project_id: String get() = property("embeddium_plus_plus_project_id") as String
val embeddium_plus_plus_file_id: String get() = property("embeddium_plus_plus_file_id") as String
val oculus_project_id: String get() = property("oculus_project_id") as String
val oculus_file_id: String get() = property("oculus_file_id") as String
val mockito_version: String get() = property("mockito_version") as String
val jupiter_version: String get() = property("jupiter_version") as String
val debug_embeddium: String get() = property("debug_embeddium") as String
val debug_embeddium_plus_plus: String get() = property("debug_embeddium_plus_plus") as String
val debug_oculus: String get() = property("debug_oculus") as String

gradle.projectsEvaluated {
    project.configurations.named("jarJar") {
        val nativeLibsDir = file("src/main/resources/natives")
        val nativeNetworkingBaseUrl = "https://github.com/${native_networking_repo}/releases/download/${network_lib_version}/"

        val targets = listOf(
            "macos/liboc2rnet-x86_64.dylib",
            "macos/liboc2rnet-arm64.dylib",
            "windows/oc2rnet-x86_64.dll",
            "windows/oc2rnet-arm64.dll",
            "linux/liboc2rnet-linux-x86_64.so",
            "linux/liboc2rnet-linux-arm64.so",
            "android/liboc2rnet-android-arm64.so",
            "android/liboc2rnet-android-x86_64.so"
        )

        val versionFile = file("$nativeLibsDir/.version")
        val oldVersion = if (versionFile.isFile) versionFile.bufferedReader().use { it.readLine() } else ""
        if (oldVersion != network_lib_version) {
            targets.forEach { path ->
                val targetFile = file("$nativeLibsDir/$path")
                targetFile.parentFile.mkdirs()
                val fileName = path.split('/').last()
                val url = URI("${nativeNetworkingBaseUrl}${fileName}").toURL()
                println("Downloading ${url} → ${targetFile}")
                targetFile.outputStream().use { out ->
                    url.openStream().use { stream -> IOUtils.copy(stream, out) }
                }
            }

            versionFile.writeText("$network_lib_version\n")
        }
    }
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000", "-h", layout.buildDirectory.get().toString().replace("\\", "/") + "/c"))
        }
    }
}

version = if (System.getenv("RELEASE_TYPE") == "release") semver.trimStart('v') else "${semver.trimStart('v')}+${getGitRef()}"
group = "li.cil.oc2"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
}

val hasGithubPackageCredentials =
    (hasProperty("gpr.user") && hasProperty("gpr.key")) ||
        (System.getenv("GITHUB_ACTOR") != null && System.getenv("GITHUB_TOKEN") != null)

repositories {
    mavenCentral()
    maven {
        url = uri("https://cursemaven.com")
        // Without the filter a 429 from cursemaven aborts resolution of modules
        // that were never going to be found here (JEI, ProjectRed, li.cil, ...).
        content { includeGroup("curse.maven") }
    }
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
        content { includeGroupByRegex("mezz\\..*") }
    }
    maven {
        url = uri("https://maven.covers1624.net/")
        content { includeGroupByRegex("io\\.codechicken(\\..*)?|mrtjp(\\..*)?") }
    }
    maven {
        url = uri("https://api.modrinth.com/maven")
        content { includeGroup("maven.modrinth") }
    }
    maven {
        url = uri("https://maven.neoforged.net/releases/")
    }
    if (hasGithubPackageCredentials) {
        val githubPackages = listOf(
            "fnuecke/ceres" to "li.cil.ceres",
            "North-Western-Development/sedna" to "li.cil.sedna",
            "North-Western-Development/minux" to "li.cil.sedna"
        )
        githubPackages.forEach { (repo, groupName) ->
            maven {
                url = uri("https://maven.pkg.github.com/${repo}")
                credentials {
                    username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
                content { includeGroup(groupName) }
            }
        }
    }
    maven {
        name = "localLibs"
        url = uri("libs")
    }
}

neoForge {
    version = neo_version

    parchment {
        mappingsVersion = parchment_mappings_version
        minecraftVersion = parchment_minecraft_version
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("gameTestServer") {
            type.set("gameTestServer")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("data") {
            data()
            programArguments.addAll("--mod", modId, "--all", "--output", file("src/generated/resources/").absolutePath, "--existing", file("src/main/resources/").absolutePath)
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")

            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")

    // §170: Error Prone compiler analysis. Pinned to a version compatible with the
    // Java 21 toolchain (Error Prone 2.43+ requires JDK 21 to run).
    errorprone("com.google.errorprone:error_prone_core:2.50.0")

    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))

    implementation("li.cil.ceres:ceres:${ceres_version}")
    add("jarJar", "li.cil.ceres:ceres:${ceres_version}")

    implementation("li.cil.sedna:sedna:${sedna_version}")
    add("jarJar", "li.cil.sedna:sedna:${sedna_version}")

    implementation("li.cil.sedna:sedna-buildroot:${sedna_buildroot_version}")
    add("jarJar", "li.cil.sedna:sedna-buildroot:${sedna_buildroot_version}")

    add("additionalRuntimeClasspath", "li.cil.ceres:ceres:${ceres_version}")
    add("additionalRuntimeClasspath", "li.cil.sedna:sedna:${sedna_version}")
    add("additionalRuntimeClasspath", "li.cil.sedna:sedna-buildroot:${sedna_buildroot_version}")

    implementation("curse.maven:architectury-api-${architectury_project_id}:${architectury_file_id}")
    implementation("maven.modrinth:13P81Hg3:1.3.1")

    compileOnly("mezz.jei:jei-${minecraft_version}-common-api:${jei_version}")
    compileOnly("mezz.jei:jei-${minecraft_version}-${minecraft_sdk}-api:${jei_version}")

    runtimeOnly("mezz.jei:jei-${minecraft_version}-${minecraft_sdk}:${jei_version}")

    compileOnly("mrtjp:ProjectRed:${minecraft_version}-${pr_version}:api")
    runtimeOnly("io.codechicken:CodeChickenLib:${minecraft_version}-${ccl_version}")
    runtimeOnly("io.codechicken:CBMultipart:${minecraft_version}-${cbm_version}")
    runtimeOnly("mrtjp:ProjectRed:${minecraft_version}-${pr_version}:core")
    runtimeOnly("mrtjp:ProjectRed:${minecraft_version}-${pr_version}:transmission")

    if (debug_embeddium.toBoolean()) {
        runtimeOnly("curse.maven:embeddium-${embeddium_project_id}:${embeddium_file_id}")
    }

    if (debug_embeddium_plus_plus.toBoolean()) {
        runtimeOnly("curse.maven:embeddiumplus-${embeddium_plus_plus_project_id}:${embeddium_plus_plus_file_id}")
    }

    if (debug_oculus.toBoolean()) {
        runtimeOnly("curse.maven:oculus-${oculus_project_id}:${oculus_file_id}")
    }

    testImplementation("org.mockito:mockito-core:${mockito_version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiter_version}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiter_version}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")

    // The terminal tests construct `new Terminal()` on the plain JUnit runtime classpath.
    // fastutil/log4j used to be added here by hand; since the test classpaths now share
    // the main dependency set (see below), Minecraft pins their versions transitively.
}

// Mockito cannot create mocks for interfaces whose method signatures reach into
// Minecraft types (e.g. LayerParameters.getSavedState -> Optional<Tag>), and inet
// layer classes load Minecraft NBT classes at runtime. Share the main dependency
// set (which carries the compiled Minecraft/NeoForge classes) with the test set.
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.compileClasspath.get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.runtimeClasspath.get())
}

System.setProperty("line.separator", "\n")

tasks.register<Zip>("packageScripts") {
    archiveFileName = "scripts.zip"
    destinationDirectory = file("${layout.buildDirectory.get()}/resources/main/data/oc2r/file_systems")
    from("src/main/scripts")
    from("src/main/resources/onyxos") {
        include("fw_jump.bin")
        into("firmware_files")
    }
}

tasks.register<Copy>("copyLicensesToResources") {
    from(".")
    into(file("${layout.buildDirectory.get()}/resources/main"))
    include("LICENSE*")
}

tasks.processResources {
    dependsOn("packageScripts")
    dependsOn("copyLicensesToResources")
}

tasks.named("jarJar") { }

tasks.register<Copy>("copyGeneratedResources") {
    from("src/generated")
    into("src/main")
    exclude("resources/.cache")
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "FMLAT" to "accesstransformer.cfg",
                "Specification-Title" to "oc2r",
                "Specification-Vendor" to "North Western Development (Originally by Sangar)",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to semver.trimStart('v'),
                "Implementation-Vendor" to "North Western Development (Originally by Sangar)",
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
                "MixinConfigs" to "mixins.oc2r.json",
                "ContainedDeps" to "commons-collections4-4.4.jar"
            )
        )
    }
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier.set("api")
    from(sourceSets.main.get().allSource)
    from(sourceSets.main.get().output)
    include("li/cil/oc2/api/**")
}

idea {
    module {
        for (exclude in listOf("assets", "run", "out", "logs", "src/generated")) {
            excludeDirs.add(file(exclude))
        }
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

/* ── Publishing ──────────────────────────────────────────────────────────── */
/* SPDX-License-Identifier: MIT */

publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            artifact(tasks["apiJar"])
            pom {
                name = "OC2R"
                description = "OpenComputers 2 Rewrite"
                url = "https://github.com/TumRedSun/OC2R"
                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.txt"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/DivByDiamond/OC2R")
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-classfile", "-Xlint:-processing", "-Xlint:-path", "-Xlint:-this-escape", "-Xlint:-serial", "-Xlint:-auxiliaryclass"))
        // Preserve parameter names in bytecode so reflection (e.g. RPCParameter.getName()
        // in li.cil.oc2.api.bus.device.object.Callbacks) can recover them at runtime without
        // requiring every @Callback-annotated method to also use @Parameter annotations.
        options.compilerArgs.add("-parameters")
    }
}

checkstyle {
    toolVersion = "10.21.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
}

tasks.withType<Checkstyle>().configureEach {
    isEnabled = true
    exclude("**/jcodec/**", "**/generated/**")
}

pmd {
    toolVersion = "7.7.0"
    ruleSetConfig = rootProject.resources.text.fromFile(rootProject.file("config/pmd/ruleset.xml"))
    isIgnoreFailures = true
    isConsoleOutput = true
}

tasks.withType<Pmd>().configureEach {
    isEnabled = true
    exclude("**/jcodec/**", "**/generated/**")
}

/* ── Static analysis: SpotBugs (§169) ─────────────────────────────────────── */

spotbugs {
    // §169: plugin 6.5.10 supports Gradle 9 (6.x line); toolVersion 4.10.3 bundles the analysis engine.
    toolVersion.set("4.10.3")
    ignoreFailures.set(true)
    showProgress.set(true)
    excludeFilter.set(rootProject.file("config/spotbugs/exclude-filter.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    // Plugin 6.x registers no reports by default; create html + xml explicitly
    // (output: build/reports/spotbugs/<base>.html|xml, required defaults to true).
    reports.create("html")
    reports.create("xml")
    // SpotBugs crashes with "ResourceNotFoundException" on package-info.class
    // files (it cannot re-open them as resources), so keep them out of the
    // analysis input. They carry no runnable code, only annotations.
    classes = classes?.filter { !it.name.endsWith("package-info.class") }
}

/* ── Static analysis: Error Prone (§170) ─────────────────────────────────── */

// Error Prone is wired but OFF by default so the regular build (incl. NeoForge
// moddev compile) is unaffected. Enable per-invocation with: ./gradlew compileJava -PenableErrorProne
// (bare flag or =true enables; =false disables). All diagnostics are demoted to
// warnings, so it can never fail the build.
val enableErrorProne = when (val v = project.findProperty("enableErrorProne")?.toString()) {
    null -> false // property absent -> off by default
    "" -> true // -PenableErrorProne (bare flag)
    else -> v.toBoolean()
}

// The `errorprone` configuration is a declaration-only (canBeResolved=false)
// config; create a resolvable view over it so we can pick out the Guava jar
// that error_prone_core resolves.
val errorProneResolvable = configurations.create("errorProneResolvable") {
    extendsFrom(configurations.named("errorprone").get())
    isCanBeResolved = true
    isCanBeConsumed = false
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        enabled.set(enableErrorProne)
        if (enableErrorProne) {
            allErrorsAsWarnings.set(true)
            disableWarningsInGeneratedCode.set(true)
        }
    }
    if (enableErrorProne) {
        // SpongePowered's mixin annotation processor bundles an unrelocated
        // Guava 21.0 and lands on the processor path before Error Prone's deps,
        // so Error Prone crashes with NoSuchMethodError on
        // ImmutableMap.Builder.buildOrThrow(). Prepend the modern Guava that
        // error_prone_core resolves so it shadows the bundled copy.
        val epGuava = errorProneResolvable.filter { it.name.matches(Regex("guava-.*\\.jar")) }
        options.annotationProcessorPath = epGuava + (options.annotationProcessorPath ?: files())
    }
}

/* ── Static analysis: Qodana ──────────────────────────────────────────────── */

qodana {
    // Driven by qodana.yaml in the project root (linter, excludes, scope).
}

// Plugin 2026.x exposes the `qodanaScan` task; provide the `./gradlew qodana` alias too.
tasks.register("qodana") {
    group = "verification"
    description = "Run JetBrains Qodana analysis (alias for qodanaScan)."
    dependsOn("qodanaScan")
}

tasks.test {
    useJUnitPlatform()
}
