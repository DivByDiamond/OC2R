# Contributing to OC2R

## Code Style

- **Java 21**, NeoForge, Gradle
- **≤200 lines per file**, **≤4 files per folder** (Single Responsibility Principle)
- **Comments**: `//` comments are allowed for complex logic, tricky edge cases, and important modules where intent is not obvious. Keep them concise and meaningful; avoid redundant or obvious comments.
- **JavaDoc** on public API methods
- **No SPDX headers**
- **Build before commit**: `./gradlew build`

## Project Layout

```
core/src/main/java/li/cil/oc2/
  platform/  - Loader-independent registry/platform bridge (no NeoForge/Fabric dependency)
  bus/       - Loader-independent device bus topology
  network/   - Loader-independent network geometry/math

src/main/java/li/cil/oc2/
  api/       - Public API (do not change without discussion!)
  client/    - Client-side only (rendering, GUI)
  common/    - Shared logic (blocks, entities, VM, networking, bus)
  data/      - Data generators (blockstates, recipes, loot tables)
  jcodec/    - Bundled H.264 library (do not touch)
  platform/  - NeoForge implementation of the core/platform bridge
```

See [docs/SRC_STRUCTURE.md](docs/SRC_STRUCTURE.md) for a full breakdown, and [docs/MULTILOADER.md](docs/MULTILOADER.md) for why `core` exists.

## PR Process

1. Fork the repository
2. Create a branch from `work`
3. Make changes with meaningful commits
4. Ensure `./gradlew build` passes
5. Open a Pull Request targeting `work`

## Commit Messages

Use conventional commits:

```
feat: add redstone controller C API
fix: correct monitor framebuffer overflow
refactor: extract ComputerTerminalManager from ComputerBlockEntity
docs: update networking docs
test: add Ipv4Space extended tests
```

## AI Tool Disclosure

If you used an AI tool (Claude, ChatGPT, Copilot, Cursor, etc.) to write or review any part of your contribution, say so in the PR description. Add a short section like:

```
## AI tools used
- Claude: drafted the initial NetworkResolver implementation
- Copilot: autocomplete throughout
```

This is not a reason to reject a PR - it just needs review with that in mind, the same way any external code needs a source and a reviewer who checked it.

## Before Submitting

- `./gradlew build` passes
- `//` comments only where they explain complex logic or important modules
- Public API has JavaDoc
- ≤200 lines per file, ≤4 files per folder
- No SPDX headers
- AI tool usage disclosed in the PR description, if any was used

## Committed Binaries

Two kinds of binary files are intentionally committed to this repository, not generated at build time:

- `src/main/resources/natives/` - prebuilt `liboc2rnet`/`oc2rnet` native libraries for android/linux/macos/windows (arm64 + x86_64). These are committed so the mod works fully offline without a native toolchain or network fetch during build; see `src/main/resources/natives/.version` for the built revision.
- `gradle/wrapper/gradle-wrapper.jar` - the standard Gradle Wrapper jar, committed per [Gradle's own recommendation](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:adding_wrapper) so `./gradlew` works on a fresh checkout without a pre-installed Gradle.

Both are expected to trip generic "no binaries in git" style lint rules; that is a known, accepted tradeoff, not an oversight.

## Issues

Use the provided templates in `.github/ISSUE_TEMPLATE/`:
- [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md)
- [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md)
