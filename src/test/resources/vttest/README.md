# vttest replay fixtures

Recorded VT100 byte streams replayed against the server-side `li.cil.oc2.common.vm.terminal.Terminal`
and compared with committed golden files. The harness lives in
`src/test/java/li/cil/oc2/common/vm/terminal/vttest/` (`VttestHarnessTest` + `VttestFixtures`);
run it with:

```
./gradlew :26.1.2:test --tests 'li.cil.oc2.common.vm.terminal.vttest.*' --console=plain
```

The seven current streams are hand-made. Capturing real upstream `vttest` pages, with a pinned source revision,
page/geometry/capture provenance and reviewed goldens, remains open. Double-size rendering and pixel acceptance are also still open;
the replay harness alone does not establish them. See [terminal roadmap](../../../../docs/roadmap/terminal.md#444-vttest-automation).

## Layout

One directory per fixture: `vttest/<id>/` containing:

| file | required | content |
|---|---|---|
| `page.properties` | yes | fixture metadata (below) |
| `stream.bin` | yes | raw bytes fed to the terminal via `io.putOutput`; current replay drains one whole-file `ByteBuffer`, with split-write replay still planned |
| `screen.expected` | yes | golden: one line per visible screen row, one Unicode codepoint (UTF-8) per cell |
| `styles.expected` | no | golden: per-cell style byte as two hex digits, cells separated by `\|` (e.g. `00\|01\|00`) |

### `page.properties`

- `id` — fixture name (defaults to the directory name)
- `geometry` — columns x rows, e.g. `80x24` or `132x24` for DECCOLM. The harness already calls
  `resizeHeight`/`setWidth` before replay when the requested geometry differs from the default `Terminal` (80x24).
  Use geometries within the terminal's supported limits (`Terminal.MAX_WIDTH`/`MAX_HEIGHT`).
- `status` — `pass` or `xfail`
- `reason` — optional free text; only meaningful with `xfail`

### Golden comparison semantics

`screen.expected`: trailing spaces per line may be trimmed and are treated as spaces;
missing cells at the end of a line count as spaces; blank lines in the middle count; rows
absent from the file entirely count as all-spaces. Empty terminal cells hold `' '` (the buffer
is space-filled on allocation) and codepoint 0 is additionally normalized to `' '`.

`styles.expected`: compared only when the file exists. Trailing all-`00` cells may be trimmed,
trailing blank rows may be omitted; everything missing counts as style byte `0x00`.

Per-row double-size attributes and foreground/background colors do not yet have golden files in this harness.
Their comparison is planned separately; existing codepoint/style fixtures cannot prove double-size geometry or color correctness.

## The xfail ratchet

- `status=pass` — the grid must match, as normal.
- `status=xfail` — the test passes only while the comparison **fails** (the mismatch summary is
  printed as info). If an xfail fixture unexpectedly matches its golden, the test fails with
  "fixture `<id>` now passes — promote status to pass". This ratchets in both directions:
  implementing a feature cannot leave a stale xfail behind, and regressions in `pass` fixtures
  cannot hide.

## Regenerating goldens

```
./gradlew test -Dvttest.regen=true --tests 'li.cil.oc2.common.vm.terminal.vttest.*' --console=plain
```

writes the actual grid over `screen.expected` (and over `styles.expected`, but only if that file
already exists — styles stay opt-in) in the **source tree** `src/test/resources/vttest/`, never
into `build/`. In regen mode nothing is asserted. Regen output is a golden-authoring draft for
humans and AIs alike and **must be reviewed before committing** — a regen run happily bakes the
current (possibly wrong) terminal behavior into the goldens. The committed goldens are written in
the canonical regen shape, so a regen run over them is a no-op diff.

## How the snapshot is taken

Visible screen = rows `[lastRowToDisplay - height, lastRowToDisplay)` of the logical buffer
(the main-buffer formula the renderer and `Terminal.recordNetworkDirtyScreenRows` use); with an
alt-buffer mode active (`ESC[?1049h` etc.) the separate `altBuffer`/`altStyles` arrays are read
directly. Discovery reads the classpath copy (`build/resources/test/vttest`), which gradle
refreshes from this source directory before running the tests.

## Sources for expected content

DEC Special Graphics mapping used by the `dec-special-graphics` fixture cross-checked against
Wikipedia's *DEC Special Graphics* article (which cites VT220 Programmer Reference Manual,
Table 2-4 / IBM code page 1090), https://en.wikipedia.org/wiki/DEC_Special_Graphics — used
values: `j`→U+2518 ┘, `x`→U+2502 │, `k`→U+2510 ┐, `m`→U+2514 └, `l`→U+250C ┌, `n`→U+253C ┼,
`q`→U+2500 ─. The frequently-copied shortcut "`a`..`f` map to the box corners" is wrong
(`a`=▒ U+2592, `b`=␉ U+2409, `c`=␌ U+240C, `d`=␍ U+240D, `e`=␊ U+240A, `f`=° U+00B0,
`` ` ``=◆ U+25C6) — only use the table.
