# CPU
![Processor](item:oc2r:cpu_tier_1)

A CPU is required to start a computer or robot. Install one in its CPU slot while the machine is off. The implemented guest architecture is single-core 64-bit RISC-V with floating-point support. Lua is a guest program, not a separate processor architecture.

The four normal tiers default to 50, 100, 200 and 400 MHz. Frequency is assigned when the item is registered; its displayed name reports the assigned value. The server configuration defines tier settings. The creative processor has a separate 1 GHz setting. Emulated frequency is a target and does not guarantee host performance.

A faster CPU does not replace [RAM](memory.md), firmware or energy. See [getting started](../getting_started.md).
