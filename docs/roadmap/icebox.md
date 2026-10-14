# Roadmap: icebox

Vague or blocked backlog items. Nothing here is scheduled.

## 2. Resizable Screen (#12)

Deferred. First refactoring and TCC (both done).

### 3 Buildroot image rebuild

Related to [section 3](devices-storage.md#3-tcc-in-the-image): build a new buildroot image. Needs a toolchain/Docker for minux (outside this repo); instructions: `docs/BUILDROOT.md`. Blocked.

### 6 Terminal block area enlarge

Last item of section 6 ([terminal.md](terminal.md#6-terminal-dynamictexture-rendering)):
- [ ] Enlarge the terminal area on the block from 12x7 to 14x10 or 16x9 px.

### 1 residual

Section 1 (C API for Redstone Interface) has no open items; the only residual idea is to document `librpc` for RISC-V/Rust users (no task yet).

## 7. Projector rendering improvements

Vague ideas, no owner (see also [video-gpu.md](video-gpu.md)):
- [ ] Gamma correction after the YUV->RGB conversion.
- [ ] Try YUV444 instead of YUV420 (less blurred colors).
- [ ] Increase the depth map from 256x256 to 512x512.

## 57. OC2R outside Minecraft

Idea: run the same bundled images (Sedna, device models, terminal) in a browser or a standalone window with simulated game devices, so people can learn the system, write programs and test OS images without a Minecraft install.

How it could work: a small module reusing `core` and the VM code with stub devices (a fake inventory, redstone and monitor) and a web or desktop frontend. Sedna is plain Java, so a standalone JVM app is the cheaper first step; a browser version needs a Java-to-JS/WASM path and is a separate decision.

Pros: faster iteration for script and OS authors, a public demo, easier bug reproduction, CI can run programs against the stubs.

- [ ] Decide the form (standalone JVM first) and what the stub devices cover.
- [ ] Split the VM/terminal code from Minecraft classes where it still depends on them (see the layering lints in [quality.md](quality.md#9-lint-and-static-analysis)).
