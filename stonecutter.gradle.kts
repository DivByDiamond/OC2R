import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("dev.kikugie.stonecutter")
}

// Keep the working tree in the VCS state (1.21.1) so commits stay consistent.
// Switch with `./gradlew "Set active project to <version>"`; reset before committing
// with `./gradlew "Reset active project"`.
stonecutter active "1.21.1"

// Shared configuration for every versioned node (stonecutter `parameters`).
//
// The replacements below are anchored to the beginning of a code line: the match may only
// start on a line that is neither a `//` comment nor a Javadoc continuation (`* ...`), and the
// token may sit anywhere on that line. Without the anchor the replacements also hit comments
// and string literals, where two of them produce nonsense -- a Javadoc mention of
// `@OnlyIn(Dist.CLIENT)` used to turn into a fully qualified annotation, and a mention of
// `Properties.of()` into a fully qualified call.
//
// The anchor is `^` plus a "not a comment line" guard. The guard skips `//` lines and `*`
// (Javadoc) lines, but not `/* ...` lines: those carry the Stonecutter conditional blocks,
// whose content is active code in the version the block belongs to.
//
// The group reference in a replacement is written `${'$'}1` because Kotlin string templates
// would read a bare `$1` as the start of a template expression.
stonecutter parameters {
    // 26.1+ ships Mojang names again: `ResourceLocation` was renamed to `Identifier`.
    // Applied per node: forward on >=26.1 (ResourceLocation -> Identifier),
    // reverse on <26.1 (Identifier -> ResourceLocation) when sources are switched back.
    // `\b` keeps `ResourceLocationException` untouched; the reverse lookbehind keeps
    // `SPDX-License-Identifier` (jcodec) untouched.
    //
    // This one stays a plain token rename: eleven lines carry two `ResourceLocation` tokens
    // (`Pair<ResourceLocation, ResourceLocation>`, `ResourceLocation.ID = ResourceLocation...`),
    // and a line anchored replacement would rename only the first one, which breaks the 26.x
    // build. A rename also has to reach `{@link ResourceLocation}` in the Javadoc, because the
    // 26.x tree has no `ResourceLocation` to link to.
    replacements {
        regex(eval(current.version, ">=26.1")) {
            replace(
                "\\bResourceLocation\\b", "Identifier",
                "(?<!-)\\bIdentifier\\b", "ResourceLocation",
            )
        }
        // 26.1+ split GuiGraphics into the GuiGraphicsExtractor that screens and widgets
        // extract their render state into. No line carries the token twice, so the replacement
        // can be anchored to the start of the code line. Line comments are skipped, but Javadoc
        // is not: a `{@link #render(GuiGraphics, int, int)}` has to name the class that the
        // 26.x tree actually has, otherwise the link would not resolve.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "(?m)^(?![ \\t]*//)((?:[^\\n/]|/(?!/))*?)\\bGuiGraphics\\b", "${'$'}1GuiGraphicsExtractor",
                "(?m)^(?![ \\t]*//)((?:[^\\n/]|/(?!/))*?)\\bGuiGraphicsExtractor\\b", "${'$'}1GuiGraphics",
            )
        }
        // 26.1+ blocks need their id in the properties; BlockProps reads it from the registration context.
        // The replacement expands to a fully qualified call, so it is anchored to a code line.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "(?m)^(?![ \\t]*(?://|\\*))((?:[^\\n/]|/(?!/))*?)(?<![.\\w])Properties\\.of\\(\\)",
                "${'$'}1li.cil.oc2.common.block.BlockProps.of()",
                "(?m)^(?![ \\t]*(?://|\\*))((?:[^\\n/]|/(?!/))*?)(?<![.\\w])li\\.cil\\.oc2\\.common\\.block\\.BlockProps\\.of\\(\\)",
                "${'$'}1Properties.of()",
            )
        }
        // 26.x NeoForge no longer strips members by @OnlyIn and warns on every use at startup;
        // the annotated classes are only referenced from client code, so drop the marker there.
        // The marker is always the first thing on its line, so the replacement is anchored to
        // the start of the line and never touches the `{@code @OnlyIn(Dist.CLIENT)}` mention in
        // the Javadoc of ClientOnly.
        regex(eval(current.version, ">=26.1")) {
            replace(
                "(?m)^([ \\t]*)@OnlyIn\\(Dist\\.CLIENT\\)", "${'$'}1@li.cil.oc2.common.util.ClientOnly",
                "(?m)^([ \\t]*)@li\\.cil\\.oc2\\.common\\.util\\.ClientOnly", "${'$'}1@OnlyIn(Dist.CLIENT)",
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
