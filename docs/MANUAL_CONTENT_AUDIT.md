# English and Russian handbook audit (2026-10-18)

The English and Russian pages were checked against the current shared sources,
recipes and the actual `sedna-buildroot-0.2.10.jar` image. Russian now has the
same page set as English, including crafting/scanner modules, robotics, CPU,
GPU, Internet card, Internet gateway and speaker. Russian interface keys and
embedded-library warning messages are complete. Chinese translations retain
upstream content and were not part of this Russian-language revision.

## Corrections and evidence

| Area | Current source/evidence | Result |
| --- | --- | --- |
| First boot, CPU/RAM | `ComputerBlockFactory`, `ComputerRecipes`, `VMLifecycle`, `Items` | Require a CPU; recommend 32 MiB as a starting configuration, rather than asserting an unmeasured minimum. Survival recipe includes Linux firmware. |
| Firmware, hard disks | `MinuxFirmware`, `BuiltinDevices`, `BuiltinRootFilesystem`, `StorageRecipes` | Small boot ROM loads the kernel from a built-in writable root disk. No additional Linux hard drive is required. Firmware recipes distinguish Linux, OnyxOS and blank chips. |
| Monitor/GPU/projector | `MonitorDevice`, `GPUItem`, `Items`, `ProjectorDevice`, `MonitorMultiblock` | Monitor requires GPU and uses its tier resolution; projector remains 640x480. Multiblock limit remains 8x8. |
| Flash writer | `FlashMemoryFlasherBlock`, `FlashMemoryFlasherBlockEntity`, `FlashWriterService` | Describe the GUI, local/bundled images, target validation and the running-VM exclusion. |
| Linux utilities | Read-only `debugfs` inspection of `generated/rootfs.ext2`; `src/main/scripts` | Remove instructions to execute absent `setup-network.lua`, `lsdev.lua`, `flash.sh`, `import.lua`, `export.lua`. Describe present Linux tools and direct RPC methods. |
| Lua and other guest clients | `src/main/scripts/lib/{lua,micropython,rpc}`, rootfs Lua modules | Document `LUA_PATH`, `/dev/hvc0`, `OC2R_BUS_PATH`, device discovery, `findAll`, colon-vs-dot calls. |
| Robot and modules | `RobotDevice`, `robot.lua`, `CraftingModuleDevice`, `ScannerModuleDevice`, `InventoryOperationsModuleDevice`, `BlockOperationsModuleDevice` | Separate enqueue success from completion; correct void slot setter, zero-based inventory slots, positioning wrappers, scanner numeric arrays and missing translations. |
| Sound | `SoundCardItemDevice`, `SpeakerBlockEntity`, `ToneAudioStream`, `PcmAudioStream`, `UnsignedByteArrayJsonSerializer` | Add tone/PCM APIs, actual bounds/rate/encoding, numeric arrays and throughput limitations. Speaker has no sound-effect lookup/play methods. |
| Networking | `NetworkConnectorBlockEntity`, NIC/tunnel devices, `InternetManagerImpl`, config | Local LAN setup is separate from host-backed Internet. DHCP requires a server; Internet is disabled by default. |
| Import/export | `FileImportExportCardItemDevice`, request manager and messages | Document the real transfer state machine, terminal-user requirement, numeric byte chunks and export size limit. |
| Other existing pages | bus cable/interface, charger, disk drive, keyboard, network hub, redstone devices, wrench, manual item and their providers | Reviewed against implementation; retained applicable behavior and API. Fixed the floppy's computer link and inventory parameter typo. |

## Validation boundaries

`ManualContentTest` checks English/Russian page parity, local document/image
links, placeholders and localization keys. `ManualFontAssetTest` checks the
bundled ASCII atlas. Navigation, layout and registry tests exercise the embedded
manual itself.

Live manual acceptance covers opening the real item in a disposable world,
three tabs, a link, back navigation, scrolling, formatting and block/item/tag/
texture rendering. Screenshots and result files are generated under
`build/reports/manual-qa/`. The Russian run additionally resolves and lays out
every Russian document without English fallback.

Source/content inspection is not a live test of every peripheral, every Linux
command or every server configuration. Projector/world persistence acceptance
and the remaining Stage 3 work are tracked separately in
[stage3-acceptance.md](roadmap/stage3-acceptance.md).
