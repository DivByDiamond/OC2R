# Machine interface
![Machine interface](block:oc2r:machine_interface)

The machine interface gives programs access to the machine facing its connector. Attach it to the computer's device bus. The RPC device name is `machine_interface`. See its [crafting recipe](https://wiki.loki-code.dev/en/recipes#recipe-machine_interface).

```lua
local m = assert(require("devices"):find("machine_interface"))
print(m:getTargetBlock())
print(m:getTargetSlotCount())
```

Access follows the target machine's sided item, fluid and energy handlers. An unsupported handler raises an error.

## Items

The menu has 18 regular buffer slots, numbered 0–17. Internal slot 18 retains items that could not be returned during a transfer. Call `recoverItems()` before attempting more transfers when recovery is pending.

| Method | Purpose |
|---|---|
| `getTargetSlotCount()` | Target slot count |
| `getTargetItem(slot)` | Inspect a target slot |
| `getBufferItem(slot)` | Inspect a buffer slot |
| `pushItems(bufferSlot, targetSlot, amount)` | Buffer to target |
| `pullItems(targetSlot, bufferSlot, amount)` | Target to buffer |
| `recoverItems()` | Recover retained transfer items |

Transfers return actual item counts. Slot indices start at zero.

## Fluids and energy

`getEnergyStored()` and `getFluidStored()` inspect local buffers. Use `getTargetEnergyStored()`, `getTargetTankCount()` and `getTargetFluid(tank)` for the attached machine.

`pullEnergy(amount)` and `pullFluid(amount)` move resources from the target into the buffer. `pushEnergy(amount)` and `pushFluid(amount)` move them to the target. Check returned amounts. `recoverFluid()` handles retained fluid.

## State and settings

`getTargetBlock()` reports the block ID. `getTargetState()` reports block-state properties. Supported Create machines also expose `getSettings()` and `setSetting(id, value)`. The interface exposes supported settings rather than arbitrary target NBT. Unsupported settings APIs raise an error.
