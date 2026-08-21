# Changelog

All notable changes to OC2R are documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Fixed

- **inet**: `assert false;` in `DefaultSessionLayer` crashed the JVM on every TCP read when assertions were enabled (`-ea`).
- **inet**: fragmented IP packets (e.g. large EDNS0 DNS responses) were dropped silently; the stack now answers with ICMP Destination Unreachable / fragmentation needed (type 3, code 4, MTU 1500).
- **inet**: packets dropped due to exhausted TTL now get an ICMP Time Exceeded (type 11) reply instead of a silent timeout on the guest side.
- **inet**: ICMP port-unreachable replies were sent with source address `0.0.0.0`; they now carry the address of the unreachable host (`IcmpHandler.reject`, `SendHandler`).
- **inet**: `InetUtils.quickICMPBody` overwrote the quoted packet with zero bytes (`put` instead of `get`), so every ICMP unreachable carried an empty RFC 792 quote.
- **inet**: `MacAddressUtils` mis-parsed and mis-formatted any MAC address containing a byte ≥ `0x80` (signed-byte promotion); e.g. `5E:D1:...` parsed as prefix `0xFFD1`.
- **inet**: internet card enabled while VXLAN is disabled is now reported with a WARN at startup instead of failing silently; misleading config comment corrected.
- **inet**: one-shot WARN when the JVM-level ping fallback (`InetAddress.isReachable`) fails — on dedicated servers without `CAP_NET_RAW` it produces false negatives and made `ping` look broken (issue #13).
- **terminal**: DCH/ICH clamping cleanup in `TerminalBuffer.deleteChars`/`insertChars` — redundant min-1 clamp removed; a future `count <= 0` caller becomes a safe no-op instead of deleting one char (PR #14).
- **terminal**: test literals extracted into `ESC`/`CSI`/`SAMPLE_LINE`/`MARGIN_CONTENT` constants; PMD `AvoidDuplicateLiterals` violations reduced to zero (PR #15).

### Added

- Unit tests for the inet network layer (ICMP error generation, IP header parsing, TTL/fragment/denied-host handling), MAC address parsing/formatting, ICMP handler and session manager: +29 tests (152 total).
- Guest networking guide with addressing/DNS/diagnostics: [docs/NETWORKING.md](docs/NETWORKING.md).

### Changed

- Dependencies updated (all verified against Minecraft 1.21.1 / NeoForge 21.1.x):
  - NeoForge 21.1.233 → 21.1.248
  - ModDevGradle 2.0.124 → 2.0.144
  - Mixin annotation processor 0.8.5 → 0.8.7
  - ceres 0.0.4 → 0.0.6
  - sedna-buildroot 0.0.64 → 0.0.71 (vendored into `libs/`; guest image update)
  - JEI 19.25.1.332 → 19.44.0.403
  - ProjectRed 4.22.0-beta+33 → 4.23.0
  - Mockito 4.3.1 → 5.23.0 (byte-buddy 1.12.x cannot instrument Java 21 classes; inline mock maker is the default in Mockito 5)
  - JUnit Jupiter 5.12.2 → 5.13.4 (+ platform launcher 1.13.4)
- Test classpaths now share the main dependency set so inet-layer tests can load Minecraft NBT classes at runtime.
- Repository declarations got `content` filters so modules resolve only from their own mavens (cursemaven rate-limiting no longer breaks builds).

### Not changed

- sedna stays at 2.0.13 (latest release).
- Parchment mappings stay at 2024.11.17 (latest for 1.21.1).
- Error Prone stays at 2.50.0 (latest).
- CodeChickenLib / CBMultipart dynamic versions already resolve to the latest 1.21.1 builds.

## [0.1.0] — 2026-08-XX

Initial public release.
