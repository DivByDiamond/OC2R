# Roadmap: in-game QA checklist

Everything here is "code ready, verify in game". Each box links back to the section that describes the feature. Untested boxes stay unchecked until checked in a real world (single player and dedicated server where relevant).

### From section 12
Source: [12](video-gpu.md#12-multi-monitor-fragment-model)
- [ ] Build + multi-monitor of different sizes: assemble 2x2/3x3 in any order, break one block and see the re-partition.

### From section 19
Source: [19](terminal.md#19-terminal-diff-instead-of-raw-uart-on-the-client)
- [ ] Computer GUI and robot GUI, two clients near one computer (tracking-chunk broadcast), scrollback with the mouse, alt-screen applications (vim/less), mouse in mc/midnight, bell.
- [ ] Run vttest inside the guest (the server parser was not touched, but confirm after the dirty-routing refactor).

### From section 21
Source: [21](done.md#21-sound-tone-generator-pcm-streaming-speaker-block) (checked in v0.1.0+383c4ec)
- [ ] **Speaker is silent**: `lua -e 'local d=require("devices"); d:find("speaker"):beep(880,500)'`: RPC without errors but no sound. The chain is intact: callback -> `SoundClientMessages.sendBeep` -> `SoundCardBeepMessage` -> `SoundClientManager.playTone` ->
  `ToneSoundInstance` (stream `sound_card_beep`); POST beeps (same sound registration) do play. Check: (1) client log for `Unable to play unknown soundEvent`/exceptions in `enqueueWork`; (2) distance (`Attenuation.LINEAR`, range 16 blocks);
  (3) a silent failure in the `SoundEngine` streaming path (an exception in `CompletableFuture.thenAccept` is not logged): if needed, play without `stream=true` and return sound from `SoundBufferLibrary` (fallback `.ogg`).
  May be the same problem as issue #288 ([upstream.md](upstream.md#413-not-applicable-or-already-closed)).
- [ ] `d:find("sound")` returned nil: this was a consequence of the computer being off (energy shortage, config 256/t). After the fix (transfer 1024/t, computer buffer 8000) check again.
- [ ] Check `write` (PCM streaming) once the tone is fixed.

### From section 22
Source: [22](devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code)
- [ ] Build + check: hot-swap, data persistence, orphan cleanup.

### From section 23
Source: [23](video-gpu.md#23-gpu-as-an-item-tiers-1-to-4)
- [ ] Build + monitor with GPU T1/T2/T3/T4; without a GPU the monitor stays black (UART terminal works).

### From section 24
Source: [24](devices-storage.md#24-cpu-frequency-config-new-tiers-governors)
- [ ] Build + different CPUs, energy/performance balance.

### From section 26a
Source: [26a](video-gpu.md#26a-video-codec-switchable-raw-and-h264)
- [ ] Monitor + projector with `videoCodec=raw` and `videoCodec=h264` (and the default `delta`); measure traffic (h264 must be several times smaller).

### From section 34
Source: [34](cables-bus-energy.md#34-manual-in-game-testing-wires-and-screens)
- [ ] **Cable/energy**: a chain of 3+ cables to a generator and a consumer: energy must reach the last one (ping-pong fix); world restart must not lose network energy; IC2 EU bridge (if present).
- [ ] **Switch (NetworkSwitch)**: chunk load with a switch must not crash the server (empty `adj` list fix); several clients through a switch: frames flow, no empty-frame spam.
- [ ] **NetworkConnector**: connection through connectors and disconnect on break (empty-frame fix `frame.length > 0`).
- [ ] **PCI Card Cage**: installing a card starts energy consumption, `has_energy` in the UI updates on the server.
- [ ] **Bundled Redstone**: write/read a bundled signal on the same face (`.getOpposite()` fix). The "check all 4 horizontal faces at FACING != NORTH" line is closed: side index bug is fixed (ec49a41).
- [ ] **Monitor**: frame rendering must not chew CPU without changes (`dirtyLines.clear()` fix); terminal text renders correctly; monitor multiblock (merge/split/break) works.
- [ ] **Speaker**: the new Charger-style texture/model shows correctly from all sides.
