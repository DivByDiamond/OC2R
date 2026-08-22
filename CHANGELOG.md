# Changelog

All notable changes to OC2R are documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Fixed

- **Network**: server crash (`AssertionError`) when using a Network Cable on two connectors that are already linked — now shows an "already connected" message instead (issue #18)

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
