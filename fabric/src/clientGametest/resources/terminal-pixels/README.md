# Terminal pixel goldens

These are reviewed, cropped screen surfaces from real Minecraft clients, captured
on 2026-11-02. `provenance.json` records the profile, font hashes and image hashes.
Comparison is exact RGB: a single differing pixel fails and writes `.actual.png`
and a magenta `.diff.png` beside the full screenshot. No tolerance, resize or
reference auto-update is performed by a normal run.

- `26.1.2` / `26.2`: GUI body `(106,48,640,384)` and actual block screen
  `(247,117,360,210)` in an 854x480 window. The client boots Linux; guest printf
  supplies DHL/DWL/SWL, colors, underline, strike and DEC box drawing. The world
  capture follows an attribute-only cache probe; GUI restores the DWL row. A
  steady cursor is placed at column 40, so blink timing does not change pixels.
- `1.21.1`: the 640x384 terminal at `(106,48)` in a dedicated client screen.
  `legacy-gui` uses the widget's explicit orthographic projection;
  `legacy-world-path` uses the real retained VertexBuffer/model-view world branch.
  The two reviewed images are identical. This isolates the old GL renderer; it
  does not verify a placed block's camera/depth integration on 1.21.1.

The modern scenario also freezes noon/clear weather and gamma 0.0 to remove lighting drift.

Use Java 25 to launch Gradle, Xvfb, and Mesa software GL. Gradle selects the legacy
Java 21 toolchain. Sound is disabled before startup and inside the scenario.

```sh
bash scripts/terminal/verify-client-pixels.sh
```

The script rejects draft mode. To author candidates, explicitly set
`OC2R_TERMINAL_PIXEL_DRAFTS=true` on the corresponding client command from
`docs/roadmap/terminal-double-size-acceptance.md`. Candidates are written only in
build/run, never here. Inspect the full screenshots and crops before copying
candidates and updating provenance. Both source and pixel goldens remain subject
to normal code review before merge.

The unavailable `vt420.png` belongs to the historical audit. The current acceptance uses
pinned upstream cell captures and these repeatable pixel references; no comparison with that
old image is claimed or required.
