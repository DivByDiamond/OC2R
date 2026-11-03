# Machine interface

`oc2r:machine_interface` is a normal cube. Its arrow/connector front faces the
machine; connect a computer using a bus cable with a bus interface on a remaining
face. Right-click opens its 18-slot item buffer. Hoppers and loader item APIs use
the same inventory.

The RPC device type is `machine_interface`. Methods run on the server thread.
All item slots are zero-based; amounts are 1–64 items or 1–1000 mB/FE per call.
The device never accepts an ItemStack, fluid variant, BlockState or NBT supplied by
a guest: resources must already exist in its buffer or in the attached machine.

| Method | Result |
|---|---|
| `getTargetBlock()` | Registry ID of the attached block |
| `getTargetState()` | Read-only snapshot of block properties |
| `getTargetSlotCount()` | Number of sided inventory slots |
| `getTargetItem(slot)` | Read-only item snapshot |
| `getBufferItem(slot)` | Read-only buffer snapshot; slot 18 is recovery |
| `pushItems(bufferSlot, targetSlot, amount)` | Actual number inserted into target |
| `pullItems(targetSlot, bufferSlot, amount)` | Actual number inserted into buffer |
| `recoverItems()` | Moves rejected items from recovery into normal slots |
| `getTargetEnergyStored()`, `getTargetTankCount()`, `getTargetFluid(tank)` | Read the attached machine's resources |
| `getEnergyStored()` | Buffer energy, capacity 10,000 FE |
| `pullEnergy(amount)`, `pushEnergy(amount)` | Actual energy transferred |
| `getFluidStored()` | Fluid IDs, amount, capacity 8,000 mB and pending recovery |
| `pullFluid(amount)`, `pushFluid(amount)` | Actual millibuckets transferred |
| `recoverFluid()` | Moves pending fluid into the fluid buffer |
| `getSettings()` | Supported Create scroll settings with IDs/options/current values |
| `setSetting(id, value)` | Calls Create's public setter; returns clamped value |

Item remainders occupy a separate persistent recovery slot. Until it is emptied,
further item transfers are refused. Call `recoverItems()` or extract slot 18
through the item capability. Fluid remainders likewise require `recoverFluid()`.
Custom item and fluid components are retained, including across save/load.
Breaking the interface drops its inventory once, including recovery; stored energy
and fluid are destroyed. Rotating the interface preserves its buffers and changes
the attached machine. Unsupported capabilities report an RPC error.

Create controls are also discoverable directly with an ordinary bus interface as
`create_controls`, exposing `getSettings()`, `getValue(id)`, `setValue(id,value)`.
Numeric RPM and enum controls use Create's normal clamp, callback and sync path.
Create is optional. Other mods need an explicit settings adapter: arbitrary
BlockState/NBT writes and unrestricted reflection are deliberately absent.

RPC uses the existing NUL-delimited JSON transport on `/dev/hvc0` in Onyx. No new
kernel driver is required. List devices to obtain the UUID, then invoke e.g.:

```json
{"type":"invoke","data":{"deviceId":"UUID","name":"pushItems","parameters":[0,0,16]}}
```

## Reproducible checks

The shared `MachineInterfaceTests`/`MachineTransferTests` run on both loaders.
Create acceptance was verified with `create-1.21.1-6.0.10-280.jar`. Supply that real
NeoForge Create jar locally:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./gradlew :1.21.1:gameTest \
  -PcreateTestJar=/absolute/path/to/create-1.21.1.jar
```

The optional source set is excluded from normal builds and does not ship tests or
Create in the mod jar.

The Create opt-in runs the isolated `oc2r_create` test namespace, avoiding unrelated
mock-player login fixtures that do not negotiate Create's network channels.

Client screenshots and the buffer GUI can be reproduced on 26.1.2 or 26.2:

```sh
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ALSOFT_DRIVERS=null xvfb-run -a \
  ./gradlew :fabric:26.2:runClientGameTest \
  -Pfabric.clientTests=true -Pfabric.machineTests=true
```
