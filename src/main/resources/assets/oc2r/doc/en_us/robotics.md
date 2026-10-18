# Robotics

A [robot](item/robot.md) is a mobile computer. Install a CPU, RAM and firmware, charge it using a [charger](block/charger.md), and boot its terminal. Robots use modules instead of cards and cannot connect to the external device bus.

Set up Lua as described in [scripting](scripting.md), then run:

```lua
r = require("robot")
print(r.move("forward"))
print(r.turn("left"))
```

These library functions wait for completion and return success. Direct *devices* calls enqueue actions instead; successful enqueueing is not completed movement.

Robot inventory slot numbers start at zero. Use an [inventory operations module](item/inventory_operations_module.md) for item transfer, a [block operations module](item/block_operations_module.md) for excavation/placement, a [crafting module](item/crafting_module.md) for recipes and a [scanner](item/scanner_module.md) for inspection.

*r.facing()* returns a compass direction. *r.position()* reports coordinates relative to the placement/calibration origin; *r.calibrate()* resets it. These are not absolute world coordinates or GPS. See the [robot API](item/robot.md) for asynchronous actions and detailed methods.
