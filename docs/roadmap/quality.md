# Roadmap: quality (lint, tests, audits, docs)

Static analysis, tests, audit findings, code documentation. Closed items are in [done.md](done.md). Docs language rule: docs are English (code comments follow the same rule for new text).

## 9. Lint and static analysis

Configured: PMD (multithreading/codestyle/design, zeroed), Checkstyle 0, SpotBugs 6.5.10/4.10.3 (html+xml reports), Error Prone 5.1.0 + error_prone_core 2.50.0 (selective, `-PenableErrorProne`, warnings only; Guava shading
against the mixin processor), AvoidDuplicateLiterals, AvoidInstantiatingObjectsInLoops, Qodana 2026.2.0 (`./gradlew qodana`). SpotBugs baseline (511 entries) was triaged: the real bug was a signed byte in `PacketProcessor` VLAN (fixed +
`PacketProcessorTest`); the rest (EI_EXPOSE*, MS_CANNOT_BE_FINAL, PA_PUBLIC_*, ST_WRITE_*, about 400 entries) is deliberate design and stays in the baseline. Client/common: sound card and file import/export messages go through
`client/hooks` (`SoundCardMessageHooks`, `FileTransferHooks`); `ClientImportGuardTest` forbids new client imports in `common`.

- [ ] client/common: 25 files still have client imports (list `LEGACY_OFFENDERS` in `ClientImportGuardTest`): move them one by one into `client/hooks` and delete from the list.
- [ ] Residual hurdles in reports (`build/reports/*`): SpotBugs about 453 in main (EI_EXPOSE_REP2 99, MS_CANNOT_BE_FINAL 71, ...), Error Prone about 100: work through selectively during refactoring.
- [ ] Qodana: wire in or delete the dead `qodana` task (see [39 lint stages](#39-lint-stages)).

## 10. Tests

Extended: inet/ (Ipv4Space(+Extended), IntegerSpace(+Extended), TcpHeader, InetUtils, Rfc1071Checksum, ArpProtocol, DefaultNetworkLayer(12), MacAddressUtils(7), IcmpHandler(4), SessionManager(6); FrameChunkerTest, FrameCodecTest);
terminal/ (TerminalBufferTest (62), SGRTest (17), SGRColorParserTest (8)).

- [ ] Terminal test gaps: see [section 36, Tests](terminal.md#tests-gaps).

## 32. Block logic audit and PMD

Audit of blocks for logic bugs + PMD (2026-08-18, branch work). Closed: BundledRedstone get/set on different faces (`BundledRedstoneCallbacks.java`, `.getOpposite()` removed so set/get/input are symmetric), see
[done.md](done.md#32-closed-items). The PMD warning zeroing item (423 pre-existing warnings) is closed: PMD was zeroed (423 -> 0) at the cost of about 81 inline `// NOPMD` markers, reviewed in [section 33](#33-code-documentation-and-nopmd-review).
The "Redstone Interface side index" note is treated as fixed (commit ec49a41, "redstone FACING rotation"): `getRedstoneOutput`/`setRedstoneOutput` now use `HorizontalBlockUtils.toGlobal()` like `getRedstoneInput`.

## 33. Code documentation and NOPMD review

PMD was zeroed (423 -> 0) at the cost of about 81 inline `// NOPMD` markers and missing documentation on complex logic (branch work, HEAD 8168a1f, 2026-08-19). Two directions: real Javadoc, and reviewing NOPMD where it covers a poor refactor.

### Documentation (Javadoc)

- [ ] **Classes without a header**: add class-level Javadoc (purpose, invariants, threading model) for `EnergyTransferManager` (network energy distribution once per tick: pull/redistribute/push), `BusCableBlockEntity`,
  `NetworkConnectorConnectionManager`, `SwitchHostTable`/`SwitchPortManager`, `MonitorBreak`/`MonitorMerge`/`MonitorRepartition` (rectangle search: BFS expansion, corner priority), `TerminalOutput`/`CSIManager`/`SGR` (VT100 state machine,
  ESC/CSI/OSC phases, what each dispatcher does), `ModeTable` (ANSI mode table), `EstablishedState` (TCP session state machine), `SimpleFramebufferDevice` (dirty layers and frame encoding), `AbstractContainer`/`AbstractMachineTerminalContainer` (slot logic),
  `ICaptureInputStateStorage` (input capture contract).
- [ ] **Complex methods**: Javadoc/comments before complex logic: `distribute`/`redistribute`/`collectNetwork` (energy), `findBestRectangle`/`expandBlock` (monitor multiblock), `selectStyle`/`handleExtendedColor` (SGR), `playSound` (SoundCardItemDevice),
  `stackIntoExistingSlots` (containers), `renderBackground` (run-length drawing).
- Documentation language rule: decided, docs and new comments are English (the project was a mix of RU/EN).

### NOPMD review

- [ ] Review about 81 markers and replace with real code where possible:
  - `// NOPMD getter API ... renaming is API churn` (6: `getCaptureInputState`/`getPowerState`): either rename to `isXxx` with all callers (about 20 files) or move into an interface with a proper Javadoc contract.
  - `// NOPMD 10-case VT100 ... dispatch` (4: `dispatch`, `handleSingleCharEscape`, `handleControlChar`, `handleModifier`): the PMD threshold is 10 and they have exactly 10 branches; extract case groups into methods/tables.
  - `// NOPMD ... depends on loop iteration` (26): check whether allocation can be hoisted out of the loop (pools, mutable buffers); where it truly depends on the iteration, keep it with a Javadoc justification.
  - `// NOPMD immutable after init` (CH2) and `// NOPMD allocation depends on loop iteration`: unify wording.
- [ ] Goal: after the refactor repeat `./gradlew pmdMain` (0) + `checkstyleMain` (0) + `test`.

## 37. Comprehensive audit

Audit of the whole mod (structure, logic, thread safety, style/build, tests, contracts/security), 2026-08-25, branch master, HEAD 2a6b185. All blockers were verified by reading sources. Issue #17 (mount `/mnt/builtin`) can be closed:
the fix in image 0.0.72-oc2r1 is documented in `docs/BUILDROOT.md`. Closed blockers and majors are in [done.md](done.md#37-closed-items).

Open majors:
- [ ] RPCDeviceBusAdapter: handoff of `synchronizedInvocation` VM thread <-> server thread without volatile/atomicity (`bus/adapter/RPCDeviceBusAdapter.java:49,117-121`) + TOCTOU pause/resume <-> step (:101-133): registry rebuild in parallel with RPC dispatch. (Section 47 B3 added `volatile`; TOCTOU part to verify.)
- [ ] InternetConnectionImpl.saveAdapterState: `.get()` on the server thread on autosave (`inet/internet/connection/InternetConnectionImpl.java:38`): tick freeze + deadlock risk.
- [ ] Build: `ContainedDeps` references a non-existent commons-collections4 (`build.gradle.kts:331`); sedna-buildroot drift 0.0.70 vs 0.0.72-oc2r1 (`gradle.properties:20` vs `settings.gradle.kts:30`); architectury/markdownmanual twice on the classpath (fileTree libs + maven).

Minor / thread safety (short): `AsyncExecutorHelper.shutdownNow` interrupts foreign ForkJoinPool threads (:88-104); `GlobalInterruptController` non-atomic RMW of the interrupt mask (see 47 L6); `TaskImpl.closed`/`SocketManager` refcount
non-atomic; `MultipartMessage` has no limit of parallel streams per connection; `ExportedFileMessage` guest-supplied name goes into the client dialog path; `NativeLoader` writes the native lib to a predictable path (user.dir) instead of a temp dir;
natives are downloaded without SHA-256; all linters were advisory-only (`ignoreFailures=true`, see 39 lint stages); OSC/DCS/APC are not interrupted by CAN/SUB; `api -> common` inversion (6 files `api/inet/**`); `common -> client` cross imports
(27 files, see 47 L13); God class `Terminal.java`.

Tests (not covered, high): `tcp/state/*` (state machine), `serialization/*`, `InternetConnectionImpl/StreamSessionImpl/TunnelManager`, `robot/*`, `Utf8Decoder`, csi handlers except SGR (see 47 T1 to T4).

Refuted hypotheses of the audit (do not fix): RPC Gson arbitrary call is impossible (MethodInvoker only registered groups); BlobStorage path traversal excluded (UUID paths); Utf8Decoder/Rfc1071Checksum/TerminalLineShifter/BlockOperationCooldown are correct;
no secrets/ProcessBuilder/eval; CH1..CH11/NullLayer/ICMPReply are live.

### 39 lint stages

Linter config tightening plan (audit 2026-08-25). Parent section: [39 in network-inet.md](network-inet.md#39-security-hardening-of-inet-linter-configs-libraries).
Key facts: checkstyle `severity=error` (`checkstyle.xml:8`) since `b3695d99` (2026-08-27), so the build does fail on a finding — `isIgnoreFailures=false` is set for checkstyle, PMD and SpotBugs (SpotBugs with `config/spotbugs/baseline.xml`); `lintRatchet` exists (CI runs checkstyle, pmd, spotbugs and lintRatchet, which counts violations from the XML reports and fails on growth beyond
`config/lint-baseline.properties`, baseline 0 for checkstyle and PMD); the CI upload-artifact step runs with `if: always()`; Error Prone is default-on for the NeoForge nodes (`enableErrorProne` defaults to true, `allErrorsAsWarnings=false`), but the `core` and `fabric` modules do not apply the plugin; Qodana has `qodana.yaml` + a gradle task but no workflow calls it (dead setup).
Checkstyle "enabled but suppressed": MissingSwitchDefault, FallThrough, IllegalCatch, EmptyCatchBlock, ReturnCount, CyclomaticComplexity, NestedIfDepth, IllegalThrows, NeedBraces, the whole naming/formatting block, MethodLength/FileLength, MissingJavadocMethod, all checks in tests; most
`SuppressionSingleFilter` have no `files` attribute so they mute globally. PMD excluded: GuardLogStatement, ImplicitSwitchFallThrough, CloseResource, UnusedPrivateMethod, GodClass/NcssCount/MutableStaticState etc. (MagicNumber is ACTIVE, do not touch). SpotBugs exclude filter is clean (only jcodec + generated).
Current state (2026-10-21): the repository-wide `checkstyleMain` is green — 0 findings on 1.21.1, 0 on 26.1.2 and 0 on 26.2; the `core` module reports 0 on all three nodes, and the `fabric` module is covered by Checkstyle for the first time (0 findings; PMD and SpotBugs are not applied to it, the `core` module runs all three). Before the import cleanup (state of 2026-10-20) the same run reported 45 findings on 1.21.1, 183 on 26.1.2 and 171 on 26.2, composed of `CustomImportOrder` (import order, including the expanded Stonecutter `//?` blocks), `EmptyLineSeparator`, `OneStatementPerLine` and `MissingSwitchDefault` — that composition is the historical reason for the cleanup, the numbers above are the state after it.

Import order and the multiversion layout: the 1.21.1 node lints the shared tree, the 26.x nodes lint the Stonecutter-resolved trees, and the replacements of `stonecutter.gradle.kts` rename `ResourceLocation` to `Identifier` and `GuiGraphics` to `GuiGraphicsExtractor`, which moves those imports to a different place in the alphabetical order. A single order that satisfies every version therefore does not always exist: `net.minecraft.resources.ResourceKey` and `net.minecraft.resources.ResourceLocation` (and the moved `net.minecraft.Util`) demand opposite orders in 1.21.1 and 26.x. For the eight files that import such a pair, `CustomImportOrder` is suppressed by path in `config/checkstyle/checkstyle.xml` with the reason in a comment; the rule stays enabled everywhere else. The local helper scripts for the import order are documented in [scripts/lint/README.md](../../scripts/lint/README.md).

- [x] Stage A done (00a7aa7), see [done.md](done.md#39-closed-items).
- [ ] **Stage B (failBuild for new violations)**: the gates are in place since `b3695d99` (2026-08-27) — `isIgnoreFailures=false` for checkstyle, PMD and SpotBugs, `checkstyle.xml:8` `severity=error`, SpotBugs
  `baselineFile.set(config/spotbugs/baseline.xml)`; remaining: bring `checkstyleMain` to zero (done 2026-10-21, see the current-state paragraph in this section) and verify the SpotBugs baseline against the current code. PMD 7 has no built-in baseline, `lintRatchet` covers checkstyle+pmd (verify it is wired for both).
- [x] **SpotBugs findings of the Stage 3 code** (2026-10-21): the checked-in baseline did not cover the features added during Stage 3, so `spotbugsMain`/`spotbugsTest` were red before the import cleanup. The findings are fixed instead of baselined and the baseline is back to its previous 515 entries: the real null dereference in the flash writer screen (`Path.getFileName()`), the non-volatile fields of the screen and of the upload session, the monitor taken from a foreign object in `FlashMemoryFlasherDevice`, the static-synchronised initialiser of `FlashWriterService`, the public attribute of `BuiltinDevices` and two broken test assertions are fixed in code; the remaining `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` are point suppressions with a justification (the exposed arrays and objects are either not mutated or intentionally live), and the detector limitation on record accessors is in `config/spotbugs/exclude-filter.xml`, as in commit `f5a4a17b`. `scripts/lint/spotbugs-report.py` lists the findings the baseline does not cover; it never writes to the baseline.
- [ ] **SpotBugs does not analyse the 26.x nodes** (2026-10-21): SpotBugs 4.10.3 cannot read Java 25 bytecode, so `spotbugsMain` on 26.1.2 and 26.2 analyses no packages and reports nothing. The 26.x nodes are covered by Checkstyle and by the compiler, but the SpotBugs gate there is a no-op until the tool version or the analysis JDK is raised.
- [ ] **Stage C (Error Prone always-on)**: default-on only for the NeoForge nodes since `b3695d99` — `enableErrorProne` defaults to true, `allErrorsAsWarnings=false`; the `core` and `fabric` modules do not apply the plugin, so Error Prone is not default-on for the whole matrix yet. Planned configuration: default `enableErrorProne=true` (`build.gradle.kts:447-451`), `allErrorsAsWarnings=false` (:466); critical set `-Xep:*:ERROR`: ArrayToString, UnusedVariable, Finally, DeadException, LoopConditionChecker,
  EqualsIncompatibleType, BoxedPrimitiveEquality, CompareToZero, FormatString; noisy OFF: UnusedMethod, StrictUsedInaccurately, StringSplitter; keep the Guava-shadow workaround (:470-478). Rollout: a week with `allErrorsAsWarnings=true` in CI (`-PenableErrorProne`), collect real hits, fix/disable selectively, then default true.
- [x] CI: the upload-artifact step runs with `if: always()` (linter reports always) since `b3695d99`.
- [ ] Qodana: wire it into a workflow or delete the dead `qodana` task from `build.gradle.kts:488-492`.
- [ ] Style-2 / sync of `qodana.yaml` excludes with checkstyle/spotbugs (generated/gametest).
- Prioritization of the audit (historical): now: AvoidPrintStackTrace + `ByteBufferFlashStorageDevice` fix (done); week: System.out -> LOGGER, MissingSwitchDefault/FallThrough, SpotBugs failBuild + baseline; month: severity=error + ratchet, Error Prone default-on, CI `if: always()`.

## 47. Audit 2026-09-17

Full audit (structure/architecture, logic bounds/arrays/arithmetic, thread safety, style/build, tests, contracts/security), verified by reading sources. Closed items (B1 to B7, L1 to L5, L7 to L9, L11, L12, Style-5, spotbugs failures) are in
[done.md](done.md#47-closed-items). Terminal item P1 is in [terminal.md](terminal.md#47-terminal-items). Verification of each fix: `./gradlew checkstyleMain pmdMain spotbugsMain lintRatchet test gameTest` + a manual run.

### Logic and thread safety (open)

- [ ] **L6: `GlobalInterruptController.raisedInterruptMask` RMW without atomicity** `[common/vm/context/global/GlobalInterruptController.java:9]`: lost interrupt. Verified: still a plain int `raisedInterruptMask`. Fix: `AtomicInteger` or `synchronized`.
- [ ] **L10: `NBTDeserializerImpl` masks a format error** `[common/serialization/nbt/NBTDeserializerImpl.java:103]`: returns `into` on an unexpected Tag; must `throw SerializationException`.
- [ ] **L13: common -> client, 6 imports** `[ComputerVirtualMachine.java:5] -> LoopingSoundManager, [MonitorStateManager.java:5] -> MonitorGUIRenderer, [MonitorBlockEntity.java:4], [BusCableModelData.java:6], [NetworkConnector*.java:9], [BusCableInteractionHandler.java:7]`:
  breaks the dedicated server; invert via EventBus/DistExecutor.
- [ ] **P2: `ServerScheduler` `SimpleScheduler` listeners without synchronization** `[SimpleScheduler.java:8]`: `for(Runnable r:listeners)` without `synchronized` vs `add/remove`.
- [ ] **P3: `SocketManager` `usesCount` without synchronization** `[SocketManager.java:29]`: double-create leak of a Selector.
- [ ] **P4: `BusElementManager` `scanDelay` off-by-one** `[BusElementManager.java:91]`: 101 ticks instead of 100 (minor).
- [ ] **KB-4: `ImportFileRequestManager` forged sender**: the current `!PendingPlayers.contains(sender)` does not consume the request (correct); add a test for `sender==null`.

### Style / build (minor)

- [ ] **Style-1: checkstyle 41 rules suppressed globally** `[config/checkstyle/checkstyle.xml:21-148]` (`SuppressionSingleFilter` without `files`): hides 98 lines > 120 (`Network.java:58` len 151, `Terminal.java:938` len 292). Fix: remove the `LineLength/FileLength/MethodLength` suppressions or document as tech debt.
- [ ] **Style-2: `qodana.yaml` excludes differ from checkstyle/spotbugs** `[qodana.yaml:6] vs [checkstyle.xml:15]`: synchronize (generated/gametest).
- [ ] **Style-3: `gradle.properties` dynamic ccl 4.6.1.+ / cbm 3.5.0.+** `[gradle.properties:32-33]`: pin `strictly`, add `gradle.lockfile`/`verification-metadata.xml`, remove the duplicate `fileTree(libs)` vs `maven libs` `[build.gradle.kts:226 vs 168]`.
- [ ] **Style-4: System.out in gametest** `[gametest/DeviceBusTests.java:34] + [RedstoneInterfaceTests.java:44]`: 5x `println` outside lint due to `exclude "**/gametest/**"` `[build.gradle.kts:408]`: add `// NOPMD` or include gametest in checkstyle with a filter.
- [ ] **Style-6: magic numbers without constants**: checkstyle has no MagicNumber (PMD's MagicNumber is active); introduce `FULL_DIRTY_MASK`, `BLINK_*`, `TAB_WIDTH` constants (see [36 m12](terminal.md#minor-open)).

### Tests: high-priority holes

- [ ] **T1: VM core has 0 tests**: `vm/{VirtualMachine,VMRunner,runner/*,lifecycle/*,context/*}`: add `VMRunnerTest`, `VMLifecycleTest`.
- [ ] **T2: bus has 0**: `common/bus/*` (about 60 files) only `ImportFileRequestManagerTest` + 3 gametests: `BusControllerTest`, `GroupManagerTest`, `RPCAdapterTest`.
- [ ] **T3: inet is incomplete**: no `UDP/DHCP/DNS`, `TcpStates` (SYN_SENT...), `Ethernet/LinkLocal`: extend `DefaultNetworkLayerTest` + new tests.
- [ ] **T4: concurrency**: only `FrameStateTest` + `AsyncVideoEncoderTest`: add a `VMRunner tick vs TerminalDiff capture` multithread test, `SessionManager` under a thread.

### Architecture (medium priority, not a blocker)

- [~] **A1: Terminal god class** (partial): `TerminalNetworkState`, `TerminalRenderState`, `TerminalResizer`, `TerminalHeightResizer` and `SavedCursorState` extracted (2026-09-17), `Terminal.java` 973 -> 426 lines. Remaining: `Terminal.java` is still > 200 lines;
  geometry fields (buffer/colors/styles/scroll margins/cursor x,y) are read directly by 26+ files, and extracting their ownership is riskier than the secondary goal (file size).
- [ ] **A2: package cycles** `blockentity <-> bus.provider`, `vm <-> bus.device.vm`: break through a `DeviceFactory`/`api` interface.
- [ ] **A3: shadowed name `TunnelManager`** `[NetworkTunnelDevice.java:42]`: the inner `TunnelManager` shadows `common.vxlan.TunnelManager`; rename to `TunnelEndpointRegistry`.

Implementation priority of the audit: B1-B7, then L4-L9, then L11-L13, then Style-1/3, then T1-T2, then the rest.
