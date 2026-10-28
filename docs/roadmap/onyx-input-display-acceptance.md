# Onyx native input and completed display frames — 2026-10-28

Accepted in development `work` on 2026-10-28. Capture client checks pass on all
six loader/version targets; integrated Onyx acceptance passes on Fabric 26.1.2
and 26.2.
No release or platform publication is part of this change.

## Resulting behavior

The keyboard and mouse are one block. Shift+use selects it with private selection
feedback; using a computer, monitor or projector stores an identity-checked link.
Using the keyboard captures controls with the world visible. Looking at another
screen on the same guest changes input focus. Right Ctrl releases capture by
default; Minecraft Controls can rebind it, including keys normally used by vanilla
shortcuts. Escape and pointer buttons stay available to the guest. Focus changes,
exit, disconnect, distance loss and unload release held keys/buttons.

Onyx accepts multiple native input queues instead of rejecting every keyboard
after the first. It preserves per-device held state, dispatch order across harts,
left/right modifiers, and the existing userspace pointer snapshot layout. Each device
posts all 256 event buffers within its existing DMA pages, so Sedna's native event
batch cannot silently exhaust the previous eight buffers.

The PSF font has consistent cap/digit height, baseline, descenders and interline
spacing. Console geometry remains 80x24; native framebuffer resolution is unchanged.
Scaled console presentation reuses a bounded horizontal lookup and writes aligned
pixel groups. A real Sedna benchmark measured about 62% less transfer time with
identical output pixels; this is not a Minecraft FPS guarantee.

The optional [frame completion protocol](../FRAME_PRESENT.md) keeps the last
completed snapshot visible during guest drawing. Onyx's console uses
BEGIN → viewport/cursor → COMMIT → DISABLE; legacy raw framebuffer applications
retain immediate behavior. It does not introduce a compositor or a public GUI
ownership ioctl. Full-region console scrolling is still transferred, rather than
sent as a host-side scroll/blit command.

## Images and provenance

- OnyxKernel: `6506c16`, including `e912892` (font/presenter) and `2e512ad`
  (optional publication). The S-mode build uses the repository's target flags,
  including `reserve-x4`, then `llvm-objcopy -O binary`.
- Kernel SHA256: `1a108abad44ad30ae8d70d637704ba262f75212b163dde36e07a1a0211cbb90a`.
- Root SHA256: `81c28f41486d64d6258aed1121ed49f8925b131b9d878ecb1bf06cc2fdc31cff`.
  Only the existing 5124-byte PSF payload at byte offset 4878336 changed from the
  preceding bundled root; app binaries and all other filesystem bytes are unchanged.
- The preceding root remains `onyxfs-20261018.img`, SHA256
  `a03dc9a5bceaf7fe2e65b19c05eaefbf951a35dcfe0b3a272ce753d5cb349561`.
  The older `onyxfs-20261016.img` is also retained. Saved sparse pages resolve
  their exact original base, including on a fresh installation without its cache.
  Existing private roots retain their files, including their installed font.
  A [manual stock-font update](../ONYXOS.md#updating-the-stock-font-in-an-existing-root)
  was verified with a secondary disk: only the font changed, its backup and all
  other prior regular-file contents were preserved, and a cold guest boot passed.

Source commits currently live in the owning OnyxKernel repository; the mod build
uses bundled artifacts and does not fetch these source checkouts.

## Verification

- Isolated clients pass on NeoForge and Fabric 1.21.1, 26.1.2 and 26.2:
  Controls registration, RMB capture, guest Escape/right mouse, Right Ctrl exit,
  F2 remapping before vanilla shortcuts, camera aiming and held-input cleanup.
  The four additional client probes check the real RMB/server-owner path; the
  integrated modern Fabric Onyx probes check that same path with the live guest.

- Required NeoForge and Fabric 1.21.1 builds pass, including Checkstyle, PMD
  and SpotBugs; modern test Checkstyle also passes.
- Full JVM suites pass on all three versions: 575 on 1.21.1 and 576 each on
  26.1.2 and 26.2 (1,727 total, zero failures, errors or skips).
- All six loader/version targets compile and pass Checkstyle. The repeated server
  matrix passes 396 OC2R GameTests: NeoForge 51/53/53 and Fabric 79/80/80
  on Minecraft 1.21.1/26.1.2/26.2. The four keyboard cases live in the repository
  and run through the regular GameTest tasks; temporary worlds are disposable.
- Combined Onyx host suite: 69 core, 252 kernel and 3 integration tests passed;
  RV64 S-mode release build passed. Unrelated existing warnings remain.
- Actual Sedna5.3 guest: ten separately initialized native keyboards each execute
  a marked command; three independent RGB565 diagnostics pass. Across 34 observed
  console frames, 18,619 forced captures stay unchanged inside active transactions.
- The old single-keyboard and malformed/partial-queue paths have regressions;
  round-robin fairness, completion-index wrap, per-device modifier/button ownership,
  cross-hart dispatch gating and all eight pointer bits are covered.
- Atomic host regressions include late-watcher baselines, concurrent store/copy,
  reset/abort/legacy mode, independent NBT snapshots, MMIO reservation preservation,
  memory-budget fallback and failed warm-mount save/cleanup/retry. A warm-save
  regression failed before closing stopped overwriting the partial draw blob; CPU continuation now finishes that same frame.
- Saved-root tests restore old writable pages against the retained bundled base on
  a fresh cache; archive hashes are pinned.
- Integrated Fabric 26.1.2 and 26.2 clients boot the packaged firmware and root,
  execute fresh commands through native PC, monitor and projector inputs, and
  observe guest pointer movement/buttons. Real raycasts switch focus to a monitor
  and the projected wall; held input is released on focus change and exit.
- Both clients show the new font on a 1024x768 monitor, observe negotiated console
  publication and RGB565 client texture updates, then reopen the saved world with
  the guest and projector association intact. The fixture does not inject host pixels.

Retained local evidence: `build/reports/keyboard-rework/`,
`build/reports/atomic-frame-publication/`, `build/reports/onyx-font-refresh/`,
`build/reports/onyx-atomic-publication/`, `build/reports/onyx-multiinput/` and
`build/reports/onyx-input-live/`, and `build/reports/onyx-font-manual-update/`.
Those directories contain reproducible fixtures,
source provenance, logs and candidate images, not distributed release assets.
