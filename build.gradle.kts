import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.io.IOUtils

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
    id("net.neoforged.moddev") version "2.0.124"

    id("checkstyle")
    id("pmd")
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
    }
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
    }
    maven { url = uri("https://maven.covers1624.net/") }
    maven { url = uri("https://api.modrinth.com/maven") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
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
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    implementation(fileTree(mapOf("dir" to "libs", "include" to "*.jar")))

    implementation("li.cil.ceres:ceres:0.0.4")
    add("jarJar", "li.cil.ceres:ceres:0.0.4")

    implementation("li.cil.sedna:sedna:2.0.13")
    add("jarJar", "li.cil.sedna:sedna:2.0.13")

    implementation("li.cil.sedna:sedna-buildroot:0.0.64")
    add("jarJar", "li.cil.sedna:sedna-buildroot:0.0.64")

    add("additionalRuntimeClasspath", "li.cil.ceres:ceres:0.0.4")
    add("additionalRuntimeClasspath", "li.cil.sedna:sedna:2.0.13")
    add("additionalRuntimeClasspath", "li.cil.sedna:sedna-buildroot:0.0.64")

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

    testImplementation("org.mockito:mockito-inline:${mockito_version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${jupiter_version}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${jupiter_version}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

System.setProperty("line.separator", "\n")

tasks.register<Zip>("packageScripts") {
    archiveFileName = "scripts.zip"
    destinationDirectory = file("${layout.buildDirectory.get()}/resources/main/data/oc2r/file_systems")
    from("src/main/scripts")
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
                "Implementation-Version" to semver,
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
            url = uri("https://maven.pkg.github.com/TumRedSun/OC2R")
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

tasks.test {
    useJUnitPlatform()
}
