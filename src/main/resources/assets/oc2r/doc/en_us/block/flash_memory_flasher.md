# Flash Memory Flasher
![Firmware writer](block:oc2r:flash_memory_flasher)

Insert a flash chip or a writable hard drive by using it on the flasher. Use the block with an empty hand to open its writer interface. Sneak-use with an empty hand ejects the item when no write is active.

The interface offers bundled firmware and local-image upload. The server validates the target type and capacity. Writing replaces the target's previous contents; wait for completion before retrieving it. The GUI writer cannot start while the device is mounted by a running computer: shut that computer down first.

Connected through a device bus, the flasher also exposes inserted writable media to the guest as a block device such as */dev/vdb*. Inspect */proc/partitions* to identify it. A raw image can be written with normal guest block-device tools, but choosing the wrong device destroys data. The old *flash.sh* helper is not bundled in the current scripts package.

See [flash memory](../item/flash_memory.md) for bundled firmware and boot requirements.
