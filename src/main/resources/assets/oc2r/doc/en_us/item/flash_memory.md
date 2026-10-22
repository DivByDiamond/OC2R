# Flash Memory
![Boot code](item:oc2r:flash_memory)

A flash chip contains the initial code executed by the [computer](../block/computer.md) or [robot](robot.md). Blank chips and chips with bundled firmware are different items. A blank chip alone does not provide a working OS.

The bundled Linux firmware is a small boot ROM; it loads the kernel from the built-in root disk. The kernel is no longer embedded in this firmware, and an additional hard drive is not required for the default Linux boot.

To obtain bundled firmware through crafting, combine the largest blank flash chip with a [scrench](wrench.md) for Linux; combine the resulting Linux chip with a scrench again for OnyxOS firmware. The tool is returned. Do not confuse a firmware recipe with the recipe that clears a chip.

The [flash memory flasher](../block/flash_memory_flasher.md) also accepts bundled firmware and local images through its interface. Shut down before replacing a computer's firmware. Custom firmware must match the emulated hardware and provide its own boot requirements.
