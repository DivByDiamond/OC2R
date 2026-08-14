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
src/main/java/li/cil/oc2/
  api/       — Public API (do not change without discussion!)
  client/    — Client-side only (rendering, GUI)
  common/    — Shared logic (blocks, entities, VM, networking, bus)
  data/      — Data generators (blockstates, recipes, loot tables)
  jcodec/    — Bundled H.264 library (do not touch)
```

See [docs/SRC_STRUCTURE.md](docs/SRC_STRUCTURE.md) for a full breakdown.

## PR Process

1. Fork the repository
2. Create a branch from `1.21.1`
3. Make changes with meaningful commits
4. Ensure `./gradlew build` passes
5. Open a Pull Request targeting `1.21.1`

## Commit Messages

Use conventional commits:

```
feat: add redstone controller C API
fix: correct monitor framebuffer overflow
refactor: extract ComputerTerminalManager from ComputerBlockEntity
docs: update networking docs
test: add Ipv4Space extended tests
```

## Before Submitting

- `./gradlew build` passes
- `//` comments only where they explain complex logic or important modules
- Public API has JavaDoc
- ≤200 lines per file, ≤4 files per folder
- No SPDX headers

## Issues

Use the provided templates in `.github/ISSUE_TEMPLATE/`:
- [Bug Report](.github/ISSUE_TEMPLATE/bug_report.md)
- [Feature Request](.github/ISSUE_TEMPLATE/feature_request.md)
