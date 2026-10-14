# Source Code Structure

- [Modules and versions](#modules-and-versions) — `core`, root (NeoForge), `fabric`, `versions/`
- [API](#api) — public interfaces for addon development
- [Common](#common) — core logic: blocks, entities, VM, networking
- [Client](#client) — rendering, GUI, audio
- [Data](#data) — data generators for recipes, loot tables, models
- [Resources](#resources) — textures, models, sounds, localisation
- [Scripts](#scripts) — guest VM scripts (Lua, MicroPython, C)
- [Tests](#tests) — JUnit 5 test suites

---

## Modules and versions

| Module | Path | Contents |
|--------|------|----------|
| `core` | `core/src/` | Loader-independent code, no NeoForge/Fabric imports: `platform/` bridges (registries, network, menus, capabilities, item handlers, energy, common and client events, environment), `bus/` topology, `network/` wire geometry. Tests in `core/src/test` |
| root (NeoForge) | `src/` | The mod itself: `api`, `common`, `client`, `data`, `gametest`, `jcodec`, NeoForge `platform/` implementations, resources, guest scripts |
| `fabric` | `fabric/src/` | Fabric entrypoints, bridge implementations, Fabric mixins and client models; also compiles the shared `common`/`client` sources from the root project. `src/gametest` holds Fabric-only GameTests |
| versions | `versions/<mc>/`, `core/versions/<mc>/`, `fabric/versions/1.21.1/` | Stonecutter nodes with per-version `gradle.properties` |

Supported nodes: `1.21.1` (NeoForge and Fabric, released), `26.1.2` and `26.2` (NeoForge only, in development; Java 25). Version differences inside shared sources are marked with Stonecutter `//? if` conditionals, and on 26.x `ResourceLocation` is replaced by `Identifier`. See [MULTILOADER.md](MULTILOADER.md) and [roadmap/multiloader.md](roadmap/multiloader.md#42-multiloader-neoforge-and-fabric-and-multiversion).

---

## API

`li.cil.oc2.api` — published as a separate artifact for addon authors.

| Package | Description |
|---------|-------------|
| `bus/` | Device bus graph: `DeviceBus`, `DeviceBusElement`, `DeviceBusController` |
| `bus/device/` | Device hierarchy — `Device`, `VMDevice`, `RPCDevice`, `ObjectDevice` (`@Callback`) |
| `bus/device/provider/` | `BlockDeviceProvider`, `ItemDeviceProvider` |
| `bus/device/vm/` | Low-level `VMDevice`, `FirmwareLoader`, `VMContext` |
| `bus/device/rpc/` | JSON-RPC device interfaces (`RPCDevice`, `RPCMethod`, events) |
| `bus/device/object/` | Reflection-based RPC via `@Callback` annotations |
| `capabilities/` | Minecraft capability interfaces (`NetworkInterface`, `Robot`, etc.) |
| `inet/` | Internet API: layers (`NetworkLayer`, `TransportLayer`), providers, sessions |
| `util/` | `Side`, `Registries`, `RobotOperationSide` |

See [DEVICES.md](DEVICES.md) for detailed device documentation.

---

## Common

`li.cil.oc2.common` — server + client shared logic. Core classes:

| Class | Role |
|-------|------|
| `Main.java` | `@Mod` entry point, registers blocks, items, entities, containers, providers |
| `CommonSetup.java` | `FMLCommonSetupEvent` — integrations, internet manager, type adapters |
| `ConfigManager.java` | Configuration |
| `Constants.java` | Byte sizes, CPU freq (25 MHz), error messages, NBT keys |
| `NativeLoader.java` | Loads `oc2rnet` native library per platform |

### `block/` — Block definitions

| Subpackage | Blocks |
|------------|--------|
| `computer/` | `ComputerBlock` — main computer case |
| `cable/` | `BusCableBlock` — device bus cabling with state properties, shapes |
| `monitor/`, `projector/` | Display blocks |
| `network/` | `NetworkConnectorBlock`, `NetworkHubBlock`, `NetworkSwitchBlock`, `VxlanBlock` |
| `disk/` | `DiskDriveBlock`, `FlashMemoryFlasherBlock` |
| `energy/` | `ChargerBlock`, `CreativeEnergyBlock` |
| `misc/` | `InternetGatewayBlock`, `PciCardCageBlock`, `RedstoneInterfaceBlock` |

### `blockentity/` — Block entity implementations

| Subpackage | Key entities |
|------------|-------------|
| `computer/` | `ComputerBlockEntity` + sub-managers for terminal, persistence, VM, items |
| `monitor/` | Full video pipeline: encoder, decoder, state manager, workers |
| `projector/` | 3D projection pipeline |
| `network/` | BusCable, NetworkConnector/Hub/Switch, Vxlan (28 files) |
| `energy/`, `disk/`, `keyboard/`, `misc/` | Charger, disk drive, keyboard, redstone, etc. |

### `bus/` — Device bus system

| Subpackage | Description |
|------------|-------------|
| `controller/` | `CommonDeviceBusController`, `BusState`, scanning logic |
| `adapter/` | `RPCDeviceBusAdapter` — JSON-RPC over VirtIO serial |
| `device/` | Device implementations: RPC devices (CPU, energy, fluid), VM devices (disk, network, keyboard) |
| `device/provider/` | 20+ providers for all item/block types |
| `device/data/` | `FirmwareRegistry`, `BlockDeviceDataRegistry` |
| `element/` | Abstract bus element base classes |

### `vm/` — Virtual machine core

| Subpackage | Description |
|------------|-------------|
| root | `AbstractVirtualMachine`, `VMRunner`, `BuiltinDevices` |
| `context/` | `GlobalVMContext`, `ManagedVMContext` — resource allocation |
| `lifecycle/` | `VMLifecycle` — orchestration: energy → firmware → CPU → mount → board init → run |
| `state/` | `SerializedState` — Ceres serialization |
| `terminal/` | Full VT100/ANSI terminal emulator. 30+ CSI sequences, colors, cursor, fonts |
| `device/` | `PciRootPortDevice`, `SimpleFramebufferDevice` |

### `inet/` — Network stack

Full TCP/IP implementation. See [NETWORKING.md](NETWORKING.md).

| Subpackage | Description |
|------------|-------------|
| `layer/` | `DefaultNetworkLayer`, `DefaultTransportLayer`, `DefaultSessionLayer` |
| `protocol/` | ARP, ICMP |
| `tcp/` | `TcpHeader`, TCP state machine (7 states) |
| `session/` | Datagram, stream, echo sessions |
| `internet/` | `InternetManagerImpl`, `InternetAdapter` |
| `vxlan/` | VXLAN tunneling via native `oc2rnet` lib |

### Other packages

| Package | Description |
|---------|-------------|
| `entity/robot/` | Robot entity: movement, inventory, animation, VM |
| `item/` | Item definitions: CPU, memory, drives, cards, wrench |
| `container/` | 23 container classes for all UIs |
| `network/` | Network messages, load balancers (monitor/projector) |
| `serialization/` | Ceres, Gson, NBT serializers |
| `config/` | Configuration specs (common, client, energy, VM, VXLAN) |
| `mixin/` | 5 NeoForge-side mixins (Frustum, LevelRenderer, Minecraft, ServerChunkCache, ServerGamePacketListenerImpl); Fabric has its own set in `fabric/` |
| `integration/` | JEI, ProjectRed, wrench support |
| `util/` | Scheduler, NBT utils, sound, text, world helpers |

---

## Client

`li.cil.oc2.client` — client-side only (rendering, GUI, audio, manual).

| Package | Description |
|---------|-------------|
| `ClientSetup.java` | Registers renderers, model loaders, color handlers through the `ClientRegistrar` bridge |
| `renderer/blockentity/` | Computer, monitor, charger, projector, gateway, disk drive renders |
| `renderer/entity/` | `RobotRenderer` + `RobotModel` |
| `renderer/font/` | `MonospaceFontRenderer` |
| `gui/screen/` | Terminal, container, monitor, robot, network, file screens |
| `gui/widget/` | Terminal, monitor, power/inventory buttons |
| `model/` | `BusCableBakedModel`, `BusCableModel` |
| `audio/` | `LoopingSoundManager` — computer running sound |
| `manual/` | In-game manual (en/ru/zh-CN) |

---

## Data

`li.cil.oc2.data` — data generators run via `runData` Gradle task.

| Class | Generates |
|-------|-----------|
| `ModBlockStateProvider` | Block state JSONs |
| `ModItemModelProvider` | Item model JSONs |
| `ModBlockTagsProvider`, `ModItemTagsProvider` | Block/item tags |
| `ModLootTableProvider` | Loot tables |
| `ModRecipesProvider` + subclasses | Recipes (card, computer, robot, storage, components) |

---

## Resources

`src/main/resources/` — game assets and data.

| Path | Contents |
|------|----------|
| `assets/oc2r/blockstates/` | 17 block state JSONs |
| `assets/oc2r/models/block/` | Block models (cable variants + OBJ monitor) |
| `assets/oc2r/models/item/` | 60+ item models |
| `assets/oc2r/textures/` | Block textures, item sprites, GUI widgets |
| `assets/oc2r/sounds/` | Computer, floppy, HDD `.ogg` sounds |
| `assets/oc2r/fonts/` | Monocraft TTF (Minecraft-style monospace) |
| `assets/oc2r/shaders/` | Projector GLSL (vertex + fragment) |
| `assets/oc2r/doc/` | In-game manual (en, ru, zh-CN) |
| `assets/oc2r/lang/` | Localisation (en, ru, ja, zh-CN) |
| `data/oc2r/` | Recipes, loot tables, tags, file systems, advancements |
| `onyxos/` | OnyxOS firmware + root FS (`fw_jump.bin`, `onyx-kernel.bin`, `onyxfs.img`) |
| `META-INF/` | `neoforge.mods.toml`, access transformer |
| `natives/` | Prebuilt `oc2rnet` libs: linux, macos, windows, android |

---

## Scripts

`src/main/scripts/` — scripts that run inside the guest VM.

| Path | Contents |
|------|----------|
| `bin/` | Lua/shell utilities: `export.lua`, `import.lua`, `flash.sh`, `lsdev.lua`, `redstone.lua`, `setup-network.lua`, `swconfig.lua`, plus terminal colour test scripts |
| `lib/lua/` | Lua device API (`devices.lua`, `robot.lua`) |
| `lib/micropython/` | MicroPython device API (`devices.py`, `robot.py`) |
| `lib/rpc/` | C library for RPC protocol (`rpc.c`, `rpc.h`, Makefile) + examples (`redstone_blink.c`, `note_block_player.c`) and a C++ RAII wrapper (`rpc_raii.hpp`, `example_raii.cpp`) |
| `firmware_files/` | OpenSBI firmware blob (`fw_jump.bin`) |

---

## Tests

JUnit 5 + Mockito. Run `./gradlew :1.21.1:test :core:1.21.1:test :fabric:1.21.1:test`; GameTests: `:1.21.1:gameTest` and `:fabric:1.21.1:runGameTest`.

| Location | Tests |
|----------|-------|
| `src/test/.../common/inet/` | ARP, TCP header, sessions, layers, `Ipv4Space`, checksums and other utils |
| `src/test/.../common/bus/` | `CommonDeviceBusController`, `BlockDeviceBusController`, RPC item devices |
| `src/test/.../common/blockentity/network/` | cable face overrides, switch `PacketProcessor` |
| `src/test/.../common/vm/` | `VMRunner`, terminal (`TerminalBuffer`, vttest golden-grid harness), video |
| `src/test/.../common/util/` | `IntegerSpace` and misc utils |
| `src/test/.../client/audio/` | audio stream buffers |
| `src/test/.../common/ClientImportGuardTest` | guards against new client imports in `common` |
| `src/test/.../platform/` | NeoForge capability bridge |
| `core/src/test/` | `PlatformTest`, bus topology, wire geometry |
| `src/main/java/.../gametest/`, `fabric/src/gametest/` | in-world GameTests |

---

## See also

- [Architecture Overview](ARCHITECTURE.md) — device bus, VM lifecycle, threading
- [Device System](DEVICES.md) — VMDevice, RPCDevice, ObjectDevice, providers
- [Networking Stack](NETWORKING.md) — TCP/IP, VXLAN, native library
- [Multiloader / multiversion](MULTILOADER.md) — module split and Stonecutter
- [Buildroot image](BUILDROOT.md) — guest OS image sourcing, TCC, rebuilding
