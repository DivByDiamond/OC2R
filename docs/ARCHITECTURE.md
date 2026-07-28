# Architecture Overview

## Device Bus Graph

The central architectural pattern is a **device bus** — a graph of interconnected `DeviceBusElement` nodes. A `DeviceBusController` scans reachable elements from a root, aggregates all exposed devices, and presents them to the virtual machine.

```
 BusCable ── BusCable ── Computer
    │                     │
    │                     ├── CPU (item slot)
    │                     ├── Memory (item slot)
    │                     ├── HardDrive (item slot)
    │                     ├── NetworkCard (item slot)
    │                     └── ...
    │
 InternetGateway      Monitor
```

Scanning is lazy: a `scan` is scheduled on bus changes and runs during the game tick. The controller transitions through states:

```
SCAN_PENDING → SCANNING → READY
                            ├── INCOMPLETE (some devices missing)
                            ├── TOO_COMPLEX (too many elements)
                            └── MULTIPLE_CONTROLLERS (conflict)
```

## Three Device Layers

Devices exist at three levels of abstraction:

| Layer | Interface | Mechanism | Use Case |
|-------|-----------|-----------|----------|
| VMDevice | `VMDevice` | MMIO + interrupts | Disk, keyboard, monitor, network cards |
| RPCDevice | `RPCDevice` | JSON-RPC over VirtIO serial | CPU, energy, fluid, inventory, redstone |
| ObjectDevice | `@Callback` | Reflection-based RPC | Convenience layer for `RPCDevice` |

### VMDevice (low-level)

Registers memory-mapped IO regions and interrupt lines. Requires kernel drivers in the guest. Lifecycle: `mount()` → `unmount()` → `dispose()`.

### RPCDevice (high-level)

Uses a VirtIO serial port for JSON-based RPC communication. No kernel driver needed — the guest communicates via a simple serial protocol. Methods are discovered through reflection (`@Callback` annotations) or explicit `RPCMethod` declarations.

### FirmwareLoader

Special devices that provide boot firmware (OpenSBI, Linux kernel) to the VM during initialization.

## VM Lifecycle

```
STOPPED → LOADING_DEVICES → RUNNING → STOPPED
```

Each tick when the bus is `READY`, the lifecycle manager proceeds through:

1. **Energy check** — verify sufficient power
2. **Firmware check** — ensure at least one `FirmwareLoader` is present
3. **CPU check** — detect a CPU device
4. **VM device mount** — call `mount()` on all `VMDevice`s
5. **Board init** — initialize the RISC-V board (load firmware, reset CPU)
6. **RPC device mount** — register RPC devices with the bus adapter
7. **Run** — start the VM runner thread

Any error halts the machine with a displayable message.

## Video Pipeline

```
VM → Framebuffer → H.264 Encoder → Network → H.264 Decoder → Render
     (jcodec)       (packet)        (jcodec)      (OpenGL)
```

- Bundled jcodec library for H.264 encode/decode
- Worker thread pools for encoding/decoding
- Load balancers distribute work across monitors/projectors
- Custom GLSL shaders for projector depth rendering and color compositing
- Dynamic texture with NEAREST filtering for terminal text

## Persistence

[Ceres](https://github.com/fnuecke/ceres) annotation-based serialization saves the full VM state to NBT:
- RISC-V board state (CPU registers, RAM, devices)
- `GlobalVMContext` (allocated interrupts, memory ranges)
- `BuiltinDevices` (UART, RTC, block devices)
- RPC device registry
- VM device state (mounted devices)

## Threading Model

| Thread | Work |
|--------|------|
| Minecraft main thread | Tick logic, energy, bus scanning |
| VM thread pool (daemon) | RISC-V CPU execution (500ms slices) |
| VXLAN thread (daemon) | Native ICMP/UDP receive |
| Internet thread | Network stack integration |
| Encoder/Decoder pools | H.264 video processing |

## Related

- [Device Types](DEVICES.md) — detailed device layer documentation
- [Networking Stack](NETWORKING.md) — TCP/IP and VXLAN internals
- [Source Structure](SRC_STRUCTURE.md) — code layout
