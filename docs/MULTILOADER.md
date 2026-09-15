# Multiloader / Multi-version Plan

Design doc for turning OC2R from a single NeoForge/1.21.1 module into a
mod that builds for NeoForge **and** Fabric, on MC 1.21.1, 26.1 and 26.2.
Written 2026-09-15, not yet implemented — this is the plan, not the
build. Each stage below gets its own implementation plan (and its own
PRs) when work actually starts on it; only Stage 1 is designed in detail
here.

## Why this is staged

236 of ~1050 Java files under `src/main/java` import NeoForge directly
(registries, capabilities, network handling, energy, FML events). That's
real coupling, not a config problem — "just add Fabric" isn't a thing.
Doing all six targets (3 MC versions × 2 loaders) in one effort would be
months of work with no working checkpoint in between. Splitting into
stages means `work` stays green and shippable after every stage, and
each stage is small enough to actually review.

**Stage 1** — `core`/`neoforge` module split, one subsystem (registries)
fully migrated as the reference pattern. Detailed below.
**Stage 2** — real `fabric` module; migrate the remaining NeoForge-coupled
subsystems (capabilities, network, energy, client events) one PR at a
time, following Stage 1's template.
**Stage 3** — Minecraft version multiplexing (1.21.1, 26.1, 26.2) via
Stonecutter, across both loaders.
**Stage 4** — CI/release matrix for all 6 targets; CurseForge/Modrinth
uploads per target.

## Background: Mojang's 2026 version scheme change

Starting 2026 Mojang switched from `1.x` to year-based versions
(`YY.drop[.hotfix]`). `1.21.11` was the last `1.x` release; `26.1` "Tiny
Takeover" (March 2026) and `26.2` "Chaos Cubed" (June 2026) are the first
two drops under the new scheme. Both NeoForge and Fabric already support
26.1/26.2 (Fabric: Loom 1.17 / Loader 0.19.x for 26.2; NeoForge: builds
available since the 26.1 snapshots). Naive string/dot version comparison
does not order `1.x` against `YY.x` correctly — use release chronology
(the official version manifest), not a version-string sort, anywhere the
build needs to compare versions.

## Stage 1 — `core`/`neoforge` split

### Goal

Prove the loader-independent/loader-specific boundary on one real
subsystem, with zero behavior change and zero new build targets. Done
when:

1. `core` builds standalone and has **no** direct dependency on NeoForge.
2. Registries are fully migrated: `core` (Bridge interface + types) →
   NeoForge implementation → tests/GameTest/CI, all green.
3. The other ~230 NeoForge-coupled files are **not** touched. They stay
   in their current form until their own later migration PR. Do not let
   "while we're at it" scope creep pull more subsystems into Stage 1.
4. The shipped jar is functionally identical to today's.
5. `./gradlew build` (checkstyle/PMD/SpotBugs/tests) and `ci-work.yml`
   stay green, with no changes to the workflow logic itself (only the
   module path being built changes).

### Module layout

```
oc2r/
├── core/       — loader-independent (plain java-library, no NeoForge/Fabric API)
│   └── li/cil/oc2/{common,client,api,data,jcodec}/...   (packages unchanged)
├── neoforge/   — net.neoforged.moddev (unchanged from today), entrypoint + Bridge impls
└── fabric/     — empty skeleton in Stage 1; real implementation lands in Stage 2
```

Dependency direction is one-way: `neoforge` / `fabric` → `core`. `core`
never imports `net.neoforged.*` or `net.fabricmc.*`.

**Package naming**: the existing `li.cil.oc2.common` package (server-side
game logic, as opposed to `li.cil.oc2.client`) is NOT renamed. It moves
into the `core` module as-is. The Gradle module is deliberately called
`core`, not `common` — `li.cil.oc2.common` already has an established
meaning in this codebase (server/shared game logic vs. client rendering)
that has nothing to do with loader-independence, and overloading the
word would make every future "common" reference ambiguous.

**Mappings**: `core` compiles against vanilla Minecraft through official
mappings + Parchment — already what this project uses. That convergence
point (both NeoForge's ModDevGradle and Fabric Loom can target official
mappings) is what makes a shared `core` module possible at all. The exact
Gradle wiring for how `core` gets a Minecraft artifact without pulling in
a real mod loader is an implementation-plan detail, not decided here.

**Bulk move mechanics**: moving `src/main/java` → `core/src/main/java`
does not change any package names, so it's a plain `git mv` (no import
rewriting needed — nothing breaks because no path changes). See "Tooling
notes" below for why `jmove` (a project-local Rust CLI) is not used for
this specific move, and where it *does* help later.

