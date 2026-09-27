plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.144"
    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.5.10"
}

// Vanilla Minecraft only (NeoForm, no loader): lets core declare Minecraft-typed bridge APIs.
// neoform (vanilla mappings) and neo_version (loader) track different artifacts, so each has
// its own gradle.properties key.
neoForge {
    neoFormVersion = providers.gradleProperty("neoform_version").get()
}

// Loader-independent module: it must never depend on the NeoForge or Fabric loader APIs (see
// docs/MULTILOADER.md, Stage 1); Minecraft types come from NeoForm above. Loader modules
// implement the bridge interfaces defined here.
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

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
