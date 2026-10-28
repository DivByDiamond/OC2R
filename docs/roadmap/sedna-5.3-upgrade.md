# Sedna 5.3 development upgrade — 2026-10-26

Sedna is updated from 5.2.0 to 5.3.0; Buildroot 0.2.10 and Ceres 0.0.7 are retained. This development change does not publish a release or replace bundled firmware. Runtime code was verified at `ce8efb2f`, including the separate prerequisite storage fix `d59029ec`.

The new four hardware contracts cover interrupt 31 and hard-reset behavior of CLINT, PLIC and floating-point state. Each failed against 5.2 and passed against 5.3. A separate Ceres probe preserves old CPU/FPU/timer/interrupt state across 5.2 → 5.3 without a reset. Dependency locks and the local downloader use the published JAR/POM/module, verified against strict SHA256 metadata.

## Automated checks

- Final six assembles and all 1,697 unit tests: exit 0, no failures/errors/skips (53-final-build-unit-refresh.log).
- Current required OC2R GameTests: NeoForge 47/49/49, Fabric 75/76/76, total 372.
- Current storage source: all three main Checkstyle tasks, legacy PMD/SpotBugs pass; both storage regressions observed RED before writer fix, third real dirty-flag regression observed RED before STOP transition fix. Logs stopped-root-red.log, stopped-root-green.log, stopped-eligibility-red.log, stopped-final-green.log and stopped-final-fabric-VERSION.log.
- The CONTRIBUTING-required `:1.21.1:build :fabric:1.21.1:build` and modern compatibility-test/Checkstyle reruns passed after the test helper varargs cleanup (contributing-required-build-final.log).
- All six final artifacts contain exactly one R5Board matching the verified published 5.3.0 class. Artifact SHA256 and storage-class hashes: 53-packaged-sedna-attestation-final.json.
- Full machine-readable report: 53-final-verification.json. Final reports archived in 53-final-build-unit-server-reports.tar.gz.

## Live inputs and outcomes

| Input lineage | Result |
|---|---|
| Untouched original 26.2 world from 5.2 | PASS |
| Explicit new 26.1 world created on 5.2 | PASS; never represented as original world |
| Continued original 26.2, synced and saved on 5.2 | PASS |
| Original 26.1 clone after one-time counter-only recovery/resave on 5.2 | PASS on normal 5.3, with NO recovery agent |
| Running 32 MiB Linux robot saved on 5.2, 26.1 | PASS |
| Running 32 MiB Linux robot saved on 5.2, 26.2 | PASS |
| Untouched original 26.1 | Known failure reproduced on both 5.2 and 5.3; not accepted as successful migration |

Each positive scenario checks pre-input CPU continuation, retained RAM marker bytes and full raw computer framebuffer CRCs (the scene computer supplies the display in robot scenarios), fresh guest RAM/disk response, warm world reopen, two guest reboots, guest poweroff, normal world close/reopen while STOPPED, no automatic CPU resume, manual start and retained disk file. Guest sync with fresh acknowledgment precedes resets/final saves. Each closed powered-off root was independently reconstructed from actual saved sparse pages: readonly e2fsck exit 0 and exact private file bytes read via debugfs. Computer/robot baseline metadata and historical failures were not silently replaced.

Every live run records the actual R5Board CodeSource, JAR SHA256 c7fd24b7104641ac4258e9ef55bc423371ae086200b1d7f6226efbaa1e3c0742 and an empty instrumentation-agent list. Runtime fixtures reject a different 5.3 JAR or any agent.

## Material limitation

Original 26.1 has a pre-existing CPU/RAM checkpoint mismatch: saved CPU time 15,168,660,000 precedes RAM timekeeper cycle_last 19,257,600,037. Its first resumed Linux timekeeping call latches a huge unsigned offset, preventing fresh UART shell response. Unmodified 5.3 repeats the same failure at PC ffffffff8002bc20, rather than repairing it. Original files remain untouched. The separately labeled experimental 5.2 clone recovery advanced only the counter before the first original instruction, synchronized existing guest data, resaved coherently and passed normal 5.3 without an agent. It is not a generic production migration mechanism.

Audit all results with fixture/summarize-53-live.py --require-complete. Output 53-live-results-summary.json preserves the known negative separately from six positive scenarios.

## Power-off disk persistence fix

Live acceptance discovered a pre-existing data-loss path: computer and robot writers omitted the entire VM state when STOPPED, discarding the private root's sparse overlay. They now persist VM state for every power state; terminal data retains its existing conditional behavior. A stopped snapshot has no runner and restores powered off. Sparse serialization includes modified disk pages rather than the immutable base or managed RAM. The computer also eagerly marks its loaded server chunk dirty on the actual STOP transition, retaining the existing closing-chunk guard.

Two real-container GameTests write distinct bytes across a 64 KiB COW boundary, stop, save and restore into fresh containers, then check exact disk bytes and powered-off state. Both reproduced data loss before the writer fix. A third test independently reproduced the missing dirty flag after a previously clean save; it also verifies repeated stop stays a no-op. All three are required on all six targets.

## Coverage and retained evidence

Live Minecraft world migration covers Fabric 26.1.2 and 26.2. All three Minecraft versions on both loaders have automated build/server coverage. A dedicated 1.21.1 live-client migration remains a release-qualification follow-up; this change does not claim that test or publish a beta. Moving constructions and Velocity integration retain their separate acceptance tasks.

The logs, private cloned worlds, baseline provenance, recovery invariants, readonly filesystem checks and executable fixtures are retained locally under `build/reports/sedna53-migration/`. Key reports are `53-final-verification.json`, `53-live-results-summary.json`, `53-packaged-sedna-attestation-final.json` and `counter-recovery-original-26.1.2/RESULT.md`. They are development evidence rather than distributed release assets.
