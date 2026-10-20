# Stage 3 acceptance

Stage 3 is still open. Compilation and passing unit tests alone do not establish
client rendering or feature parity. The supported implementation currently has
four targets: NeoForge 1.21.1, 26.1.2 and 26.2, and Fabric 1.21.1. Fabric 26.x and
the six-target release matrix remain separate work.

## Automated checks

Run from the repository root:

```bash
bash scripts/verify-stage3.sh --console=plain
```

This assembles all four targets, runs the NeoForge unit suites and all server
GameTests. Static analysis is separate: run `:<version>:build` or the repository's
lint tasks for those checks. The NeoForge `gameTest` wrapper requires a fresh,
nonempty TSV and writes JUnit XML under
`versions/<version>/build/test-results/gameTest/results.xml`.

Check the server logs as well as test counts. A test can report success while
the VM worker logs an exception. In particular, no `Payload ... may not be sent
to the client!` errors are acceptable. A vanilla mock server player skips the
configuration handshake: use `TestSupport.serverPlayer` for 26.x tests that add
players to the level, so NeoForge's real mock-channel setup is applied before
computer and robot tracking broadcasts reach them.

## Remaining client acceptance

Use disposable worlds on both NeoForge 26.x targets. Record screenshots, logs
and the version for each completed check. The existing dated evidence is in
[multiloader.md](multiloader.md#26x-rendering-notes-2026-10-14); generated images
are under `build/reports/render-qa/` and are not committed.

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
but the allocation cleanup path needs a separate reproducible investigation
before treating robot reload/shutdown as clean.

- [ ] Monitor models: each facing, single block and multiblock edges/corners;
  framebuffer alignment, GUI image and powered/unpowered states.
- [ ] Bus cable models: straight, bends, branches, six interface directions,
  facades and facade removal on an already-loaded chunk.
- [ ] Network cables: connect/disconnect on loaded chunks and after reload.
- [ ] Robot item: inventory, held item, dropped item and placement; robot model
  and inventory/terminal screens after placement and reload.
- [ ] Storage item default/dyed colours and item model properties; NIC face
  overlays for each enabled/disabled face.
- [ ] Computer and robot terminals: background, text, cursor, clipping,
  scrolling, GUI scaling and in-world depth on both 26.x versions.
- [ ] Projector on 26.2: real guest frames, terrain occlusion, camera movement,
  power loss/restoration and world reload; the reversed-Z port compiles but
  still needs live acceptance.
- [ ] Projector entity/particle occlusion on both 26.x versions; implement and
  verify depth from block-entity models.
- [ ] Warm VM CPU/RAM continuation and framebuffer retention after world reload.

## Remaining implementation

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

Keep Stage 3 open until the required implementation and client acceptance are
complete. Stage 4 owns publishing and the expanded release matrix.
