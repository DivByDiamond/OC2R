# New peripherals — owner decisions, 2026-11-02

Planning only: the owner requested a roadmap update, not implementation. The new-device list contains only the eight directions selected by the owner; no additional devices are scheduled. Wireless access points are first; the remaining order is provisional. Mechanics below distinguish the owner's requested direction from suggested details. No new blocks or items are implemented by this update.

## 1. Wireless access point

Plan first for future implementation. Proposed architecture: an access point bridges the existing wired Ethernet network to nearby access points in the same dimension. Computers retain their existing network card and Linux TCP/IP tools; this first block does not require a new guest driver. Connect a network connector to each access point to bridge two wired segments.

Proposed MVP (not yet finalized): configurable network name and shared key, server-authoritative configuration, configurable radius (initially 64 blocks), deterministic delivery inside the radius, bounded packets/frame size and TTL. No dimension crossing, chunk loading, real-world network access or simulated packet loss. Matching keys select a private in-game network; this is not cryptographic encryption. Two access points communicate directly; a chain requires wired interconnection between radio segments rather than implicit radio repeaters.

Acceptance: real block/capability registration and placement, two wired endpoints exchange Ethernet frames through the radio, range/key/dimension isolation, cycles remain bounded, chunk unload/removal/reload cleans registrations, settings survive save/reload. Keep the driver/API documentation and recipe/model/localization with the feature. Height bonuses, GUI scanning and moving-construction acceptance are details to consider when designing this block.

## 2. Printer

Proposed design, subject to agreement before implementation. Text first: paper + black dye produce a titled page item. A program opens a page, writes text at a bounded cursor position, then commits it. An unfinished page survives world reload; committing consumes materials once and only when the output slot can accept the result. Inventory automation feeds paper/dye and extracts pages. Pages can be collected into a readable book while retaining order. No screenshot/raster printing in the first version.

Use one shared page/job backend for RPC and the native interface described in devices-storage §50. Define page dimensions, Unicode policy and ink cost before implementing. Test full output, interrupted jobs, save/reload and repeated commit requests.

## 3. Positioning beacon (OC2R navigation)

A physical reference station advertises its ID, dimension, configured site name and known world coordinates, and answers range requests over the OC2R wireless network. A navigation program gathers multiple stations and computes position; one station can provide a named destination and distance without pretending it determines a unique 3D location. Provide a reference Linux service/client and robot navigation examples. Do not require four full computers just to host four beacons. No administrator omniscient fallback in the normal survival mechanic. Define coordinate frames before supporting ships/contraptions.

## 4. Environment sensor

A bus peripheral reports local biome, light, time, weather and temperature data with documented units and sampling intervals. Only query loaded surroundings. Events report threshold changes; avoid a per-tick scan of every attached sensor. It is not a replacement for the robot scanner module.

## 5. Document reader

An inventory peripheral reads books and printed pages into guest text, retaining page order and title/author where present. Start with Minecraft book components and the printer's page format. No OCR or arbitrary client filesystem access. Feed documents using normal inventory automation.

## 6. Media player and writable music discs

The owner-approved workflow is PC file import (for example MP3) -> record a physical custom music disc -> play it in the media player. Retain ordinary Minecraft disc playback too. Reuse the existing speaker/sound path for custom audio. Specify bounded import duration/size, decoding, world-local audio blob persistence, multiplayer synchronization and disc copying/deletion semantics before implementation. A disc stores a stable media reference, not megabytes of audio in its item data. Imported audio must survive world/server restart. This is a separate feature from projector jcodec removal.

## 7. Long-range radio antenna

After access points: a dedicated large antenna extends range between fixed sites. Range depends on a documented height/open-sky rule, with server caps and an energy budget. Keep the same Ethernet medium and dimension restriction. No new cable tiers. Model and installation mechanics need a separate design pass.

## 8. Creative command chip

Use an installable computer chip/card, not a separate command computer block. It enables a documented command API only while installed. The item is creative/admin-only, has no survival recipe and is disabled by default in server configuration. Server-side authorization and an explicit command permission ceiling remain mandatory: inserting an item must not turn an arbitrary creative player or guest VM into a server operator. Specify execution identity, audit logging, rate limits and revocation on removal/reload.

## Existing hardware — avoid duplicates

- **Robot charger already exists:** `ChargerBlockEntity` charges neighboring blocks and nearby entities. No separate robot dock is scheduled.
- **Redstone relay already exists in substance:** the redstone interface block exposes analog/bundled input and output and change events over the device bus. Reuse and document it instead of adding a second block with the same purpose.

Status: roadmap only. The owner requested wireless access point planning, an adapted positioning concept, environment sensor, document reader, writable audio discs/media player, long-range antenna and a creative command chip. Printer mechanics and numerical radio defaults are proposals, not finalized requirements. No implementation, release tag or platform upload is authorized by this plan.
