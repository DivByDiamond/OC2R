# Roadmap: in-game QA checklist

Everything here is "code ready, verify in game". Each box links back to the section that describes the feature. Untested boxes stay unchecked until checked in a real world (single player and dedicated server where relevant).

### From section 12
Source: [12](video-gpu.md#12-multi-monitor-fragment-model)
- [x] Multi-monitor on 26.x (2026-10-20, [stage3-acceptance.md](stage3-acceptance.md)): single and 3x2 merged screens, all four facings, distinct frames per screen; re-partition of cached devices after a merge is covered by `DeviceBusTests.monitorMergeReplacesCachedDevices`. Not yet checked live: 2x2/3x3 in any order with a block broken afterwards, and NeoForge/Fabric 1.21.1.

### From section 19
Source: [19](terminal.md#19-terminal-diff-instead-of-raw-uart-on-the-client)
- [ ] (computer/robot GUI scrolling and GUI scaling verified on both 26.x versions, see stage3-acceptance.md; still open: two clients, vim/less, mouse in mc, bell) Computer GUI and robot GUI, two clients near one computer (tracking-chunk broadcast), scrollback with the mouse, alt-screen applications (vim/less), mouse in mc/midnight, bell.
- [ ] Run vttest inside the guest (the server parser was not touched, but confirm after the dirty-routing refactor).

### From section 21
Source: [21](done.md#21-sound-tone-generator-pcm-streaming-speaker-block) (checked in v0.1.0+383c4ec)
- [x] (2026-10-16: a real guest RPC beep produced 880 Hz for 500 ms on 26.1.2, see todo.md) **Speaker is silent**: `lua -e 'local d=require("devices"); d:find("speaker"):beep(880,500)'`: RPC without errors but no sound. The chain is intact: callback -> `SoundClientMessages.sendBeep` -> `SoundCardBeepMessage` -> `SoundClientManager.playTone` ->
  `ToneSoundInstance` (stream `sound_card_beep`); POST beeps (same sound registration) do play. Check: (1) client log for `Unable to play unknown soundEvent`/exceptions in `enqueueWork`; (2) distance (`Attenuation.LINEAR`, range 16 blocks);
  (3) a silent failure in the `SoundEngine` streaming path (an exception in `CompletableFuture.thenAccept` is not logged): if needed, play without `stream=true` and return sound from `SoundBufferLibrary` (fallback `.ogg`).
- [x] (the speaker device is found and beeps in the 2026-10-16 run) `d:find("sound")` returned nil: this was a consequence of the computer being off (energy shortage, config 256/t). After the fix (transfer 1024/t, computer buffer 8000) check again.
- [x] Check `write` (PCM streaming) once the tone is fixed (2026-10-16: all 44 packets played, 0.51042 s captured against 0.51084 s supplied).

### From section 22
Source: [22](devices-storage.md#22-disks-cleanup-tiers-3d-models-dead-code)
- [ ] Build + check: hot-swap, data persistence, orphan cleanup.

### From section 23
Source: [23](video-gpu.md#23-gpu-as-an-item-tiers-1-to-4)
- [x] Build + monitor with GPU T1/T2/T3/T4; without a GPU the monitor stays black (UART terminal works). (2026-11-12, `HardwareTierTests` on a real server: each tier mounts a framebuffer of 320x200 / 640x400 / 1024x768 / 1920x1080 on the monitor, and a monitor without a GPU gets none while the guest keeps running. Not checked: how the larger sizes look and perform on a real client.)

### From section 24
Source: [24](devices-storage.md#24-cpu-frequency-config-new-tiers-governors)
- [x] (2026-11-12, `HardwareTierTests`) Build + different CPUs: the guest boots on tiers 1 to 4 and its clock equals the item's frequency. Still open: energy/performance balance.

### From section 26a
Source: [26a](video-gpu.md#26a-video-codec-switchable-raw-and-h264)
- [ ] Monitor + projector with `videoCodec=raw` and `videoCodec=h264` (and the default `delta`); measure traffic (h264 must be several times smaller).

### From section 34
Source: [34](cables-bus-energy.md#34-manual-in-game-testing-wires-and-screens)
- [ ] **Cable/energy**: a chain of 3+ cables to a generator and a consumer: energy must reach the last one (ping-pong fix); world restart must not lose network energy; IC2 EU bridge (if present).
- [~] (2026-11-12: the block entity ticks and loads without neighbours, `NetworkSwitchTests`; forwarding cannot be checked because the switch exposes no `NetworkInterface` capability and has no item, so connectors cannot reach it and players cannot get it. Needs an owner decision: enable it or remove it) **Switch (NetworkSwitch)**: chunk load with a switch must not crash the server (empty `adj` list fix); several clients through a switch: frames flow, no empty-frame spam.
- [x] (26.x, 2026-10-20: connect/disconnect on loaded chunks and after reload; both guest NICs worked, TCP echo passed) **NetworkConnector**: connection through connectors and disconnect on break (empty-frame fix `frame.length > 0`).
- [x] (2026-11-12, `PciCardCageTests`) **PCI Card Cage**: a cage on a running computer is powered, runs dry when its supply is removed and recovers when it returns; a cage on a stopped computer draws nothing. The in-GUI indicator was not looked at.
- [~] (2026-11-12: fixed a real bug, see below) **Bundled Redstone**: write/read a bundled signal on the same face (`.getOpposite()` fix). The "check all 4 horizontal faces at FACING != NORTH" line is closed: side index bug is fixed (ec49a41).
- [ ] (merge/split and terminal text verified on 26.x; the idle CPU check is still open) **Monitor**: frame rendering must not chew CPU without changes (`dirtyLines.clear()` fix); terminal text renders correctly; monitor multiblock (merge/split/break) works.
- [ ] **Speaker**: the new Charger-style texture/model shows correctly from all sides.

- [ ] Network wires: check the blue color and the sag on long (up to 16 block) wires in game (see network-overhaul.md)


### Notes from the 2026-11-12 acceptance pass
- **Bundled redstone bug**: the interface stored bundled output by the block-relative side but a ProjectRed cable asks by world direction, so on any interface not facing north a cable read the wrong face. Storage, reads and writes now use the world direction like the plain redstone output does (`BundledRedstoneCallbacksTest`). ProjectRed has no 26.x build and was not available in the test world, so the cable side itself is still unchecked in game.
- **Still open from this list**: disks (hot-swap, persistence, cleanup), codecs (`raw`/`h264`/`delta` traffic), wire sag, vttest in the guest, cable/energy chain, monitor idle CPU. These need long real-client runs or external mods.
