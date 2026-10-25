# Sedna 5.3.0 assessment — 2026-10-25

Recommendation: upgrade in a separate change after the Fabric 26.x release
closeout. The isolated checks below found a useful reset fix and no immediate
boot or core snapshot blocker. They do not establish full OC2R world-save
compatibility. Production remains on Sedna 5.2.0 and Buildroot 0.2.10.

## Exact upstream change

The upstream tags resolve to `61a721cdd34d9aef2ca55563073d7d9542580d9f`
(5.2.0) and `82ac59a5e0fa430d748af6417b3684c6ab99eddf` (5.3.0).
There is exactly one intervening commit, touching four Java files:

- `R5Board.getInterruptCount()` returns 32 instead of 31, including reserved
  source zero. OC2R's allocator rejects zero and uses an exclusive upper bound,
  so this exposes valid source 31. Its integer mask supports that sign bit.
- Hard CPU reset clears floating-point registers, exception flags, rounding
  mode and floating-point status.
- CLINT implements `Resettable`, clears software/timer interrupt lines and
  sets time comparators to unsigned maximum.
- PLIC implements `Resettable`, clears priorities, thresholds, enable masks
  and claimed bits, then updates its output lines. It retains pending source
  bits; this is the actual upstream behavior, not a claim of clearing all state.

No fields, existing public signatures, CPU instruction implementations or
build dependencies changed in the tag diff. The two added reset methods and
interfaces are additive. Existing serialized field declarations are unchanged.
The published POM retains Ceres 0.0.7 and the same runtime dependency versions.
Buildroot and sedna-cpm are separate artifacts; this upgrade does not require
adding CP/M or Z80 support.

Primary sources: [release](https://github.com/fnuecke/sedna/releases/tag/5.3.0),
[complete tag comparison](https://github.com/fnuecke/sedna/compare/5.2.0...5.3.0),
[published POM](https://repo.maven.apache.org/maven2/li/cil/sedna/sedna/5.3.0/sedna-5.3.0.pom).

## Checks executed

These used the published 5.3.0 JAR from Maven Central, the project's cached
5.2.0 JAR and Ceres 0.0.7, with standalone `javac`/`java` processes. Neither the
project dependency nor its Gradle state was changed. The probe compiled against
5.2.0 and ran unchanged against 5.3.0.

| Check | Observed result |
|---|---|
| Board snapshot written by 5.2.0, read by 5.3.0 in a new JVM | Passed: a 940-byte Ceres binary snapshot preserved the chosen general register and raised interrupt mask. |
| Hard FPU reset | 5.2.0 retained register value 123, `fs=3`, `frm=4`; the same assertions passed on 5.3.0 with all three zero. |
| Board reset of CLINT and PLIC | 5.2.0 retained comparator 123, source priority 5 and enable mask 2; 5.3.0 reset these to -1, 0 and 0. |
| Real Linux cold boot | 5.3.0 booted the existing bundled Buildroot 0.2.10 firmware and writable ext2 disk to `localhost login:` after 657,974,906 retired instructions. |

The boot probe attached 64 MiB RAM, UART interrupt 4 and a virtio root disk
on interrupt 7, matching those OC2R device choices. The kernel was Linux
6.6.32; ext2 mounted and `/sbin/init`, networking and virtual filesystem setup
completed. This is a standalone emulator boot, not a Minecraft client test.

Local evidence and the executable probe remain under
`/tmp/oc2r-sedna-assessment/` (`Probe.java`, `run.py`, `reset.py`, `old.bin`,
`boot.log`); the exact tag checkout is `/tmp/oc2r-sedna-assessment-src`.
The 5.3.0 JAR SHA-256 is
`c7fd24b7104641ac4258e9ef55bc423371ae086200b1d7f6226efbaa1e3c0742`.
Those temporary paths are local evidence, not distributed project assets.

## Remaining upgrade acceptance

The snapshot probe exercises Sedna's Ceres binary serialization, not OC2R's
NBT visitor, RAM snapshots, disk overlays or complete mounted device graph.
The cold boot does not exercise warm continuation or a second reboot.

Before shipping a dependency bump:

1. Change only Sedna to 5.3.0 and record its verified dependency metadata;
   preserve Buildroot 0.2.10 to isolate emulator changes.
2. Run all six assemblies and existing unit/server GameTest matrices,
   including root-overlay restore and VM lifecycle regressions.
3. Load a world saved with 5.2.0 in a fresh JVM using 5.3.0. Verify RAM-only
   shell nonces, CPU continuation, framebuffer contents and writable root
   files before sending guest input; cover both computer and robot.
4. Test repeated guest reboot/power-off/start, timer/interrupt delivery and
   devices assigned interrupt 31. Preserve old save copies for rollback.
5. Run modern client acceptance and a representative legacy client. Keep
   movement lifecycle and proxy-config fixes outside the dependency change.
