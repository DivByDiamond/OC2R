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
