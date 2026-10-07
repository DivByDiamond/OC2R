plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.5.10"
}

// Vanilla Minecraft only (NeoForm, no loader): lets core declare Minecraft-typed bridge APIs.
// neoform (vanilla mappings) and neo_version (loader) track different artifacts, so each has
// its own gradle.properties key. Per-version overrides live in versions/<v>/gradle.properties
// and are read with property() (providers.gradleProperty ignores project-dir files).
neoForge {
    neoFormVersion = property("neoform_version").toString()
}

// Loader-independent module: it must never depend on the NeoForge or Fabric loader APIs (see
// docs/MULTILOADER.md, Stage 1); Minecraft types come from NeoForm above. Loader modules
// implement the bridge interfaces defined here.
java.toolchain.languageVersion = JavaLanguageVersion.of((property("java_version") as String).toInt())

dependencyLocking {
    lockAllConfigurations()
}

repositories {
    mavenCentral()
}

val jupiter_version: String = providers.gradleProperty("jupiter_version").get()

dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiter_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiter_version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "utf-8"
}

tasks.test {
    useJUnitPlatform()
    // Multiversion: the node lives in core/versions/<v>/ but the branch's source root (and the
    // Stage-2 test CWD) is core/.
    workingDir = rootProject.layout.projectDirectory.dir("core").asFile
}

/* ── Static analysis: same tool versions and configs as the root project ──── */

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
    toolVersion = "7.7.0"
    ruleSetConfig = rootProject.resources.text.fromFile(rootProject.file("config/pmd/ruleset.xml"))
    isIgnoreFailures = false
    isConsoleOutput = true
}

tasks.withType<Pmd>().configureEach {
    isEnabled = true
    exclude("**/jcodec/**", "**/generated/**", "**/gametest/**")
}

spotbugs {
    toolVersion.set("4.10.3")
    ignoreFailures.set(false)
    showProgress.set(true)
    excludeFilter.set(rootProject.file("config/spotbugs/exclude-filter.xml"))
    baselineFile.set(rootProject.file("config/spotbugs/baseline.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html")
    reports.create("xml")
    // Same package-info crash workaround as the root project (see build.gradle.kts).
    classes = classes?.filter { !it.name.endsWith("package-info.class") }
}
