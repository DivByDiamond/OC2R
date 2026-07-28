# Device System

OC2R has three layers for adding devices to a virtual computer. Each serves a different use case.

## VMDevice (low-level)

Registers memory-mapped IO (MMIO) regions and interrupt lines directly on the RISC-V board. The guest OS needs a kernel driver to use these devices.

**Lifecycle:**

1. `mount(VMTarget, VMContext)` — allocate resources, register MMIO and interrupts
2. `unmount()` — stop I/O, deregister
3. `dispose()` — free all resources

**Built-in implementations:**

| Device | Purpose |
|--------|---------|
| `DiskDriveDevice` | Floppy disk I/O |
| `HardDriveDevice` | Block storage |
| `KeyboardDevice` | Keyboard input |
| `MonitorDevice` | Framebuffer display |
| `NetworkInterfaceCardDevice` | Ethernet networking |
| `InternetCardDevice` | Internet connectivity |
| `NetworkTunnelDevice` | Network tunnel |
| `MemoryDevice` | RAM expansion |
| `PciCardCageDevice` | PCI device slot |
| `ProjectorDevice` | 3D projection |
| `FirmwareFlashStorageDevice` | Firmware storage |
| `SimpleFramebufferDevice` | Simple framebuffer |
| `PciRootPortDevice` | PCI root port |

## RPCDevice (high-level)

Communicates via JSON-RPC over a VirtIO serial port. No kernel driver needed — the guest runs a simple client that sends JSON method calls and receives JSON responses.

**Interface:**

```java
public interface RPCDevice {
    String getTypeName();
    List<RPCMethodGroup> getMethodGroups();
    Optional<RPCInvocation> getInvocation(RPCMethod method);
}
```

## ObjectDevice (@Callback)

A convenience implementation of `RPCDevice` using reflection. Annotate methods with `@Callback` and they are automatically exposed.

**Example:**

```java
public class MyDevice extends ObjectDevice implements NamedDevice {
    @Callback
    public double getTemperature() {
        return 36.6;
    }

    @Override
    public String getDeviceName() {
        return "Thermometer";
    }
}
```

**Available annotations:**

| Annotation | Usage |
|------------|-------|
| `@Callback` | Expose a method to the VM; supports `synchronize` (main thread), `name`, `description` |
| `@Parameter` | Describe a method parameter |

**Optional interfaces:**

| Interface | Purpose |
|-----------|---------|
| `NamedDevice` | Custom device name |
| `DocumentedDevice` | Extended documentation |
| `LifecycleAwareDevice` | Mount/unmount callbacks |

## FirmwareLoader

Special devices that provide boot firmware. The base interface adds `getFirmware()` returning a `Firmware` object (OpenSBI, Linux kernel, etc.). The lifecycle manager calls `load()` during initialization.

**Implementations:**

- `MinuxFirmware` — loads the `minux` kernel
- `ByteBufferFlashStorageDevice` — loads firmware from a byte buffer

## Device Bus Integration

Devices are exposed to the bus through **providers**:

```java
// BlockDeviceProvider — scan blocks for devices
public interface BlockDeviceProvider {
    void getDevices(BlockDeviceQuery query, List<Device> devices);
}

// ItemDeviceProvider — scan items for devices
public interface ItemDeviceProvider {
    void getDevices(ItemDeviceQuery query, List<Device> devices);
}
```

**Registration:**

Providers are registered via NeoForge `DeferredRegister` in `Providers.java`. Example item providers:

- `CPUItemDeviceProvider` — CPUs of various tiers
- `MemoryItemDeviceProvider` — RAM modules
- `HardDriveItemDeviceProvider` — storage drives
- `NetworkInterfaceCardItemDeviceProvider` — NIC
- `RedstoneInterfaceCardItemDeviceProvider` — redstone I/O

## Device Types

| Type | Tag | Example |
|------|-----|---------|
| `MEMORY` | `oc2r:devices/memory` | RAM stick |
| `HARD_DRIVE` | `oc2r:devices/hard_drive` | HDD/SSD |
| `FLASH_MEMORY` | `oc2r:devices/flash_memory` | BIOS/EEPROM |
| `CARD` | `oc2r:devices/card` | Network card |
| `CPU` | `oc2r:devices/cpu` | Processor |
| `ROBOT_MODULE` | `oc2r:devices/robot_module` | Robot upgrade |
| `FLOPPY` | `oc2r:devices/floppy` | Floppy disk |
| `NETWORK_TUNNEL` | `oc2r:devices/network_tunnel` | Tunnel card |

## Related

- [Architecture](ARCHITECTURE.md) — device bus graph and VM lifecycle
- [Source Structure](SRC_STRUCTURE.md) — code layout