### Abstraction pattern

`Bridge` interfaces live in `core`; each loader module provides an
implementation, wired via `java.util.ServiceLoader`
(`META-INF/services/...`) — the same idiom MultiLoader-Template-style
multiloader mods use. Example shape:

```java
// core
public interface RegistryBridge {
    <T> Supplier<T> register(ResourceKey<? extends Registry<T>> registry, String name, Supplier<T> factory);
}
```
```java
// neoforge
public final class NeoForgeRegistryBridge implements RegistryBridge { /* DeferredRegister-backed */ }
```
```
// neoforge/src/main/resources/META-INF/services/li.cil.oc2.platform.RegistryBridge
li.cil.oc2.platform.NeoForgeRegistryBridge
```

### Toolchain choice: separate official plugins, not Architectury Loom

`neoforge` keeps using `net.neoforged.moddev` exactly as today (currently
2.0.144, with GameTest CI integration, datagen and checkstyle/PMD/SpotBugs
already wired against it). `fabric` (Stage 2) will use the official
Fabric Loom. Architectury Loom (a single unified toolchain across
Fabric/Forge/NeoForge) was considered and rejected:

| | Architectury Loom | ModDevGradle + Fabric Loom (chosen) |
|---|---|---|
| NeoForge today | switches away from the toolchain we just tuned CI/GameTest/datagen against | unchanged |
| Fabric later | convenient | equally convenient (official Loom) |
| Shared abstraction | Architectury API (optional) or our own | ours, full control |
| Dependency chain | Minecraft → NeoForge → Architectury Loom → project | Minecraft → NeoForge ModDev → project (and separately → Fabric Loom → project) |
| Migration risk now | real (touches a working, recently-hardened stack) | none |

The Architectury API runtime library (shared registry/network/energy
abstractions) is a separate, optional thing from Architectury Loom — we
use neither; `RegistryBridge` and friends are ours.

### Reference subsystem: registries

Chosen over capabilities (30 files), network (42 files) and energy (16
files) because it's the most foundational — nearly everything else
(blocks, items, block entities, menu types) registers through it — and
NeoForge's `DeferredRegister` vs. Fabric's `Registry.register` are close
enough in shape to design a clean, non-leaky `RegistryBridge` without
fighting event-bus/lifecycle differences (which capabilities and network
both have in spades). Once this one subsystem has gone
`core → Bridge → NeoForge impl → tests/CI`, the same shape gets reused
for capabilities, network and energy in Stage 2 — those become
applications of an established pattern, not fresh architecture work.

### Git / PR strategy

No long-lived multiloader branch. Branch-by-abstraction directly on
`work`, in small PRs, each green on its own:

1. Module skeleton (`core`/`neoforge` split, zero behavior change) — its
   own PR.
2. `RegistryBridge` + NeoForge implementation + registries migration —
   its own PR.

Every subsequent subsystem migration (Stage 2+) follows the same
two-PR-or-fewer shape: infra first if needed, then the migration itself.

### CI impact (Stage 1 only)

`ci-work.yml` / `build.yml` do not change their trigger/job logic — only
the Gradle task path changes (`:neoforge:build` instead of the current
root `build`), and the output artifact is the same jar. The
loader/version build matrix is a Stage 4 concern, once there's a second
real target to actually build.

## Tooling notes

**`jmove`** (`/storage/project/rust/jmove`, this account's project) is a
Rust CLI that moves Java/TS files and rewrites package + imports,
real-world tested against `google/guava`. Checked it against this exact
use case (2026-09-15): it refuses cross-source-root moves —

```
jmove: plan rejected: Java move: target directory 'core/src/main/java/li/cil/oc2/common'
  is outside the Java source root 'src/main/java'
```

— so it does **not** help with the Stage 1 bulk move (which doesn't need
it anyway, since no package name changes). It's the right tool for
**later**, targeted package renames during Stage 2+ subsystem migrations
(e.g. if `RegistryBridge`-style types get pulled into a new
`li.cil.oc2.platform` package) — that's exactly its tested Guava
use case. Two caveats before relying on it then:
- Build from commit `0216fd6` (last known-good) — the working tree has
  an uncommitted, non-compiling WIP (`fix` engine, `E0063` on
  `core::index::Index`).
- It does not auto-add missing imports for same-package implicit
  references (found this exact gap on the Guava smoke test) — run
  `jmove check` after any move it makes.
