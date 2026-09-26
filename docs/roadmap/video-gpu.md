# Roadmap: video, monitors, GPU

Monitors, projector, GPU items and the video codec. In-game verification boxes of sections 12, 23 and 26a are consolidated in
[qa-checklist.md](qa-checklist.md). Projector rendering ideas (section 7) live in [icebox.md](icebox.md#7-projector-rendering-improvements).
Section 18 (raw RGB buffer, obsolete) is in [done.md](done.md#18-remove-jcodec-raw-rgb-buffer-obsolete).

## 12. Multi-monitor: fragment model

Goal: replace the OBJ monitor model with fragment JSON models + a custom BakedModel. Code is ready: 48 fragment textures, config
`monitorMaxWidth/Height`, ModelProperty, `MonitorBakedModel`, `MonitorMerge`/`MonitorBreak`/`MonitorRepartition` (assemble a full rectangle
in any order, breaking one block re-partitions).

- [ ] Build + in-game check: see [qa-checklist.md](qa-checklist.md#from-section-12).

## 23. GPU as an item, tiers 1 to 4

Done: `GPUItem`/`GPUDevice`/`GPUItemDeviceProvider` (4 tiers, slot, crafts, models, lang), config `gpuEnergyPerTickTier1..4`. The framebuffer
provider is already size-independent (`fb.getWidth()/getHeight()`, stride = width x 2).

| Tier | Resolution | Text mode | Description |
|---|---|---|---|
| GPU T1 | 320x200 | 80x25 | Basic, iron/redstone craft |
| GPU T2 | 640x400 | 160x50 | Medium, gold/lapis |
| GPU T3 | 1024x768 | 256x96 | Advanced, diamonds |
| GPU T4 | 1920x1080 | 320x135 | Endgame, netherite/emeralds |

- [x] Monitor integration: `MonitorDevice` asks the bus controller for a GPU; without one the framebuffer is not mounted (black screen); with one,
  `SimpleFramebufferDevice(width, height)` from the GPU (`MonitorGpuLink` + afterDeviceScan listener in `ComputerBlockEntity`; the blob is recreated on resolution change).
- [x] Hard-coded 640x480 removed: server (`MonitorVideoController`) and client (`RenderInfo` recreates the DynamicTexture by frame size,
  `MonitorTextRenderer`/`MonitorDisplayWidget` use the last frame resolution).
- [x] Without a GPU the UART terminal still works.
- [ ] Build + in-game check (monitor with GPU T1..T4, black without GPU): see [qa-checklist.md](qa-checklist.md#from-section-23).

The former "merge blocker" banner is dropped: the base branch is 1.21.1 now; only in-game verification remains.

## 26a. Video codec: switchable RAW and H.264

(Original number 26; the second section 26 is now [26b](devices-storage.md#26b-onyxos-in-oc2r-s-mode-boot-onyxfs-disk-network).)

Config `videoCodec` in `GameplaySpec` (the default is now `delta`, see section 40). Vendored jcodec was restored (`li.cil.oc2.jcodec.*`, "Vendored from
JCodec 0.2.5", lint excludes back, reference in `ref/jcodec/`). `FrameCodec` (`common/vm/video/`): stateful encoder/decoder, YUV420 <-> RGB565,
deflate/inflate; hand-written StreamCodec (8 components); Reassembler by explicit frameSize; fallback to RAW on decoder BufferOverflow. Tests:
`FrameCodecTest` + `FrameChunkerTest`.

- [ ] In-game check: see [qa-checklist.md](qa-checklist.md#from-section-26a).

### 38 video items

Parent section: [38 in network-inet.md](network-inet.md#38-performance-audit-network-monitors-bus). Closed items (V1 to V3 and others) are in
[done.md](done.md#38-closed-items). Note: `videoCodec` default is already `delta`, and the DELTA codec makes V4, V5 and V7 moot.

- [x] **V4: RAW mode = 600 KB/frame x 20 fps = about 12 MB/s per watcher**: moot, solved by DELTA as the default.
- [x] **V5: RAW fallback inside an H264 stream breaks the decoder until IDR**: moot, H264 path is legacy (see K5 below for the DELTA equivalent).
- [x] **V7: QP 12 vs 24**: moot, H264 path is legacy.
- [ ] **V6: dirty lines are ignored** (low priority): `SimpleFramebufferDevice.copyFrame:62-83` copies the whole buffer and clears all dirty bits, so a full encode runs
  even for one changed row. Fix: encode the dirty region.
- [ ] V8 (low priority): client decode on the main thread: `MonitorFramebufferMessage.handleMessage:66` -> inflate + H264 + YUV->RGB + full 1.2 MB texture
  upload (`RenderInfo.java:49-71`) causes render hitches. Fix: decode off the render thread.
- [ ] V9 (low priority): per-frame allocations on the client: new Picture 460 KB, new byte[600 KB], new Deflater/Inflater + ByteArrayOutputStream. Buffer pooling.
- [ ] V10 (low priority): `ProjectorFrameSender` is a verbatim duplicate of `MonitorVideoController`: fixes must be made twice. Extract a common abstraction.

## 40. Removing jcodec: full usage list

The only entry point is `common/vm/video/FrameCodec.java`, which imports exactly 7 jcodec classes (checked by grep over the whole `src`):
`H264Decoder`, `H264Encoder`, `CQPRateControl`, `ColorSpace`, `Picture`, `RgbToYuv420j`, `Yuv420jToRgb` (all under `li.cil.oc2.jcodec`). They transitively pull about
86 files (~16K lines): encoder (MotionEstimator, CABAC/CAVLC, MBWriter*, DeblockingFilter), decoder (SliceReader, BlockInterpolator, MBlockDecoder*), infrastructure
(BitReader/BitWriter, VLC, IntObjectMap, Picture/Size). A full replacement means rewriting the functional contract of those 7 classes, not their protocol.

- [x] **K1: own delta codec DELTA** (2d77dd6): `DeltaFrameCodec`, 32x16 tiles, dirty tracking, per-tile best of RLE/zlib/raw, zlib key frame at start and on resolution
  change; sizes are carried in the payload (stale stream rejected); decoding runs on a copy of the frame (a broken delta block does not corrupt the reference).
- [x] **K2: YUV is not needed on the DELTA path** (2d77dd6): DELTA encodes RGB565 directly; jcodec classes remain only on the legacy H264 path (they vanish with K4).
- [x] **K3: `VideoCodec.DELTA(2)`** in the enum + selection in `FrameCodec` (2d77dd6). Decision: H264 stays a legacy option until K4 (a week of DELTA soak as default:
  `GameplaySpec videoCodec raw -> delta`, done); K4 removes h264 from the enum/config with a WARN migration.
- [ ] **K4: delete `src/main/java/li/cil/oc2/jcodec/`** (-86 files) after a soak of the DELTA default; remove excludes from `build.gradle.kts` (:405, :417), checkstyle/pmd/spotbugs/qodana configs
  and `docs/jcodec-analysis.md` (replace with a note about the DELTA codec).
- [ ] **K5: tests**: extend `FrameCodecTest` for DELTA: static frame (almost empty stream), full noise (worst case <= RAW), bit-exact RGB565 roundtrip, BufferOverflow -> RAW fallback (also covers V5:
  RAW inside a DELTA stream is as unacceptable as in H264).
- [ ] **K6: order of work**: (1) DELTA codec next to `FrameCodec` + tests (done); (2) switch the default `videoCodec` to DELTA (done); (3) a week of soak; (4) delete jcodec + H264 (K4).

DELTA solved four performance findings of section 38 at once (V1, V4, V5, V7).
Related historical detail (library assessment from section 39): recommended own delta codec (option B); optional intermediate option A (patch vendored jcodec, 2 to 4x gain) is not needed.
