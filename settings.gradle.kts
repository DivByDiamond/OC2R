pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver") version "0.9.0"
}

val minecraft_version: String get() = gradle.providers.gradleProperty("minecraft_version").get()
val minecraft_sdk: String get() = gradle.providers.gradleProperty("minecraft_sdk").get()

rootProject.name = "oc2r-${minecraft_version}-${minecraft_sdk}"

fun substituteLocal(directoryName: String, libraryName: String) {
    val path = java.io.File("../${directoryName}")
    if (path.exists()) {
        println("Found local [${directoryName}] project, substituting...")
        includeBuild(path.toString()) {
            dependencySubstitution {
                substitute(module(libraryName)).using(project(":"))
            }
        }
    }
}

gradle.settingsEvaluated {
    val rootDir = settings.rootDir
    val marker = java.io.File(rootDir, "libs/li/cil/sedna/sedna-buildroot/0.0.70/sedna-buildroot-0.0.70.jar")
    if (!marker.isFile) {
        val script = java.io.File(rootDir, "scripts/download-libs.sh")
        if (script.isFile) {
            println("GitHub-Release-only deps (ceres/sedna/sedna-buildroot) missing from libs/ — running download-libs.sh once...")
            val pb = ProcessBuilder("bash", script.absolutePath)
                .directory(rootDir)
                .redirectErrorStream(true)
            val proc = pb.start()
            val stdout = proc.inputStream.reader().readText()
            proc.waitFor()
            if (proc.exitValue() != 0) {
                System.err.println(stdout)
                throw GradleException(
                    "Failed to auto-fetch GitHub-hosted deps via scripts/download-libs.sh (exit code ${proc.exitValue()}).\n" +
                    "Either fix the network issue and re-run, or set GITHUB_ACTOR/GITHUB_TOKEN to use the authenticated GitHub Packages repo instead.\n" +
                    "Script output is above."
                )
            } else {
                println(stdout)
            }
        }
    }
}

substituteLocal("ceres", "li.cil.ceres:ceres")
substituteLocal("sedna", "li.cil.sedna:sedna")
substituteLocal("minux", "li.cil.sedna:sedna-buildroot")
