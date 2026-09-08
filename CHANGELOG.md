# Changelog

All notable changes to OC2R are documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Added

- **Terminal**: non-destructive column change — DECSCPP (`CSI Pn $ |`) selects 80 or 132 columns via a new `resizeWidth` primitive that copies existing contents into the surviving columns instead of clearing: screen content, scroll margins, cursor position and tab stops are preserved (new columns are default-initialized, matching xterm's resize). Private-marker forms (`?`/`>` prefix) are ignored, as xterm does. DECCOLM (mode 3) remains the destructive reset (#38)
- **Terminal**: per-instance color palette — OSC 4 (query/set an indexed color) and OSC 104 (reset one or all indexed colors to default) are implemented; "computed dim" derives SGR 2 (dim) from the palette instead of a fixed table (#31)
- **Terminal**: CSI cursor-positioning coverage completed — CBT/CHT (backward/forward tabulation), CNL/CPL (cursor down/up + CR), VPR/HPA/HPR (vertical/horizontal position, absolute/relative), RCP and CSI ?u (XTRESTORE, save/restore private mode state), REP (repeat preceding char) (#24)
- **Terminal**: `XT_RAW_PASSTHROUGH` mode (CSI ?7777h/l) — an in-band byte-capture debugger that renders every received byte as a visible glyph without interpreting it, for diagnosing what a guest program actually sends
- CI: `lintRatchet` task (count-based ratchet for Checkstyle/PMD, baseline 0) and SpotBugs baseline file, so lint regressions fail the build going forward

### Changed

- **Terminal**: OSC/DCS/APC string-sequence termination (ST, BEL, CAN/SUB abort, `ESC` followed by a non-`\`) is now handled uniformly by a single `TerminalOutput.handleStringByte` state machine instead of divergent per-manager logic, mirroring xterm's dispatch and closing a stuck-DCS/APC griefer vector
- **Terminal**: cursor save/restore (DECSC/DECRC, SCOSC/SCORC, `SAVE_CURSOR`/`SAVE_CLEAR_AND_SWITCH`) unified into a single `SavedCursor` helper — SCORC and DECRC can no longer silently diverge, and the pending-autowrap flag is now correctly saved and restored
- **Terminal**: `Terminal.SCROLL_BACK_COUNT` is `static final` instead of a mutable public field, removing a source of buffer/index mismatches
- Dead code removed and visibility tightened across the terminal module (unused `SessionOperator`/`ColorUtils`/`RunnableUtils`, unused `TerminalIO.putOutput(byte)`, `TerminalBuffer.shiftUp/shiftDown(int)`, `Utf8Decoder.hasActiveSequence()`, unused `Glyph` metrics fields; several render/mode methods made private)
- Video encoding moved off the server thread onto an async last-frame-wins encoder; internet tick, frame buffers and the VXLAN send path optimized (idle-skip, pooling, caching, report-once warnings)
- Block entity sync consolidated onto a single channel, dropping duplicate payload messages
- Linting is stricter by default: Checkstyle/PMD/SpotBugs now fail the build instead of only reporting, and Error Prone runs on by default with a curated set of checks promoted to errors
- Sedna updated to 3.1.0 (patched minux with working 9p support) (#29)

### Fixed

- **Terminal**: xterm-256 color cube used `0xdf` instead of `0xd7` for the 4th cube level — a typo that shifted several palette colors (#30)
- **Terminal**: `DSR` (`CSI n`) with no parameter now replies with a status report (`\033[0n`) instead of hanging — ECMA-48 treats the omitted default as `Ps=5`
- **Terminal**: CSI cursor-move argument saturation (`Integer.MAX_VALUE`) could overflow when added to the current position; counts are now clamped before the add (#27)
- **Terminal**: `REP` (repeat preceding character) with a huge count could freeze the VM worker under the IO lock; clamped to one screen's worth of repeats
- **Terminal**: `HPA` moved the cursor under DECOM origin mode instead of keeping the row fixed
- **Terminal**: `DECRC` restored a saved cursor column beyond the current width after a `DECCOLM` shrink, causing a false line wrap on the next character; restore now clamps like every other cursor-move path
- **Terminal**: `ESC )` (designate G1 charset) was writing into G0 instead of G1, making the G1 slot unreachable — both designators now route to their own charset field
- **Terminal**: a truncated true-color/256-color SGR sequence (e.g. `38;2;1`, missing RGB components) leaked its leftover byte into being applied as an unrelated style attribute (e.g. bold); the whole incomplete color spec is now discarded instead
- **Terminal**: the per-row dirty bitmask could silently wrap (`1 << dirtyLine` on a 32-bit int) when writing far back into scrollback, flipping an unrelated bit instead of the row that changed; out-of-range rows now force a full redraw instead
- **Terminal**: `FontHandling`/`UnicodeFontRenderer` are now annotated `@OnlyIn(CLIENT)`, so an accidental import from common code fails fast at class-load instead of risking a dedicated-server crash inside `Minecraft.getInstance()`
- Internet card MAC address is now derived from a stable per-card UUID instead of being read from NBT (also closes a spoofing/collision risk)
- Rate limits added for PCM/keyboard/framebuffer network messages; ICMP echo handling made non-blocking
- VXLAN manager could throw after shutdown in some races; default host settings sanitized

## [0.1.1-beta.2] — 2026-08-24

### Added

- **GPU**: graphics card items (4 tiers: 320×200 / 640×400 / 1024×768 / 1920×1080) are now required for a monitor to show a framebuffer. Without a GPU the monitor stays dark but the computer, keyboard and UART terminal keep working; swapping a GPU re-creates the framebuffer at the new resolution (stale framebuffer contents are discarded)
- **Terminal**: server-authoritative screen sync — the client no longer receives raw UART bytes and re-parses VT100; the server ships only changed screen rows as diffs (full snapshot on VM restart), so several players watching one computer see a consistent screen and new viewers start from the current content
- **OnyxOS**: updated kernel + root filesystem — monitors show the OnyxOS console (framebuffer via FDT `simple-framebuffer`, RGB565), login accepts the in-game terminal's Enter key, root logs in with an empty password on first boot

### Changed

- **Video**: monitor/projector frames are transferred as raw RGB565 in 256 KB chunks — no encode/decode thread pools, no byte budgets; frame rate configurable via `monitorFps` (1–60, default 20). Vendored jcodec (H.264/YUV420) is restored from repo history (with attribution) and used only when `videoCodec=h264`
- **Monitor**: client-side texture is created at the resolution of the incoming frame instead of a fixed 640×480; text/GUI rendering scales with the actual GPU resolution
- **Terminal**: terminal state is owned by the server; keyboard/mouse mode flags (application cursor keys, mouse reporting, bracketed paste, focus events) travel with every diff, keeping input forwarding intact without client-side VT parsing

### Fixed

- **Terminal**: crash (AIOOBE) that permanently froze terminal output when scrolling down/reverse-indexing with a full scrollback buffer
- **Terminal**: freeze/DoS — `CSI S`/`CSI T` with a huge counter saturated the parser into minutes of locked work; counters are clamped to the screen height
- **Computer**: terminal output stopped entirely after the diff-sync refactor (UART bytes were dropped before reaching the server-side parser)
- **Network**: server crash (`AssertionError`) when using a Network Cable on two connectors that are already linked — now shows an "already connected" message instead (issue #18)
- **Sound**: incompatibility with C2ME — `ThrottledSoundEmitter` used the level-owned random source from the VM runner thread, tripping C2ME's thread-ownership check and halting the boot process; it now uses a dedicated `RandomSource` (issue #22)
- **Terminal**: the bell sound replayed on every keystroke after firing once — the pending-bell flag is now consumed when capturing each terminal diff snapshot and is delivered exactly once (issue #23)

### CI/CD

- Releases are automatically uploaded to CurseForge ([OpenComputers II: Modern](https://www.curseforge.com/minecraft/mc-mods/opencomputers-ii-modern)) on publish

## [0.1.1-beta.1] — 2026-08-21

### Added

- **Terminal**: text rendering — bright foreground on bold (bold-bright), blink, underline, full-screen inverse (DECSCNM) (4494906)
- **Terminal**: insert mode (IRM) — typed characters shift the line right instead of overwriting (67a0d65)
- **Energy**: cable networks distribute energy across the network with a per-tick buffer and limit; creative energy block as an infinite source (383c4ec, 8168a1f)
- **Sound**: Speaker block and sound card — tone generator (`beep`/`playTone`) and PCM streaming from the guest (383c4ec)
- **Monitor**: fragment-based multiblock model replacing OBJ; assemble 2×2/3×3 in any order, breaking a single block repartitions the rest without losing state (8168a1f)
- **CPU**: configurable processor frequencies and VM time quota in the config (89aaf6c)
- **Guest development**: C RPC examples (redstone blink, note block player) and a C++ RAII wrapper for `librpc` (da00ae8)
- **Monitor**: configurable client-side border `monitorBorder` (fe9cded)

### Fixed

**Network / internet**
- ICMP replies instead of silent packet loss: fragmentation needed for fragments (large DNS responses no longer time out), Time Exceeded on TTL exhaustion; port-unreachable now comes from the unreachable host's address instead of `0.0.0.0` (2d4f38c)
- Ping no longer looks broken on dedicated servers: false negatives from the JVM fallback (`CAP_NET_RAW`) are logged with a one-shot WARN (2d4f38c, issue #13)
- MAC addresses containing bytes ≥ `0x80` are parsed and formatted correctly; ICMP-unreachable carried an all-zero packet quote (2d4f38c)
- JVM crash on TCP reads when running with `-ea` (debug `assert false`) (2d4f38c)
- Internet card enabled while VXLAN is disabled now logs a WARN instead of failing silently (2d4f38c)

**Energy and blocks**
- Energy only reached the first cable neighbor ("ping-pong" buffers) — network-wide distribution once per tick (8168a1f, 75c8cc4)
- Server crash when loading a chunk with a network switch; empty-frame spam from connectors that crashed switches (8168a1f)
- Monitor re-encoded frames every tick even without changes (8168a1f)
- Bundled redstone wrote and read the signal on opposite block faces (8168a1f)
- Screen positioning was off on multiblock monitors; monitor connects to the bus from any face (fe9cded, a5ba9cc)
- OnyxOS: kernel crash at boot with a network card installed (virtio-net) — updated `onyx-kernel.bin` (c9db904)

**Terminal (VT100/vttest)**
- CSI argument parser and cursor control brought to VT100 spec (57b7581); insert/delete char/line (ICH/DCH/IL/DL) per vttest suite 8 (d36e579)
- Scrolling IND/RI/NEL only at scroll-region boundaries; line/line clearing, tab stops (width 8), backspace at pending-wrap, DECOM origin mode (80bb076, b921a05, fb4c55a, c78643f, e92000b, f6b1b44)
- Clearing a line no longer resets the text color; SGR 38/48 no longer drops trailing parameters; ANSI/256-color palettes synchronized; font red/blue channels were swapped (e7d352e, 80bb076, adaba93, 32a7521, 943e39c)
- DECSC/DECRC save and restore style, charset and colors, not just the cursor; RIS fully resets saved state and parser state (cc10421, ed77e0f)
- Malformed extended-color SGR sequences no longer enable blink/dim; out-of-range 256/RGB values are clamped (c9f254d, 72abd63)
- Stale-line rendering artifacts after scrolling into scrollback and IL/DL/SU/SD (dirty-mask) (a86fafb, 8a673c9, c858266, 62dfb63)
- The cursor is correctly constrained to the scroll region on absolute movement (099b88e, 8644c6a, 6dd4415)
- Keyboard input is encoded as UTF-8 — Cyrillic and clipboard paste no longer turn into mojibake (be838ab)
- Utf8Decoder no longer drops control characters interrupting multi-byte sequences (8338484)
- Terminal screen data is no longer written to chunk NBT (~512 KiB → ~2 KiB) (71fb105)
- Double UV calculation for the square glyph in the font atlas (71fb105)

### Changed

- Dependencies updated: NeoForge 21.1.248, JEI 19.44, ProjectRed 4.23.0, ceres 0.0.6, sedna-buildroot 0.0.70 (0.0.71 rolled back: it removed 9p from the guest kernel, which broke `/mnt/builtin` — minux issue #12), Mockito 5, JUnit 5.13 (ed75650)
- Versioning switched to a Modrinth-friendly format, mod metadata updated (109f6e9)
- Item textures reorganized into folders; bilinear filtering disabled on the monitor screen — crisp pixels (a5ba9cc)
- Speaker received a Charger-style model/texture (8168a1f)
- Default parameters of all CSI sequences normalized per VT100 (f3f4501)
- Internal: ScreenRegistry DSL for screen registration, unified private-mode table (ModeTable), dirty-layer separation, +29 inet unit tests (67b2a8b, 6135a0c, f3f4501, 2d4f38c)

### Docs

- Guest OS networking guide: [docs/NETWORKING.md](docs/NETWORKING.md) (08c9a60)
