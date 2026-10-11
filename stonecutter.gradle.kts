import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("dev.kikugie.stonecutter")
}

// Keep the working tree in the VCS state (1.21.1) so commits stay consistent.
// Switch with `./gradlew "Set active project to <version>"`; reset before committing
// with `./gradlew "Reset active project"`.
stonecutter active "1.21.1"

// Shared configuration for every versioned node (stonecutter `parameters`).
stonecutter parameters {
    // 26.1+ ships Mojang names again: `ResourceLocation` was renamed to `Identifier`.
    // Applied per node: forward on >=26.1 (ResourceLocation -> Identifier),
    // reverse on <26.1 (Identifier -> ResourceLocation) when sources are switched back.
    // `\b` keeps `ResourceLocationException` untouched; the reverse lookbehind keeps
    // `SPDX-License-Identifier` (jcodec) untouched.
    replacements {
        regex(eval(current.version, ">=26.1")) {
            replace(
                "\\bResourceLocation\\b", "Identifier",
                "(?<!-)\\bIdentifier\\b", "ResourceLocation",
            )
        }
        // 26.1+ split GuiGraphics into the GuiGraphicsExtractor that screens and widgets
        // extract their render state into.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "\\bGuiGraphics\\b", "GuiGraphicsExtractor",
                "\\bGuiGraphicsExtractor\\b", "GuiGraphics",
            )
        }
        // 26.1+ blocks need their id in the properties; BlockProps reads it from the registration context.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "(?<![.\\w])Properties\\.of\\(\\)", "li.cil.oc2.common.block.BlockProps.of()",
                "li\\.cil\\.oc2\\.common\\.block\\.BlockProps\\.of\\(\\)", "Properties.of()",
            )
        }
        // 26.x NeoForge no longer strips members by @OnlyIn and warns on every use at startup;
        // the annotated classes are only referenced from client code, so drop the marker there.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "@OnlyIn\\(Dist\\.CLIENT\\)", "@li.cil.oc2.common.util.ClientOnly",
                "@li\\.cil\\.oc2\\.common\\.util\\.ClientOnly", "@OnlyIn(Dist.CLIENT)",
            )
        }
    }
}

System.setProperty("line.separator", "\n")

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000", "-h", layout.buildDirectory.get().toString().replace("\\", "/") + "/c"))
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
