plugins {
    `java-library`
}

// Loader-independent module: plain JDK only. It must never depend on NeoForge or Fabric
// (see docs/MULTILOADER.md, Stage 1). Loader modules implement the bridge interfaces defined here.
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenCentral()
}

val jupiter_version: String = providers.gradleProperty("jupiter_version").get()

dependencies {
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
