# Roadmap: done

Closed sections and closed parts of open sections, with their original headings. Prose is condensed; hashes, dates and results are unchanged.

## 0. Refactoring: structure, SOLID, KISS, DRY

Rule: at most 200 lines per file, at most 4 files per folder. Fully done (about 50 extractions: Terminal -> buffer/writer/scrolling/shifter/IO/output/render/colors, Network, ComputerBlockEntity, Robot,
MonitorBlockEntity, BusCableBlockEntity and others; all big folders split into subfolders). Details in git history until 2026-08.

- `common/vm/terminal/escapes/csi/` (31 files): leave alone, one file per CSI handler by design.
- `common/blockentity/network/` (25 files) was split into cable/, connector/, switches/, hub/, vxlan/.
- Deferred: replacing jcodec with the Maven dependency `org.jcodec:jcodec` (cancelled, see section 11); `api/`: do not touch.

## 8. Screen and Container auto-registration

- [x] Created `ScreenRegistry` (a DSL over `RegisterMenuScreensEvent`), `Containers.registerScreens` switched to it.

## 11. jcodec to Maven dependency (cancelled)

Cancelled: task 18 (full jcodec removal, raw RGB video pipeline) made the Maven dependency unnecessary; the story continued in [26a](video-gpu.md#26a-video-codec-switchable-raw-and-h264) and [40](video-gpu.md#40-removing-jcodec-full-usage-list).

## 13. Wire rewrite: FE and EU energy, connection fix

A cable is an energy conductor: FE through `IEnergyStorage`, EU (IC2) through `Ic2EuBridge`/`EuEnergyAdapter` (4 FE = 1 EU). Config `cableEnergyCapacity`/`cableEnergyTransferPerTick`. `EnergyTransferManager` distributes energy over the network once per tick
(fixes the "ping-pong"). Creative block = `InfiniteEnergyStorage`. Auto-connect + a single `recomputeConnections()`.

## 14. Computer case rewrite (Block)

The front face is the block direction at placement. RMB on a non-front face opens the component inventory GUI (`ComputerBlockInteraction.useWithoutItem`); RMB on the front opens the terminal; start with Shift+RMB or `PowerButton`; POST beeps on start errors
(`ComputerPost`, 5 signals: firmware/energy/CPU/memory/unknown, hook `ComputerVirtualMachine.handleBootErrorChanged`).

## 17. Automatic VM restart when the device set changes

Soft restart through `stop(); start();`: `AbstractVirtualMachine` has `AtomicBoolean devicesChangedWhileRunning` + `deviceChangeRestartDelay` + `markDevicesChanged()`; `tick()` postpones the restart by 2 ticks (batching serial connections).
`VMLifecycle.handleDevicesAdded/Removed` only add/remove + mark; the `RUNNING -> LOADING_DEVICES` transition was removed from `handleBeforeDeviceScan`. RPC devices hot-plug through a live adapter without restart.

## 18. Remove jcodec, raw RGB buffer (obsolete)

Historical (2026-09-15): the raw-only approach (jcodec removed entirely) was implemented, but section 26a brought jcodec back (switchable RAW/H.264), and then [section 40](video-gpu.md#40-removing-jcodec-full-usage-list) took a third, current path: an own DELTA codec
(tiles + dirty tracking), whose K1 to K3 are in code (`DeltaFrameCodec.java`). The old open checkbox is obsolete; live work is K4 to K6 in section 40. Original idea: a frame is a ready RGB565 buffer as is, the client renders through a DynamicTexture (NEAREST); jcodec/balancers/worker pools removed.

## 21. Sound: tone generator, PCM streaming, Speaker block

Done: RPC `beep`/`playTone` (client-side sine synthesis, `ToneAudioStream`), PCM streaming `write(byte[])` (ring buffer + `StreamingPcmSoundInstance`, stop on silence), Speaker block (RPC, auto-connect to a cable), POST beeps on start, cooldown config.
The open in-game checks (speaker silent, `find("sound")` nil, `write` check) are in [qa-checklist.md](qa-checklist.md#from-section-21).

## 31. VT100 terminal audit no. 1

Audit 2026-08-18, branch work, HEAD 75c8cc4. All 25 findings closed. First full audit of `common/vm/terminal/**`. All blockers (B1 clearLine reset the color, B2 SU no-op on a fresh terminal, B3 dirty mask mapped wrong rows), major/minor/nit findings and
architectural decisions are closed and covered by tests. Kept as source of truth: `ModeTable` (single source for 74 private + 4 ANSI modes, replaced about 60-case tables in CH1/CH2/CH3/CH6), `CSISequenceHandler.defaultParameters(CSIState)` (per-handler default args instead of one rule in CSIManager;
DECSTBM and XTRESTORE share the final `r`), and the single dirty distribution point `Terminal.markDirty(mask)`/`markAllDirty()` (buffer layer and CSI handlers no longer touch `renderers` directly).

## 46. Terminal sweep follow-ups

Deferred tails of the terminal sweep (chunk 1 to 3, PR #45/#46/#49); all three PRs are merged. Each documented small deferred discrepancies:

- [x] (chunk 1, PR #45) `Utf8Decoder` resync differs from xterm-410 `decodeUtf8` on a lead byte in the middle of a multi-byte sequence. Partly fixed: `processContinuation` now resyncs on a lead byte (`0xC0` mask) instead of stitching low 6 bits into the previous codepoint;
  xterm additionally emits `UCS_REPL` for a broken sequence, ours silently drops (no REPL) until a separate decision.
- [x] (chunk 1, PR #45) Shift-dirty scrollback rows outside the visible window were not sent to the client until the view re-stuck to the bottom. Fixed 2026-09-17 together with chunk 3 (common root): `TerminalDiff.capture` used to drop `dirty.rows()` on full refresh and substitute only
  `visibleWindowRows()`, so `Terminal.markAllBufferRowsDirty()` (only call: `TerminalBuffer.clearScrollback`, ED `3 J`) was lost; now `capture` ships the union of the visible window and explicitly marked rows (`TerminalDiff.fullRefreshRows`). Test `eraseScrollbackFullRefreshShipsOffScreenRowsNotJustVisibleWindow`.
- [x] (chunk 2, PR #46) XTRESTORE restored the DECCOLM flag but not the resize itself. Fixed: `XTRESTORE.execute` now calls `resetRendition()` + `setWidth()` on DECCOLM restore like CH2/CH3; test `xtrestoreDeccolmAlsoRestoresColumnWidth`.
- [x] (chunk 3, PR #49) More than 32 shift operations in one diff window dropped the backlog and forced a full refresh, leaving scrollback above the visible window out of sync. Fixed 2026-09-17: "scrollback together with full refresh":
  the overflow branch of `Terminal.recordNetworkShift` now marks ALL buffer rows dirty (`networkDirtyRows.set(0, height * SCROLL_BACK_COUNT)`), not only the `fullRefresh` flag; together with the `TerminalDiff.capture` fix this self-heals in one full-refresh diff. Test
  `shiftBacklogOverflowSelfHealsFullScrollbackOnNextCapture`. Trade-off: that diff is once large (up to `height * SCROLL_BACK_COUNT` rows), but the trigger is rare.
- Accepted residue (not a blocker, a known render inaccuracy): possible cell content tear between frames and in-place palette mutation (also tracked as [P1 in terminal.md](terminal.md#47-terminal-items)).

## Closed parts of open sections

### 29 closed items

Section 29 (world sync fixes), audit result: 3 real bugs + many duplicates. Closed bugs:
- [x] `MultipartMessage` cache key bug (`MultipartMessage.java:133,144,149`): on the server the static `lastAssignedMultipartMessageId` was used instead of the record field `multipartMessageId`, so on a dedicated server all file imports of all clients were written into one buffer (key 0) and mixed up. Fixed in `d59ab0a`
  (field `multipartMessageId` + key `(connection, multipartMessageId)`); S->C got `sendToClient` and `ExportedFileMessage` is registered multipart.
- [x] `ServerCanceledImportFileMessage` cast on the wrong side (`:33`): registered `playToClient` but the handler cast `(ServerPlayer) context.player()` on the client (ClassCastException on every import). Fixed in `d59ab0a` (handled via `Minecraft.getInstance()` on the client).
- [x] `MonitorStateManager` save/load were mixed up (`:54-67`): `savePersistent` wrote `isPowered` under key `projecting` while `loadPersistent` read `hasEnergy` from `has_energy` (never written); `isMounted` not saved. Now `hasEnergy`/`isPowered` are written and read consistently; `isMounted` is runtime state and not persisted.
- [x] `InternetGateWayBlockEntity.notifyPlayers`: only `sendBlockUpdated(2)` without BE data, so `inbound/outboundCount` (animation) did not update on the client; now `ClientboundBlockEntityDataPacket.create(this)` is sent to players tracking the chunk.
- [x] `ExportedFileMessage` was sent as one payload up to `1MB-1`, at the NeoForge limit (1 MB): big exports could silently fail; `MultipartMessage.sendToClient` + registration.
- [x] Duplication groups closed by section 38 (2026-08-26): facade x3 -> x1, connectors x2 -> x1, interface names tag-only, floppy/flash x2 -> x1, `chunk==null` (see [38 closed items](#38-closed-items)).

### 32 closed items

- [x] **BundledRedstone get/set on different faces** (`blockentity/misc/redstone/BundledRedstoneCallbacks.java:18,24,29,43`): `getBundledOutput` read index `side.getDirection()` while `setBundledOutput`/`setBundledOutputs`/`getBundledInput` used `side.getDirection().getOpposite()`, so a written bundled signal went out on the opposite face.
  Fix: `.getOpposite()` removed (set/get/input symmetric; the world reader `getBundledSignal` reads the `worldDir` index directly).
- [x] **Redstone Interface side index** (`setRedstoneOutput`/`getRedstoneOutput` used the world index while the world reader converts world->local via `HorizontalBlockUtils.toLocal`): fixed in commit `ec49a41` ("redstone FACING rotation"), see [41.2 fixed items](#412-fixed-items). The note and the "check all 4 faces" line of section 34 are closed.
- [x] PMD warning zeroing (423 pre-existing): PMD zeroed (423 -> 0), see [section 33](quality.md#33-code-documentation-and-nopmd-review) for the NOPMD follow-up.

### 36 closed items

Second VT100 audit (2026-08-23, branch 1.21.1; 74 files; findings verified by reading sources; blocker B1 confirmed by manual trace). Open items: [terminal.md](terminal.md#36-vt100-terminal-audit-no-2).
- [x] Blockers B1 (AIOOBE SD/RI on a full scrollback) and B2 (freeze SU/SD MAX) closed; regression tests `CSI 2 T`, `999999999S`.
- [x] Major M1 G1 designate, M2 truncated true-color, M3 dirtyLine overflow, M4 render race, M5 `@OnlyIn`, M6 `SCROLL_BACK_COUNT` final: closed (PR #24, #28, #30, #34, #36).
- [x] m1: DECRC/restoreSavedCursor did not clamp coordinates after a width change: closed by PR #24 (unify cursor save/restore into `SavedCursor`, 2026-08-25): restore goes through `setCursorPos` (clamp), repro test `decrcClampsSavedCursorAfterWidthShrink`.
- [x] m3: `CSI 3 J` (erase scrollback, xterm E3) was silently ignored (`escapes/csi/ED.java:26-39`): `ED:39` -> `bufferManager.clearScrollback()` (copy visible window to the start, blank tail, `lastRowToDisplay(Max)=height`, `markAllBufferRowsDirty`).
- [x] m4: `CSI n` without a parameter did not reply: closed by PR #28 (2026-08-25): `DSR.defaultParameters()` returns `{5}`, bare `CSI n` resolves to `Ps=5` -> `\033[0n` per ECMA-48.
- [x] m7: xterm-256 palette off canon: closed by PR #30 (2026-08-25): `0xdf` was a typo for `0xd7` at the 4th cube level; a full canonical xterm-256 palette test was added (942e1f9).
- [x] m8: `TerminalUtils.resetTerminal` static mutable ByteBuffer + bare 'J': rewritten by task 19 (2026-08-23): RIS + full snapshot.
- [x] m9: DCL without volatile in `Terminal.client()`: fixed with `private transient volatile TerminalClient clientInstance;`.
- [x] m11: dead code mostly closed by PR #36 (2026-08-25, refactor/terminal-dead-code-cleanup): removed `Utf8Decoder.hasActiveSequence()`, `TerminalIO.putOutput(byte)`, `TerminalBuffer.shiftUp/shiftDown(int)`, `SessionOperator`/`ColorUtils`/`RunnableUtils`, unused `Glyph` fields;
  `TerminalRenderer.findLineIndex` / `TerminalCharRenderer.isPrintableCharacter`/`renderForegroundChar` / `TerminalBufferWriter.setChar` made private; `ImplementedPrivateModes.modeStatus` -> `private static final`, `instance` -> `public static final`.
- [x] Nits: HT inside CSI now honors `tabs[]` (`CSIManager:105`, like `TerminalOutput.handleTab`); `ESC # 8` (DECALN) resets margins and homes (`TerminalOutput.handleHash:344`); DL pre-clear removed (`DL:26`); RIS resets transient `hasPendingBell` (`RIS.java:32`).
- [x] OSC/DCS/APC managers (ST/BEL termination): closed by PR #35 (2026-08-25, `StringSequenceTest.java`, 17 tests: ST/BEL termination, CAN/SUB abort, ESC+non-`\` abort-and-redispatch, double ESC, nested string start).
- Refuted hypotheses (verified correct, do not fix): stale args between CSI (reset on `[` and CAN/SUB); null read of ColorData before SGR (RIS in constructor); OOB in clearChars/deleteChars/insertChars (clamp proven); `Math.clamp` min>max (guard `setWidth`); DECSTBM/CUP/HVP/DECOM; SGR 38/48 consumption on valid input;
  `shiftLines` for count>1 from IL/DL (clamped to the region); dirty-mask write/read formulas are mutually inverse; input queue (single lock); `displayOnly` respected by all reply producers; IRM `?4h` implemented; RIS complete over serializable fields.

### 37 closed items

Comprehensive audit (2026-08-25, branch master, HEAD 2a6b185). Open items: [quality.md](quality.md#37-comprehensive-audit).
- [x] Blockers: B1 MessageUtils distance, B2 file injection, B3 TcpHeader loop, B4 FrameChunker, B5 IntegerSpace: DONE (f5ccb2d, 6df1e09, abd739e, bd9711d, 082bc4c). B6 DECRC: see 36 m1.
- [x] `TerminalDiff.apply`: `rows.length == rowData.length` was never checked (`vm/terminal/TerminalDiff.java:205-207,313`) -> AIOOBE/client disconnect; width clamp. Done: `readSnapshot` clamps a negative `rowCount` and `boundedCount`; `apply:418` uses a `min(rows.length, rowData.length)` overlap, width clamped by `resizeWidth`.
- [x] CUD/CUF int overflow at argument MAX_VALUE (`csi/CUD.java:17`, `CUF.java:17`): `moveCursorBy` clamps through `Math.clamp(dx, -width, width)`; tests `cudMovesCursorDownAndClampsSaturatedCount` etc.
- [x] Duplicate `RegistryUtils`: `common/util/item/RegistryUtils.java` removed, `Main.java:26` switched to `common.util.RegistryUtils`.
- [x] `System.out` in production (about 14 places: ConfigManager.java:21, VxlanBlockEntity.java:102, SwitchLog.java:28-51, TerminalMouseHandler.java:82,146, ByteBufferFlashStorageDevice.java:111, PciRootPortDevice.java:54-78) -> Log4j2. Closed by stage A (00a7aa7): 13x System.out replaced, `grep -r System.out src/main` is empty.

### 38 closed items

Performance audit (2026-08-25). Symptom: "Network performance is really bad still"; 8 bottlenecks. Open items: [network-inet.md](network-inet.md#38-performance-audit-network-monitors-bus) and [video-gpu.md](video-gpu.md#38-video-items).

Internet card:
- [x] **P1: one frame per tick in each direction** (2280692): PendingFrame -> `ArrayBlockingQueue(64)` both ways, drain loops in `process()` and `processInternetAdapter`.
- [x] **P2: PendingFrame held ONE frame** (2280692): class removed, silent loss gone; test `InternetConnectionImplTest` (drain without loss, stop on a full queue).
- [x] **P3 (partly): buffer >= 32K** (61fd98d): `streamBufferSize` default 2000 -> 32768 (`InternetCardSpec.java` + `Config.java`).
- [x] **P3.1: TCP sliding window** (closed 80a66b1, confirmed by verification 2026-08-26): window with several segments in flight + cumulative ACK; tests `EstablishedStateTest`.
- [x] **P4 (partly): read/write until EAGAIN** (91a4f07): `channel.read` loop until EAGAIN/EOF/full buffer in `readSession`; write flushes `sendBuffer` to the end in `sendStream`. `processQueue` early exit intentionally kept: the Receiver carries exactly one session/buffer per `receiveSession` call, a frame is one segment.
- [x] **P5: OP_WRITE accumulates, `toWrite` never consumed** (closed 797dbc1): OP_WRITE is not registered (`SocketManager.java:105-113`), the queue removed.
- [x] **P6: ping-pong server <-> Internet thread** (2026-08-26): `InternetManagerImpl.onTick` exits without an executor submit when `connections && tasks` are empty. "At least 2 ticks per frame" is by design.
- [x] **P7: per-packet/per-frame allocations**: `InternetConnectionImpl.java:101` -> frame pool `framePool` (obtain/recycle) + contract "the frame is handed to the adapter as borrowed" (gateway keeps the only reference: defensive `frame.clone()`); `VirtIONetworkDevice` copies bytes into guest memory inside `writeEthernetFrame` (checked with javap on sedna 2.0.13).
  `SendHandler` discriminators intentionally left (map keys, about 24 B in a TLAB).

Monitor/video:
- [x] **V1: deflate (BEST_COMPRESSION=9) over already compressed H264** (da56987): deflate/inflate removed, H264 payload is raw Annex-B; start-code guard keeps the contract "garbage -> empty" (test `h264PayloadIsRawAnnexBNotZlib`). Partly removed V9 (per-frame Deflater/Inflater/BAOS).
- [x] **V2: whole encode path on the server thread** (2026-08-26): new `common/vm/video/AsyncVideoEncoder` shared by monitor and projector, one shared daemon worker, inbox of capacity 1 with eviction (last-frame-wins), outbox(8), buffer pool with exact length matching. The server tick only does `copyFrame` + `offer`; ready frames go via
  `flush()` every tick outside throttle/dirty gates (`MonitorTickHandler.tick`, `ProjectorBlockEntity.serverTick`). Buffer ownership is exclusive (recycle strictly after slicing, `FrameChunker.slice` copies). `FrameCodec` stays non-thread-safe by design (thread confinement via the worker). Tests `AsyncVideoEncoderTest` (4).
- [x] **V3: 4 MB direct buffer + encoder per BlockEntity** (ed9e195): lazy allocation of encoder/decoder/buffer/picture on first real use; sharing one encoder between active BEs rejected (alternating would force IDR every frame).
- [x] V4, V5, V7: moot, solved by DELTA (see [40](video-gpu.md#40-removing-jcodec-full-usage-list)).

World sync / traffic:
- [x] **Sync duplicates from section 29 closed (2026-08-26), three groups**: (1) facade x3 -> x1 + `handleUpdateTag` fix: `BusCableBlockEntity.handleUpdateTag` gets `requestModelDataUpdate()`, then explicit `sendBlockUpdated(UPDATE_ALL)` and `BusCableFacadeMessage` were removed from `FacadeManager.setFacade/removeFacade`;
  (2) connectors x2 -> x1: `NetworkConnectorConnectionsMessage` removed, `onConnectedPositionsChanged` -> `sendBlockUpdated(UPDATE_CLIENTS)`, `handleUpdateTag` of the connector calls `NetworkCableRenderer.invalidateConnections()`; (3) interface names tag-only S->C (`BusInterfaceNameMessage` remains only as C->S input),
  floppy/flash x2 -> x1 (`DiskDriveFloppyMessage`/`FirmwareFlasherMessage` removed). `chunk==null`: `ComputerTerminalManager.sendToClientsTrackingComputer` resolves the chunk lazily before the first serverTick, so run/boot-error messages are no longer lost.
- [x] **T1: TerminalDiff cell packing** (already implemented, marked at verification): varint codepoint + attr byte with optional color/style fields + RLE runs of identical cells; a single echo is about a few bytes per row instead of a fixed 37 B per cell (javadoc `TerminalDiff`, `TerminalDiffTest`).
- MultipartMessage: key fixed; `MAX_PAYLOAD_SIZE=8 KB` at the NeoForge limit of 1 MB: a 512 KB import = 64 packets; header overhead < 1% (ok). `enqueueWork` in SoundCardBeep/Pcm is redundant but not a bug.

VXLAN / bus / energy:
- [x] **Sh1: bus `scan()` every tick** (53b50d2, partly obsolete): an event-driven scan with dirty flag `scheduleBusScan` + O(1) early exit already existed; done: reuse of BFS/diff collections in `BusElementManager`, one pass instead of two diff HashSets. A full push-based graph (old section 20) is not required.
- [x] **Sh2: energy network BFS flood-fill + O(n^2) redistribute each tick** (d553911): `EnergyNetworkCache` (per-level/per-origin, invalidation on config change/load/unload of a cable + `isRemoved` validation), redistribute once per 20 ticks; pull/push every tick.
- [x] **Sh3: blocking UDP send** (5c11f5d, confirmed 2026-08-26): `DatagramChannel.configureBlocking(false)` (`TunnelManager.java:123`), non-blocking send, receive on a daemon thread with a selector.
- [x] **Sh4: `System.out.printf` every tick**: replaced by LOGGER; the residual per-tick "unregistered upstream" warn (`VxlanBlockEntity.java:142`) switched to report-once (`warnedUnregisteredUpstream`, reset in `loadServer`).
- [x] **Sh5: extra VXLAN locks** (5c11f5d): ReentrantLock removed, `ConcurrentHashMap` + thread-safe offer (`TunnelManager.java:193`).
- [x] **Sh6: silent packet loss** (5c11f5d): drop statistics `TunnelInterface.droppedFrames` (:194-196, :281) + WARN once per tick (`VxlanBlockEntity.java:133-140`) + config `vxlanPacketQueueCapacity`.
- [x] **Sh7: per-send allocations in `TunnelManager.sendToOuternet`** (2026-08-26): `InetSocketAddress` cached in the constructor (`cachedRemoteAddress`), grow-only `sendBuffer`, `ByteBuffer.wrap(buffer, 0, total)`.
- [x] **Sh8: `SessionManager` per-packet** (2026-08-26): coarse shared clock (cached Instant refreshed at most every 200 us via nanoTime), expiration/retransmission scans iterate `Map.Entry` with `iterator.remove()`. `updateSession` remove+put kept (`lastUpdateTime` is an `Instant` from `api/`); collision enqueue loop kept (rare).
- ARP/ICMP/checksum/address utilities are NOT hot (ARP cache of 1 entry acceptable; ICMP single slot only on the error path).

### 39 closed items

Security hardening (2026-08-25). Open items: [network-inet.md](network-inet.md#39-security-hardening-of-inet-linter-configs-libraries) and [quality.md](quality.md#39-lint-stages).
- [x] **C5: PCM flood** (2026-08-26): `SoundCardItemDevice.write` token bucket `soundCardPcmBytesPerSecond` (`GameplaySpec`, default 128 KiB/s, 4 KiB to 16 MB/s), continuous refill by nanoTime, excess -> `IllegalArgumentException` to the guest; the bucket starts full.
- [x] **C6: rate limit on C2S messages** (2026-08-26): new `common/network/util/PlayerRateLimits` (per-player throttle + event window, WeakHashMap, thread-safe) + test `PlayerRateLimitsTest` (4, caught an off-by-one of the first window event); `MonitorRequestFramebufferMessage`/`ProjectorRequestFramebufferMessage` throttled to 250 ms per player;
  `KeyboardInputMessage` 64 events/s per player.
- [x] **C7: ICMP echo blocked the internet thread** (2026-08-26): the native branch `EchoHandler.handleEchoSession` moved to the "internet/blocking-session" executor like the fallback; reply delivered asynchronously. Also fixed: `size = data.remaining()` was computed AFTER `data.get(payload)` (always 0), so native `sendICMP` was called with an empty payload.
- [x] **C8: internet card NBT MAC/IP restored as is** (2026-08-26): per-card UUID (`DeviceId` in adapter-state, generated on first use in `InternetCardDevice`, survives unmounting) + `MacAddressUtils.macFromUuid` (SplitMix64 finalizer); `DefaultLinkLocalLayer.loadMacAddress` DERIVES the MAC from the UUID and ignores the player-writable `MACAddress` field
  (legacy path kept for pre-update cards, their MAC changes once). Tests `MacAddressUtilsTest` +2. The IP stays guest-assigned (point-to-point); source spoofing is covered by the ARP-claim source filter (see C3 in network-inet.md, needs confirmation).
- [x] **C9: TunnelManager reliability**: bind-fail guard and shutdown were already done in 672e7a3/5c11f5d; here: `DEFAULT_VXLAN_HOST "::1"` (IPv6 loopback, silently broken on IPv4 servers) -> separate defaults `bindHost="0.0.0.0"`, `remoteHost="127.0.0.1"` (Config + VXLANSpec); null-check `TunnelManager.instance()` in `VxlanBlockEntity.onUnload/loadServer`; selector -> volatile.
- [x] **Stage A** (00a7aa7): 13x `System.out` -> Log4j2 (by codebase convention, not SLF4J), `ByteBufferFlashStorageDevice` logs a full stack trace again, PMD excludes AvoidPrintStackTrace/SystemPrintln removed (0 violations), suppressions of MissingSwitchDefault/FallThrough lifted (1 violation in `TerminalOutput` fixed with a default branch), guards `RegexpSinglelineJava` id=SystemOut/PrintStackTrace added to TreeWalker.

### 41.2 fixed items

- [x] **Redstone side mixed-up (issue #164)**: fixed (`ec49a41`, 2026-09-07): `getRedstoneOutput`/`setRedstoneOutput` in `RedstoneInterfaceCardItemDevice` and `RedstoneInterfaceBlockEntity` use `HorizontalBlockUtils.toGlobal()` like `getRedstoneInput`, so outputs rotate with the block FACING.
- [x] **Network connector on fences (issue #225)**: fixed (2026-09-13): `NetworkConnectorBlock.canSurvive()` additionally allows attachment when the block on the attached side is tagged `BlockTags.FENCES`; `getStateForPlacement` untouched (it already calls `canSurvive` through the superclass).

### 43 implemented device-bus part

Implemented on branch `feat/multiloader-stage1` (device-bus lane): in `core`, `NetworkResolver` (a fresh BFS without stored state, a limit + `overflow`, `incomplete`) and `OwnerResolver` (owner = minimum key, computed, not stored) with tests; `BusElementManager` uses the resolver, the limit is now `maxBusElements`
(default 1024, config) and elements above the limit are excluded instead of `TOO_COMPLEX`; `MULTIPLE_CONTROLLERS` is no longer set: several computers share a bus, devices go to the owner (`DeviceBusController.getOwnershipKey`; the root element of a computer always has its own controller),
`CommonDeviceBusController.getOccupiedDevices()` gives "occupied by whom"; per-face `FaceOverride` (AUTO/FORCED_ON/FORCED_OFF) is stored in `BusCableBlockEntity`, FORCED_OFF is honored in traversal and device lookup; GameTest `twoComputersShareOneBus`. `TOO_COMPLEX`/`MULTIPLE_CONTROLLERS` remain in the enum for old saves/packets.
Open remainder: [network-overhaul.md](network-overhaul.md#43-cable-system-rebuild-from-scratch).

### 45 note

Section 45 (GameTest CI baseline) is done; its heading and the one residual item live in [multiloader.md](multiloader.md#45-gametest-ci-a-real-baseline).

### 47 closed items

Audit 2026-09-17. Open items: [quality.md](quality.md#47-audit-2026-09-17).
- [x] **B1: DeltaFrameCodec `tilesX==0` -> ArithmeticException on the client net thread** (`common/vm/video/DeltaFrameCodec.java:248`): guard `if(tilesX==0) return Optional.empty()` in `applyOneTile` + `long frameBytes=(long)width*height*2` with an overflow check and a `<=32MiB` cap in `encode/decode` (also L3).
- [x] **B2: `InternetManagerImpl.tasks` LinkedList cross-thread without sync** (`common/inet/internet/InternetManagerImpl.java:44-45,83,144`): `ConcurrentLinkedQueue<TaskImpl>` or `synchronized(tasks)`.
- [x] **B3: `RPCDeviceBusAdapter` handoff without volatile** (`common/bus/adapter/RPCDeviceBusAdapter.java:49,80,101-129` + `MethodInvoker.java:64`): `volatile` (+ `isPaused` volatile).
- [x] **B4: `Ipv4Space`/`IntegerSpace` signed TreeMap -> bypass of deniedHosts/allowedHosts** (`common/util/misc/IntegerSpace.java:10,77`, `common/inet/util/InetUtils.java:92`, `common/inet/util/Ipv4Space.java:10`): unsigned comparator `Comparator.comparingInt(Integer::compareUnsigned)`, `count():long` via `(long)value - key + 1L`,
  `getSubnetByPrefix` allows 0..32, prefix regex `(?:[0-9]|[12][0-9]|3[0-2])`, `interfaceIdPattern \\d+` (also L1, L2).
- [x] **B5: `FrameChunker.slice` without index validation** (`common/network/util/frame/FrameChunker.java:22`): `if(index<0||index>=chunkCount(frame.length)) throw IAE`.
- [x] **B6: `Terminal.renderers` iteration without synchronized** (`common/vm/terminal/Terminal.java:172,785,787,800`): `synchronized(renderers){ ... }` in `markDirty/markAllDirty` and `getRenderer/releaseRenderer`.
- [x] **B7: `SimpleFramebufferDevice` dirtyLines race VM vs server** (`common/vm/device/SimpleFramebufferDevice.java:13,21,37,62,106,120`): `store`+`setDirty`+`hasChanges` under `lock`.
- [x] **L4: `Terminal.hasPendingBell` without volatile/lock**: `volatile boolean` or under `networkDirtyLock`.
- [x] **L5: `VMRunner` `cycleLimit/cycles/runtimeError` without volatile** (`common/vm/VMRunner.java:46,48,49`): `volatile long cycleLimit/cycles` + `volatile Component runtimeError`.
- [x] **L7: `ServerScheduler` synchronizedMap iteration without a block** (`common/util/scheduler/ServerScheduler.java:181`): `synchronized(levelTickSchedulers){ for(...) }`.
- [x] **L8: `InternetConnectionImpl.isStopped` / `TaskImpl.closed` without volatile** (`InternetConnectionImpl.java:79`, `TaskImpl.java:7`): `volatile`.
- [x] **L9: `TunnelManager.managerInstance` without volatile** (`common/vxlan/TunnelManager.java:61`): `volatile`.
- [x] **L11: RPC JSON deserializers NPE without has/isJsonNull** (`common/bus/adapter/MessageJsonDeserializer.java:16`, `MethodInvocationJsonDeserializer.java:14`): explicit `has("type")` checks; `JsonParseException("missing 'type'")` instead of `writeError(NPE)`.
- [x] **L12: `NetworkMessages` fallback broadcast to all dimensions** (`common/network/NetworkMessages.java:55-62`): filter `player.level()==hostLevel` or like `sendToPlayersTrackingChunk`.
- [x] Style-5: natives binaries in the repo (`src/main/resources/natives/`, 8 files) are committed deliberately for offline builds, `gradle/wrapper.jar` too; documented in `CONTRIBUTING.md` (2026-09-17).
- [x] spotbugsMain/spotbugsTest failing on pre-existing findings (2026-09-17): `TerminalDiff.Snapshot` `rows()/rowData()/palette()/lineAttrs()` now clone on output (like `shiftOps()`), the hot loop in `apply()` uses locals; `CommonDeviceBusControllerTest`: 4 unused mock fields removed (`URF_UNREAD_FIELD`).
  The full checklist (`checkstyleMain checkstyleTest pmdMain pmdTest spotbugsMain spotbugsTest lintRatchet test`) is green.
- A1 partial side effects: 3 pre-existing spotbugs findings (AT_STALE_THREAD_WRITE_OF_PRIMITIVE, 2x PA_PUBLIC_PRIMITIVE_ATTRIBUTE) disappeared on their own; `SavedCursor.reset()` simplified to `new SavedCursorState()` x2.
