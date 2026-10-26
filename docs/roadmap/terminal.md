# Roadmap: terminal

VT100/xterm terminal emulation, terminal diff sync, rendering and terminal test automation.
Closed items of these sections are in [done.md](done.md).

## 6. Terminal DynamicTexture rendering

Closed: `TerminalTextureRenderer` + `TerminalTextureBuilder`, NEAREST filtering, LOD (one texture at all distances).
The last open item (enlarge the on-block screen area) moved to [icebox.md](icebox.md#6-terminal-block-area-enlarge).

## 19. Terminal: diff instead of raw UART on the client

Problem (solved): the server used to stream the raw UART byte stream (escape sequences) and the client parsed VT100
again; the Term instance was shared between server and client and not cleared on VM reboot.

Implemented (2026-08-23):
- The server is the only state owner: VT parsing stays on the server (`TerminalOutput`).
- Dirty sync: `Terminal.networkDirtyRows` (BitSet of absolute buffer rows plus a full-refresh flag) is filled from
  `markDirty`/`markAllDirty`; `setChar`, shifters and `TerminalBuffer` route through `terminal.markDirty`.
- `TerminalDiff` (`common/vm/terminal/TerminalDiff.java`): snapshot = reset flag + width + alt-buffer + changed rows
  (cell = codepoint + 2x ColorData(R,G,B,mode) + style, palette modes are not flattened) + cursor + scrollback window +
  cursorMode/DECTCEM/bell + a bitmask of 14 input/render modes. Hand-written StreamCodec.
- Messages: `ComputerTerminalDiffMessage` / `RobotTerminalDiffMessage`; the old `*TerminalOutputMessage` classes and
  their registration are gone; client-side `io.putOutput(UART)` no longer exists.
- Reset on VM restart: `TerminalUtils.resetTerminal` = `RIS.execute` + `captureFull` (also fixed m8 of
  [section 36](#36-vt100-terminal-audit-no-2)).
- Client (`TerminalDiff.apply`) writes rows into the local buffer, syncs alt-buffer/cursor/modes, `markAllDirty()`.
  Input path untouched.

Compatibility: server-side VT100 parsing did not change by a single character; the diff only carries already parsed state.

In-game checks and the vttest run for this section are in [qa-checklist.md](qa-checklist.md#from-section-19).

## 35. Terminal follow-up PRs

Follow-ups from the review of PR #10 (2026-08-21). Small, isolated.

- [x] CH10/CH11 use the new buffer helpers (`deleteChars`/`insertChars`); `System.out.println` replaced by `LOGGER.warn`.
- [ ] **DEC Special Graphics render** (medium): `drawingMode`/`SPECIAL_GRAPHICS` was parsed but not used in rendering.
  Superseded by [section 44.1](#441-line-drawing-characters-dec-special-graphics) (implemented 2026-09-16). Acceptance:
  `ESC ( 0` plus box-drawing in vttest suite 2 (see [44.4](#444-vttest-automation)).
- [x] **DECSLRM + DECSTR** (implemented 2026-09-17):
  - `DECSLRM` (`CSI Pl;Pr s`, `CH6.handleDECSLRM`) via `Terminal.scrollColFirst/scrollColLast`, active only with `DECLRMM`
    (mode 69, `isImplemented=true` in `ModeTable`). Affects cursor homing/DECOM (`Terminal.setRelativeCursorPos` overload with
    `xRelative`), autowrap (`TerminalBufferWriter.rightWrapBoundary`), ICH/DCH (`TerminalBuffer.rightEditBoundary`), IL/DL
    (`copyRowRange`, column-limited shift). Margins reset by `setWidth`(DECCOLM)/`resizeHeight`/`DECSTR`/RIS; `resizeWidth`(DECSCPP)
    clamps non-destructively (`Terminal.adjustColumnMarginsForWidthChange`).
  - Deliberately not done: SU/SD and linear scroll on IND/NEL/RI stay full-width (tied to the scrollback ring in
    `TerminalLineShifter`); SL/SR are not clipped by the left margin.
  - Tests: `TerminalBufferTest` (`decslrm*`, 11 tests).
  - `DECSTR` (`CSI ! p`, `escapes/index/DECSTR.java`, `DecstrTest`): resets mode tables without full RIS; cursor and tabs are
    kept; scroll margins (including DECSLRM) reset to full screen (per DEC VT510-RM Table 5-9 and xterm-410 `VTReset(full=false)`).

## 36. VT100 terminal audit no. 2

Second audit of `common/vm/terminal/**` (74 files), branch 1.21.1, 2026-08-23. Only open items are listed here; the closed
blockers, majors, minors and nits are in [done.md](done.md#36-closed-items). Format `[file:line]`.

### Minor (open)

- [ ] **m2: CPR reports column width+1 (no pending-wrap flag)** `[escapes/csi/DSR.java:22-31]`: `x+1` without clamp; state
  `x == width` is legal (`putChar:31`). Systemic artifact of the immediate-wrap model: ECH/DCH/ICH at `x==width` are no-ops, HTS
  silently loses the last-column tab stop. Full fix: a pending-wrap flag instead of `x == width`.
- [ ] **m5: mode 1048 saves only x/y, restore does a full DECRC** `[escapes/csi/CH2.java:110-113]`: restore overwrites
  style/colors/charset with init defaults. Related: SCOSC (`CH6 's'`) in the alt buffer writes main slots savedX/savedY while
  DECRC in the alt buffer reads altSaved*.
- [ ] **m6: keyboard queues bytes while capture is off** `[TerminalKeyboardHandler.java:29-41]`: only ESC is checked; key presses
  leak into the VM outside terminal focus (`MachineTerminalWidget.tick` sends unconditionally).
- [ ] **m10: `lastRowToDisplay/Max` is a plain int pair with three unsynchronized writers** (Netty IND/NEL, main mouseScrolled,
  getInput): rare jumps of the history view window. Also `hasPendingBell` is a plain boolean (fixed as Л4 in section 47, verify).
- [ ] **m11 residue** (outside the scope of PR #36, do not touch without a reason): `Terminal.getTerminalWidth()` is used only by
  tests (make it a public test API or remove); `incrementLastLineToDisplay(true)` branch is never called.
- [ ] **m12: duplicated magic numbers**: loop `i <= 23` x3 (`TerminalBufferScrolling:36,46`, `TerminalIO:46`) -> constant
  `FULL_DIRTY_MASK = (1 << HEIGHT) - 1`; blink `1000/500` x4 (`TerminalRenderer:42,74`, `TerminalCharRenderer:36`,
  `TerminalBackgroundRenderer:35`) -> named phase constants; `% 8` instead of `TerminalColors.TAB_WIDTH` (`CSIManager.java:94`);
  mouse mode numbers as literals although `PrivateMode.*` exist (`PrivateModeState:105-113`).

### Nit (open)

- [ ] `putResponse(String)` does N full lock/unlock cycles per byte; the response is not atomic relative to readInput.
- [ ] Reentrant lock smells: nested lock in `putInput(String)/putInput(char)` (`TerminalIO:63-71,95-104`).
- [ ] Mixed `//` vs `/* */` comment style (DSR/SGR/SGRColorParser/CH1/CSIManager/DA vs the rest); 7 lines > 120 (LineLength suppressed).
- [ ] XTVERSION version is hardcoded `oc2rvt(1.0.0)` (`CH7.java:21`).
- The stale checkbox "getInput without dirty" (section 31) is already implemented in `TerminalIO.getInput():43-49`: closed.

### Thread safety (summary)

The write path is NOT single-threaded: server = VM Runner (output) + Netty (input); client = Netty (mutates the screen) vs
Render/main (read/scroll without a lock). The input queue is protected correctly (single `io.lock`). `renderers` is a
synchronizedSet + AtomicInteger, correct. Main risks: M3/M4/m9/m10.

### Architecture

- [ ] Package cycles (5): terminal <-> buffer <-> escapes (a full triangle, no package can be extracted), terminal <-> modes, terminal <-> render.
- [ ] `Terminal` is a god object: ~60 public mutable fields, all module logic mutates them directly (consumers go through io/bufferManager).
- [ ] Dirty logic is spread over 6+ places of the data layer (Terminal.markDirty, TerminalBuffer.markDirty, TerminalBufferWriter.setChar,
  TerminalLineShifter x2, TerminalIO.getInput, TerminalBufferScrolling x2): need a single screen-row <-> buffer-row calculation point.
- [ ] 22 `saved*/altSaved*` fields copied by hand in DECSC/DECRC/RIS (partly solved by `SavedCursorState`, see section 47 A1).
- [ ] `Terminal` contains `@OnlyIn(CLIENT)` methods and a lazy TerminalClient: state knows about the client.

### Tests (gaps)

Current coverage is dense: 87 tests / ~367 assertions (SGRTest 17, SGRColorParserTest 8, TerminalBufferTest 62). Not covered:

- [ ] `Utf8Decoder`: multibyte, sequence cut between chunks, invalid continuation, 4-byte sequences.
- [ ] CSIManager on garbage input: more than 10 arguments, CAN/SUB abort, control chars inside CSI.
- [ ] CUU/CUD/CUF/CUB have zero tests (the most frequent ncurses sequences).
- [ ] Reply DSR/DA (reply format in the input queue), HTS/TBC tabs and tabs interaction with DECCOLM.
- [ ] `?1047l` exit semantics.
- [ ] Regressions for blockers B1/B2 (one parameterized test).

## 44. Terminal: line drawing, strikethrough, double-size

Three terminal tasks (acceptance: boxes are visible in tmux, vttest section 2). Reference docs: xterm sources and the VT510
programmer manual. Queue: 44.2, 44.1, 44.4, 44.3.

### 44.1 Line drawing characters (DEC Special Graphics)

Done 2026-09-16. Results: `TerminalBufferWriter.mapDecSpecialGraphics` (mapping 0x60-0x7E to Unicode, applied in `putChar` before
`setChar`) + `TerminalCharRenderer.renderBoxDrawing` (procedural quads for box-drawing U+2500-253C and scan lines U+23BA-23BD; the
rest are ordinary font glyphs). Parser side (`ESC ( 0`, `ESC ) 0`, SO/SI, DECSC/DECRC charset save, RIS/DECSTR reset) was already there.
The font cannot supply these glyphs (checked with `canDisplay` on monocraft-r.ttf: no glyph in U+2500-U+257F), hence procedural quads.
Reference table: xterm-411 `fontutils.c` `dec2ucs`; `_` (0x5F) is a space; mapping applies to 0x60-0x7E only.

### 44.2 Strikethrough (SGR 9/29)

Done 2026-09-16. `Terminal.STYLE_CROSSED_OUT_MASK = 1 << 7` (note: `style` is a `byte`, so 0x80 is a negative byte; the diff
serialization keeps the bit, covered by a round-trip test), `SGRStyleDispatch` case 9 set / case 29 clear, a quad at mid cell height
in `TerminalCharRenderer`.

### 44.3 Double-sized characters (ESC #3, #4, #5, #6)

- [x] PR-A: parsing + per-line attribute (2026-09-17): `Terminal.LINE_ATTR_*` (0 SINGLE, 1 DWL, 2 DHL_TOP, 3 DHL_BOTTOM), per-row
  `lineAttrs`/`altLineAttrs`, `handleHash` sets the attribute on the whole current line (xterm `doublechr.c`), `setWidth`/`resizeHeight`/
  `clear`/`clearScrollback`/`TerminalLineShifter` copy/blank attributes, `TerminalDiff` `PROTOCOL_VERSION 1->2` with a `lineAttrs`
  byte per row. Codes: `# 3` = DHL top, `# 4` = DHL bottom, `# 5` = SWL, `# 6` = DWL; `ESC # 7` does not exist; the attribute is per line
  and applies from column 0. Rendering is still a stub (single width).
- [ ] **PR-B: double width and render**. Keep the physical buffer stride at `width`, with one codepoint per logical column;
  doubled rows expose the first `max(1, width / 2)` cells and render each at twice the pixel width. Existing per-row attributes and
  protocol v2 suffice: padding cells, a per-character wide flag and a protocol extension are not required for whole-line scaling.
  Unicode wide-character handling remains separate. Cursor bounds, delayed autowrap, tabs, editing and copy/scroll paths still need
  row-aware behavior. No renderer reads `lineAttrs` yet: capture them in `FrameState`, render DWL at 2x width, clip the appropriate
  half of a 2x-width/2x-height DHL glyph to its own row, and restore normal scale for SWL. Cover foreground, background, procedural
  glyphs, decorations and cursor in the 1.21.1, 26.1.2 and 26.2 GUI/world render paths.
- [x] MVP criterion: parsing #3/#4/#5/#6 without a warning, marks dirty.

Audit of PR-A (9f472797) against EK-VT100-UG-003, 2026-09-19: lifecycle is complete (parsing, attribute transfer with rows, codec v2,
clearBuffers, DECALN leaves attributes alone), rendering is absent. Notes for the render work:
1. Render: verify against the screenshot `vt420.png`.
2. Updated source audit (2026-10-25): xterm's `doublechr.c:repaint_line` clamps the cursor on conversion to double size; the earlier
   statement that it does not clamp was incorrect. The DEC manuals specify loss of characters right of the center; xterm's conversion
   does not erase that hidden tail. Proposed policy: follow DEC loss semantics, clear the discarded cells and their rendition, clamp
   the cursor and clear pending wrap. Returning to SWL must not resurrect discarded text. This is still unimplemented.
3. DHL top/bottom pairs are the application's duty, not enforced by the terminal (xterm is also laissez-faire); scrolling copies attributes 1:1.
4. xterm ignores line-size changes while DECLRMM (left/right-margin mode) is enabled. Plan this explicitly instead of implicitly
   scaling horizontal margins; verify the transition into DECLRMM with existing doubled rows before implementing that interaction.

Primary references: [DECDHL](https://vt100.net/docs/vt510-rm/DECDHL.html),
[DECDWL](https://vt100.net/docs/vt510-rm/DECDWL.html), [DECSWL](https://vt100.net/docs/vt510-rm/DECSWL.html),
and [xterm doublechr.c](https://github.com/ThomasDickey/xterm-snapshots/blob/master/doublechr.c).

Separate rendering follow-up (2026-10-25): `ModRenderType.getTerminalText` caches the world render type and calls
`FontHandling.getAtlas()` only on first construction, while new glyphs are uploaded through `FontAtlas.getTextureId()`.
Live 26.2 CPU/GPU readback showed 512 differing pixels in the newly rasterized glyph region before a fresh upload and zero afterwards.
Ensure newly added glyphs reach the GPU during world-only rendering; also clear `textureIsDirty` after a successful upload.
This remains open and is **not** the solved colored-block-atlas overlay bug: a controlled forced upload did not remove that overlay.
The overlay fix (`eb032cac`) preserves sprite UV mapping by calling vertex addition and `setUv` separately on the sprite wrapper.
Local acceptance evidence (not committed): `build/reports/terminal-world-diagnosis/INVESTIGATION.md` and
`build/reports/onyx-live-acceptance/trap-diagnosis/postfix-final/26.2/terminal-render/` (normalized GPU readback and explicit-upload JSON).

### 44.4 vttest automation

vttest is interactive and cannot run in CI directly. The escape sequences it emits are hard-coded, so fixed geometries (80x24 and 132x24)
allow per-page golden cell grids run as tests.

Layer 1 (cell-level, deterministic, no client):
- [ ] Harvest upstream streams: pin a vttest source revision and record page, geometry and capture provenance. The seven current
  fixtures are hand-authored; recorded upstream page replay and its acceptance remain open.
- [x] Data-driven harness: `VttestHarnessTest` discovers per-page `stream.bin`, `screen.expected` and optional `styles.expected`,
  resizes a server `Terminal` to the requested geometry before replay, and compares cells/styles with an xfail ratchet.
- [ ] Extend golden coverage to row attributes and foreground/background colors; optional style comparison already exists.
  Add split-write replay alongside the current whole-stream replay, and review expected output independently of the implementation.
- [ ] Contested pages are decided explicitly: request/response pages (DSR/DA/status line need a stub input), purely visual ones (blink,
  double-size) go into an exception list with a reason or to layer 2.
- [ ] Golden grids are reviewed manually before merge, otherwise hallucinations become the spec.

Layer 2 (pixels, catches render errors invisible in the buffer):
- [ ] Through the GameTest infrastructure ([section 41.1](multiloader.md#411-gametest-ci-and-cd)): a guest script prints a deterministic
  pattern; capture the rendered terminal GUI and world display and compare against reviewed pixel references. This still needs a
  dedicated capture/golden test for the procedural glyphs of 44.1 and the scale of 44.3, across all three Minecraft versions.

Priority: layer 1 first (also the acceptance of 44.1/44.2), then terminal-specific pixel coverage using the existing client GameTest infrastructure.

Narrowed scope (2026-09-16: not a universal stand, but a VT for shell/vim/tmux/OnyxOS):
- Keep: `vttest` + `VttestHarnessTest` goldens as the regression format, `ref/vttest/charsets.c` only for contested VT tables, current scrolling property tests.
- Add exactly 6 regressions: UTF-8 (`split write`, `0xFF` + resync, `wide at EOL`) and resize (`altBuffer`, `scroll region`, `DECCOLM 132->80`
  with cursor/visible rows/buffer).
- VT regressions only on demand: DEC graphics U+2500 (tmux) done, SGR bold done, margins/DECOM done (`decom-margins` fixture), wrap done
  (`line-wrap` + `no-autowrap`), scrollback done (`scrollback` fixture), `simcity`/`nInvaders`.
- Defer: headless xterm runner, mass fuzz, `terminal-core` module, kitty/sixel/hyperlinks/clipboard, wide Unicode/emoji matrix, copying
  `libvterm`/`kitty` without need. `TerminalBufferTest` stays disabled until the core split of [section 42](multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion).
- Done criterion of the next stage: 44.1/44.2 green (`dec-special-graphics` xfail to pass) + the 6 tests, without extending the harness.
- [ ] Reference links: invisible-island.net/xterm/ctlseqs/ctlseqs.html; Wikipedia "DEC Special Graphics"; xterm `convtbl.c`; VT510 manual (bitsavers pdf/dec/terminal/vt510).

### 44.5 Remaining tofu glyphs

Screenshot audit 2026-09-19 (`vt420.png` vs `oc2r.png`). Monocraft 4.0 (checked with `Font.canDisplay` on the real TTFs) lacks 9 of the 31 DEC Special
Graphics glyphs; the buffer mapping is correct but the screen shows .notdef.

- [x] `a` -> U+2592 (medium shade), done 2026-09-19 by a FONT PATCH (not procedural): Monocraft HEAD (4.2) + a 6x9 edge-to-edge medium_shade matrix
  (placeholder, art pass pending). All 4 weights (r/b/i/bi) rebuilt with the upstream generator and swapped into assets; the procedural rasterizer
  was removed. Regression shield: `MonocraftFontCoverageTest` (`canDisplay` over all 4 TTFs). The rest is present in the font: diamond, degree, plus-minus, pi, not-equal, pound, middle dot.
- **Decision: keep the procedural quads now; remove them only after a font with real line-drawing glyphs lands.**
- [ ] **PLAN: move line drawing into the font**: build the full glyph set (box-drawing and scan lines instead of the procedural quads of 44.1) on Monocraft HEAD and prepare
  an upstream PR to IdreesInc/Monocraft (OFL-1.1, no RFN declared, modifications are legal; prep artifacts: /tmp/opencode/Monocraft fork + probe scripts). When the glyphs are
  in the font, simplify `TerminalCharRenderer`: remove `isBoxDrawingCharacter`/`BOX_CHARS_MASK`/`SCAN_CHARS_MASK` and render through the normal glyph path. Caveat: the procedural path is
  pixel-perfect for the cell scale, atlas glyphs have a fixed raster resolution; compare quality on a real screenshot before deleting the quads.
- [ ] `b c d e h i` -> control pictures U+2409..U+2424: tofu. A full procedure (frame + 2-letter caption) is expensive for rare use; screenshot triage first (mc/vttest).
- [ ] `y z` -> U+2264/U+2265: tofu. A composite "glyph + bar quad" is possible but only an approximation; leave tofu until the font decision.
- [ ] Future idea: an alternative font (including a VT220 font ROM dump, authentic but questionable in Minecraft; settle licensing before any re-fonting). Checked 2026-09-19: fresh
  Monocraft master TTF still lacks the same 9 glyphs (shade, control pictures, le/ge and all box/scan glyphs), so changing the font does not close the gap.

### 47 terminal items

Full section: [quality.md](quality.md#47-audit-2026-09-17). Terminal items:

- [ ] **P1: Terminal buffer/colors/styles without a lock vs serializeRow** `[Terminal.java:58] + [TerminalDiff.java:284]`: frame tear (accepted, but document as a risk) - optionally move under `networkDirtyLock` or document as known.
- [ ] Accepted residue from section 46 (see [done.md](done.md#46-terminal-sweep-follow-ups)): cell content tear between frames and in-place palette mutation.
- Closed terminal items of this audit (B6 renderers iteration, L4 hasPendingBell, A1 partial Terminal split): see [done.md](done.md#47-closed-items).
