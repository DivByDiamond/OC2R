# Recorded vttest layer-1 acceptance

Four real upstream pages supplement the seven hand-authored regression fixtures.
The upstream checkout is pinned to
[`d37932dc8a7e5dab702ba34020bdbb12cdf12858`](https://github.com/hharte/vttest/tree/d37932dc8a7e5dab702ba34020bdbb12cdf12858)
(vttest 20140305). Capture runs the unmodified executable on an 80x24 PTY, answers
its startup DA query as a VT420 and selects the documented menu/hold boundaries.
The 132-column page contains its own DECCOLM switch.

| Fixture | Geometry | Upstream function / selection | Reviewed expectation |
|---|---|---|---|
| `upstream-cursor-80` | 80x24 | `main.c:tst_movements`, menu 1, first hold | Unbroken outer `*` and inner `+` borders, centered E frame and instructions |
| `upstream-double-80` | 80x24 | `main.c:tst_doublesize`, menu 4, first hold | DWL row 9; DHL top/bottom rows 11/12 and 14/15; normal row 7 and margin markers |
| `upstream-double-132` | 132x24 | Same function, third hold | Same attributes, text and margins shifted by the upstream `w1=13` rule |
| `upstream-colors-80` | 80x24 | `color.c:show_test_pattern`, menus 11 / 6 / 2 | Two 8x8 foreground/background matrices, normal and bold |

Rows above use the manual's one-based numbering. Expected cells, style bits,
row attributes and semantic colors come from **libvterm 0.3.3**, independently of
OC2R. The page layout and matrices were also reviewed against the pinned C source.
The color format preserves `default` versus `index:N` versus `#RRGGBB`; styles
remain separate, so bold is not silently flattened into a palette index.

Each page records its revision, source URL/function/hold, menu inputs, stream
SHA-256 and oracle version in `page.properties`. The harness checks the capture
hash. Recorded fixtures are skipped by OC2R's `vttest.regen`; it cannot silently
replace an independent golden with its own output.

Every fixture is replayed whole and in 1-, 7- and 64-byte chunks, with the same
cell/style/foreground/background/line-attribute assertions. Eleven fixtures give
44 replay cases per Minecraft version. The fixed chunks split CSI and charset
sequences; dedicated decoder tests cover 2-, 3- and 4-byte UTF-8 across writes.

## Reproduce captures and oracle output

Requirements: the pinned clean `ref/vttest` checkout, a C compiler, make,
libvterm headers/library and `pkg-config` reporting libvterm 0.3.3. The historical
upstream config checks require `-std=gnu89`; the capture tool sets this without
patching its source.

```sh
python3 scripts/terminal/capture-vttest.py
```

This writes drafts to `build/reports/vttest-capture`, never over source fixtures.
A second capture was compared byte-for-byte against all committed candidate
streams and oracle files. Review changes before copying them to
`src/test/resources/vttest/`. The upstream permission notice is retained there
as `UPSTREAM-COPYING.txt`.

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./gradlew \
  :1.21.1:test :26.1.2:test :26.2:test --tests '*vttest.*' --console=plain
```

The captured pages need no query/response stub after startup. Interactive report
pages are not claimed as captured; their DSR/DA responses have dedicated unit
tests. Blink and double-size appearance require the separate pixel acceptance,
not just cell grids. This is a finite regression set, not full vttest certification.
