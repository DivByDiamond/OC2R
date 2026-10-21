# Stage 3 acceptance

Stage 3 is complete for the four supported targets (verified 2026-10-20):
NeoForge 1.21.1, 26.1.2 and 26.2, and Fabric 1.21.1. Fabric 26.x and the
six-target release matrix remain separate work. The stage is closed as
implementation and acceptance; the remaining tail — the repository-wide
`checkstyleMain`, CI confirmation of the branch and documentation sync — moved
to Stage 4 Wave A (see
[multiloader.md](multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion)).

## Automated checks

Run from the repository root:

```bash
bash scripts/verify-stage3.sh --console=plain
```

Final verification completed with exit status 0: 543 main and 17 core unit tests
on each NeoForge version (1,680 total), and server GameTests 38/38 (1.21.1),
40/40 (26.1.2), 40/40 (26.2) and 56/56 (Fabric 1.21.1). All four assemblies and
both 26.x data generators passed. Fresh JARs contain no temporary QA classes.
The final matrix was repeated after the PMD cleanup with exit status 0; its log
is ignored `build/stage3-qa/verification-post-pmd-final.log`.
For this local repeat, temporary init scripts selected disposable worlds on a
RAM disk for Fabric and NeoForge 26.x; the test tasks, result validation and
production resources were unchanged. The normal on-disk 26.1.2 server had also
completed its full save and shutdown in an earlier successful run.

This runs both 26.x client/server data generators and validates their output,
assembles all four targets, runs the NeoForge and core unit suites and all server
GameTests. Static analysis is separate: run `:<version>:build` or the repository's
lint tasks for those checks. The NeoForge `gameTest` wrapper requires a fresh,
nonempty TSV and writes JUnit XML under
`versions/<version>/build/test-results/gameTest/results.xml`.

