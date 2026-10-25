# Onyx runtime follow-up — 2026-10-25

Development continues without a new beta or release. The initial live checks
are preserved in [the acceptance report](onyx-live-acceptance.md).

## Guest trap after RGB and world reload

Tracing the original Sedna 5.2.0/kernel combination found a worker completing
after the final CPU serialization. RAM uses a live mapped blob, so later
writes can survive while the CPU restores an earlier state. One failing trace
shows three changed RAM bytes before the first restored CPU step, including
a kernel return address. Another independently shows CPU execution after the
last serialization. RGB alone completed 45 native draws without a trap.

The OC2R fix joins active VM workers at server stopping, prevents further VM
ticks and marks running computer chunks for the final save. Registration occurs
after each concrete VM constructor initializes its owner. Robots participate
in the same shutdown barrier. The persisted guest RUNNING state is retained.
This addresses clean server/world shutdown; arbitrary autosave/crash atomicity
and ordinary chunk eviction require separate investigation.

Verification: 1,682 unit tests, all three main Checkstyle tasks and all six
server GameTest suites passed with the final correction. OC2R GameTest totals
are NeoForge 44/46/46 and Fabric 72/73/73 (354 combined); the four new shutdown
regressions run on every target, including the real computer/chunk test.
The final standalone Fabric 26.2 wrapper also completed with exit 0. An earlier
multi-target shell returned 143 after successful reports; the standalone repeat
confirmed successful Gradle, test-report and wrapper completion.

The old-kernel live replay PASSED three successive reloads on each Fabric
version (six total). Every CPU register/CSR and all four complete RAM modules
matched exactly between frozen, final serialized and first restored states;
no worker ran after freeze and before restore. Each cycle additionally required
a fresh guest nonce response, RAM variable, private-root file and native RGB
monitor writes. This confirms the clean world/server shutdown fix.

The first barrier-only attempt failed this same replay: Fabric unload cleared
pending lazy-dirty flags before its final private chunk save. The callback now
eagerly marks the loaded owner chunk dirty after joining, while retaining the
existing post-unload guard. The real-computer regression asserts that the
vanilla dirty flag is immediately set. Synthetic join probes alone missed
this ordering and are not the basis for the live success claim.

The detailed six-cycle report is retained at
`build/reports/onyx-live-acceptance/trap-diagnosis/postfix-final/README.md`.

## Full-root OnyxFS snapshots

The separate OnyxKernel commit `54c4f34` replaces fixed two-block records with packed,
bounded DEFLATE/raw records, enumerates indirect allocations, validates the
whole archive before restoring and recovers interrupted block-write rollback.
A failed partial rollback quarantines the live mount until recovery succeeds.
The existing one-MiB area fits one snapshot of the complete system root;
a second full archive may legitimately return NoSpace. No disk-area enlargement
or kernel-stack enlargement was used.

Legacy raw/RLE snapshots remain readable. A kernel downgrade which writes old
snapshots over packed archives is unsupported; keep a backup before downgrading.
Recovery tests model failures between complete block writes, not hardware torn
sectors or missing backend flush guarantees.

Host verification passed 58 core, 3 tools and 234 kernel tests, including
malformed archives, capacity, indirect blocks and rollback fault boundaries.
Both kernel modes built; matched QEMU boot/login/interactive checks passed.
The existing interactive wrapper returns 1 when its final grep finds zero
illegal instructions; explicit guest/result assertions passed.

The isolated live candidate S-mode kernel SHA-256 is
`b55d8a1740ce7a72ea1e9e7a559333a980f98da891e4145a4a4ed0640c3702a3`.
Its full-root live Fabric 26.1.2 and 26.2 tests passed firmware boot, snapshot creation,
native RGB565 red/green/blue, RAM/private-root world reload and rollback of
the saved filesystem archive after reload. All seven phase assertions passed
on each version.
All 39 snapshot-change files match the committed source exactly. Other kernel
files in that isolated candidate predate concurrent FAT/trap work; this is not
an assertion that the complete candidate binary equals the committed tree.
An additional build from the pinned clean `a6e39fd` source, excluding concurrent
crypto work, produced kernel SHA-256
`f5bee25cac125fcd2aeb1734ddc5d08f49713df0ea47d9feb6a43c2cbf7a92fe`.
Its native Sedna 5.2 full-root create/list/mutate/rollback probe also passed.
Tracked bundled firmware/root resources are unchanged; the stand selects the
candidate artifacts through runtime configuration overrides.

Detailed CPU/RAM traces, fixtures and JSON results are retained locally under
`build/reports/onyx-live-acceptance/`. These are development evidence, not
published release assets.