The PMD follow-up completed successfully on 2026-10-20. PMD 7.16.0 replaces
7.7.0, whose bytecode reader rejected Java 25 classes with
`Unsupported class file major version 69`. Java 25 support starts with 7.16.0;
see [PMD Java support](https://pmd.github.io/pmd/pmd_languages_java.html).
Dependency locks and verification hashes were refreshed; the three primary PMD
JAR hashes were also checked against Maven Central. Failure checking remains
enabled.

Full `pmdMain` and `pmdTest` pass on all three NeoForge nodes and all three core
nodes, and `pmdGameTest` passes on all three NeoForge versions. Version-dependent
imports were corrected; local suppressions document intentional ownership,
identity comparisons, per-iteration fixtures and geometry allocations. Five
explicit ruleset exclusions preserve the repository's API, accessor, monitor-lock,
defensive-switch and cross-version suppression conventions. The scanner GameTest
now checks the expected error messages instead of leaving empty catch blocks.
All four assemblies, 1,680 unit tests and the three `checkstyleTest` tasks passed
in the same successful invocation; see ignored
`build/stage3-qa/pmd-verification-final.log`.

The separate repository-wide `checkstyleMain` run still reports import-order
and formatting findings; see ignored
`build/stage3-qa/pmd-quality-verification-final.log`. This PMD completion does not
claim that every `build` task or all static-analysis tools pass.

Check the server logs as well as test counts. A test can report success while
the VM worker logs an exception. In particular, no `Payload ... may not be sent
to the client!` errors are acceptable. A vanilla mock server player skips the
configuration handshake: use `TestSupport.serverPlayer` for 26.x tests that add
players to the level, so NeoForge's real mock-channel setup is applied before
computer and robot tracking broadcasts reach them.

Fabric-specific GameTest suites use class-sized batches, and the recipe sweep
reuses one mock player instead of constructing one per recipe. The runner checks
Minecraft's session lock before clearing its disposable `build/run/gameTest/world`,
so player data and test chunks cannot accumulate between local runs. The direct
Gradle task has a 12-minute limit; the matrix script also bounds its invocation.
Minecraft's region-file flush must finish before a run counts as complete.

The intermittent pre-start hang was reproduced on 2026-10-20: 53/54 tests were
done, while the last recipe test had `started=false`, `chunksLoaded=false` and
`tickCount=0`. The shared `oc2r:empty` NBT had four-byte string lengths and
little-endian numeric payloads, so Minecraft read an empty compound and a
zero-sized structure. At a chunk boundary, its computed bounds included an
unforced neighbouring chunk (ticket level 32, rather than entity-ticking level
31). The test's own tick timeout never started. Forcing precisely that missing
chunk allowed the stalled test to finish immediately. The fixture is now valid
Java Edition NBT with 9x9x9 air blocks, reproducible with
`python3 scripts/generate-gametest-structure.py`. `GameTestStructureTest` checks
it with Minecraft's own NBT reader and failed against the original malformed
file before passing with the repaired resource. Test logs and live diagnostic
snapshots are under ignored `build/fabric-hang-investigation/`.

The repaired fixture completed 22 consecutive Fabric launches with all 54 tests
passing. With the expanded suite, a further ten consecutive launches passed all
56 tests and completed shutdown; a separate 56-test prerun also passed.
An earlier additional launch was interrupted by a user-initiated Java process
stop; it is not counted as either a completed run or a reproduced hang. Forced
termination of the QA clients was also user-initiated, not evidence of an OOM.

The client follow-up exposed two independent lifecycle defects, each reproduced
before its fix: merging a wired monitor left its old origin devices cached in
the bus, and cold VM restarts retained disposed devices' interrupt reservations.
`DeviceBusTests.monitorMergeReplacesCachedDevices` now checks the live bus after
actual monitor item placement; that intermediate 26.1.2 suite passed 39 required
tests before the first-scan regression below increased the final count to 40.
`VMDeviceBusAdapterTest` checks repeated cold mounts with 20 interrupt-using
devices and separately verifies that suspension preserves reservations. The
cold-mount test failed before the reservation refresh was added to disposal.
NIC preview geometry also has focused degree conversion and face-picking tests;
26.x now draws its model and face overlays through the picture-in-picture API.
These regressions supplement the client checks below.

A later warm-reload check isolated another reset cause: `DeviceTable.scan()`
emitted both add/remove events with empty device lists on an unchanged bus.
The VM lifecycle interpreted either event as a hardware change and rebooted.
An unchanged live rescan reset the computer CPU counter from 6,892,978,008 to
13,928,044; its neighbour continued. `DeviceTableTest` failed both regressions
before the fix. The table now emits change events only for actual additions or
removals, while before/after scan notifications remain unconditional.

A second reload reset was caused by deferred cable initialization: the first
bus traversal resumed the VM with only its internal devices, then the next
scheduler tick announced all saved monitor/projector interfaces as new hardware.
Cable traversal now collects interfaces synchronously on its first complete
neighbor walk, before controller registration and the VM device snapshot. An
unloaded neighbor keeps initialization pending. The new
`firstBusScanDiscoversSavedInterfaces` GameTest failed on tick zero before the
fix and passed afterwards; the 26.1.2 server completed all 40 required tests.
Full client reload continuity is checked separately below.

The subsequent real reload exposed a persistence collision: a monitor and its
keyboard were flattened into entries with the same provider key. Saving one
member overwrote the other, losing either keyboard IRQ/virtio state or monitor
address/framebuffer state while the VM still reserved the old resources. Grouped
providers now store each member under a stable semantic key within
`oc2r:group_members`; ordinary single-device provider data keeps its flat format.
Both discovery/load orders, sibling removal, missing new members, legacy flat
fallback and unique member keys are covered by focused tests. The two original
round-trip tests failed before the fix. Already overwritten legacy state is only
partially recoverable; a cold restart establishes a complete new snapshot.

A separate pending-state defect affected even internal cards: serializing a group
before its first discovery replaced loaded device data with an empty compound.
Saving an unloaded group similarly erased its cached snapshot. Empty undiscovered
groups now preserve their pending state; a confirmed-empty query still clears it.
Both preservation tests failed before the guard, while the confirmed-removal
control passed. These cases are also covered by `GroupManagerTest`.

Loaded-chunk facade checks also reproduced a stale compiled render section:
the client facade and cached model data both contained stone while the mesh
still showed the fallback iron block. A same-state client render notification
immediately corrected it. Both facade deserialization paths now refresh model
data and invalidate the client section; two live add/remove cycles passed
with the compiled fix and without diagnostic repair. The local QA harness was
also corrected to call `removeFacade()` and to close containers on the server;
those harness corrections are not production changes.

## Client acceptance

Client acceptance uses disposable worlds on both NeoForge 26.x targets. The
completed 2026-10-20 follow-up is recorded below; the earlier partial checks in
this section and [multiloader.md](multiloader.md#26x-rendering-notes-2026-10-14)
remain historical evidence. Screenshots and runtime logs are ignored artifacts.

Live follow-up on 2026-10-20 in the 26.1.2 demo world: after a cold world reload,
the blue network cable remained reciprocal/resolved and both guest NICs worked
after reconfiguration. TCP echo passed between OnyxOS guests, and HTTP returned
`200 OK` with the expected body through the Internet Card. `/dev/fb1` and
`/dev/fb2` drew distinct solid frames to the single and 3×2 monitors; both stayed
visible at an oblique camera angle. Reopening the robot terminal after a reload
also exposed stale client VM state (`Bus Incomplete` while the server was
`RUNNING/READY`); the terminal now receives a fresh VM snapshot when opened.
After the fix, `help`, `oc2ctl robot-status` and `oc2ctl robot-demo` worked, with
`ROBOT_DEMO_OK: every queued action completed SUCCESS`. Evidence and the local
walkthrough are in ignored `build/demo-qa/` and `DEMO_LOCAL.md`. These checks
cover only part of the matrix below; they do not close the all-facings, projector,
robot item or cross-version reload items.

Live follow-up on 2026-10-20 in an isolated copy of the 26.1.2 demo world,
opened with the NeoForge 26.2 client: the real guest projector framebuffer
(`/dev/fb1`, 640x480) produced a visible green frame on the arena surface, and
the image remained visible from an oblique view. Moving the camera behind the
projection surface showed its opaque back face, with the projection out of view.
This establishes basic framebuffer presentation and camera-dependent visibility;
it does not complete the terrain/entity occlusion, power-cycle or cold-reload
acceptance. Screenshots and the upgraded QA world copy are under ignored
`build/demo-qa/` and `versions/26.2/run/saves/OC2R Demonstration 26_2_qa`.
The 26.2 client shutdown also logged four `Allocator` memory-allocation leaks
whose stacks lead through robot VM device mounting. The server stopped normally,
and the root cause was that `Robot.tick()` passed `firstTick` to its lifecycle
handler after `Entity.tick()` had already cleared it. This skipped both idle
target initialization and registration of the robot's world-unload cleanup.
The fix preserves the flag before calling `super.tick()`. Regression checks now
assert that an idle robot's target matches its position; GameTests pass 38/38 on
26.1.2 and 26.2, and both shutdown logs end without allocator leak reports.

Live follow-up on 2026-10-20 in 26.1.2 also found that tall uppercase and digit
glyphs in the terminal atlas exceeded the row height. The renderer now scales
those glyphs vertically around the common baseline, leaving their monospaced
horizontal advance unchanged. The focused font regression test passes.

- [x] Monitor models: each facing, single block and multiblock edges/corners;
  framebuffer alignment, GUI image and powered/unpowered states.
- [x] Bus cable models: straight, bends, branches, six interface directions,
  facades and facade removal on an already-loaded chunk.
- [x] Network cables: connect/disconnect on loaded chunks and after reload.
- [x] Robot item: inventory, held item, dropped item and placement; robot model
  and inventory/terminal screens after placement and reload.
- [x] Storage item default/dyed colours and item model properties; NIC face
  overlays for each enabled/disabled face.
- [x] Computer and robot terminal GUIs: background, text, cursor, clipping,
  scrolling and GUI scaling on both 26.x versions; computer in-world terminal
  depth. Robots expose their terminal through the GUI, not world-space text.
- [x] Projector on 26.2: real guest frames, terrain occlusion, camera movement,
  power loss/restoration and world reload, including the reversed-Z pipeline.
- [x] Projector entity/particle occlusion on both 26.x versions; animated and
  cutout block-entity model depth. Entity shadows use bounding boxes and particle
  shadows use billboard bounds; exact entity mesh shadows are not implemented.
- [x] Warm VM CPU/RAM continuation and framebuffer retention after world reload.

### Completed live follow-up (2026-10-20)

Both 26.1.2 and 26.2 passed the full checklist above using visible, muted QA
clients. Monitor checks covered all four horizontal facings, single and merged
screens, GUI alignment and actual energy drain/restoration. Cable checks covered
six interface directions, loaded topology changes and two facade add/remove
cycles. Robot checks included the actual stationary dropped-item renderer,
placement, held/inventory models and reopening its screens after reload. NIC
checks used real hover/click input on all five selectable faces, the disabled
south face and GUI scales 1, 2 and 3. Terminal depth used a real opaque block
between the camera and the computer screen.

Projector checks used guest-written gradient frames, terrain, an isolated
floating pig, real smoke particles and animated/cutout chest geometry. Smoke
shadows disappeared after particle expiry on both versions; the corresponding
clean views contained no shadow pixels in the measured wall region. These checks
also covered power loss, restored power, oblique cameras and world reload.

Strict warm-load checks compared all 11 framebuffer addresses and CRCs before
sending any guest input, checked that both computer CPU counters continued, then
verified fresh RAM-only shell nonces in the computer and robot. They passed on
26.1.2 and 26.2 in a new JVM, including a 26.1.2-to-26.2 world copy. Already lost
legacy grouped state still needs one cold computer stop/start to establish a
complete new snapshot; the fix preserves subsequent warm snapshots.

The final evidence is under ignored `build/stage3-qa/26.1.2/` and
`build/stage3-qa/26.2/`: `quiescent-warm-final.log`, `warm-final.log`,
`cross-version-warm.log`, `isolated-visual-final.log`,
`acceptance-driver-continued.log`, screenshots and `particle-shadow-metrics.json`.
The QA helpers were injected only through temporary Gradle init scripts, not
added to production sources. Both clients were saved and closed after checking.
Earlier user-initiated Java stops are not OOM evidence; any later QA graphics
process cleanup happened only after the integrated server had saved all worlds
and unloaded its configs.

## Known limitations

These are accepted limitations of the four verified targets, not open defects:

- Legacy worlds: grouped monitor/keyboard state that was already overwritten
  under the old flat provider key is not recovered by a warm reload — one cold
  restart of the computer establishes a complete new snapshot; new snapshots are
  correct (see "The subsequent real reload exposed a persistence collision" and
  "Completed live follow-up (2026-10-20)" above).
- Projector shadows for entities and particles are computed from bounding boxes
  and billboard bounds, not from the exact model mesh; exact entity mesh shadows
  are not implemented (see "Client acceptance" above).
- The manual content audit ([../MANUAL_CONTENT_AUDIT.md](../MANUAL_CONTENT_AUDIT.md))
  did not include refreshing the Chinese translation (see "Implementation"
  below).

## Implementation

- [x] Port model/item/blockstate datagen to 26.x and run the providers; check
  generated resources against the models actually used by the client.
- [x] Embed and port Markdown Manual for all four supported targets, removing
  the external Markdown Manual and Architectury requirements. See
  [implementation and provenance](../MARKDOWN_MANUAL.md).
- [x] Complete live manual acceptance on each target: opening with the item,
  tabs, links/back navigation, scrolling, formatted text, item/block/tag previews
  and textures. Evidence belongs under `build/reports/manual-qa/`; navigation
  assertions alone do not establish visible document rendering.

Manual acceptance completed on 2026-10-18 for all four targets. The additional
NeoForge 26.1.2 Russian run resolved and laid out all 43 Russian pages without
English fallback, with ten screenshots. English/Russian content was audited
against current code and the bundled Linux image; see
[the content audit](../MANUAL_CONTENT_AUDIT.md). Chinese translation refresh is
not included in that audit.

The required implementation and client acceptance are complete for these four
targets. Stage 4 owns publishing and the expanded release matrix.
