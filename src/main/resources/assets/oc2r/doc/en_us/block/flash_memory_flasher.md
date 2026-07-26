# Flash Memory Flasher
![Flashes before your eyes](block:oc2r:flash_memory_flasher)

The memory flasher provides a way to flash custom compiled firmware onto a flash chip for your computer.

On a Linux system, the memory flasher will typically appear as */dev/vdX* devices, following any installed hard drives. On a technical level flash memory chips work exactly like a floppy or 
hard drive does from this mod, so you could use it for data storage or sharing though they are primarily intended for firmware storage.

To flash the device you should use the *flash.sh* script found in the */mnt/builtin/bin* directory of the default linux distro. You can use it as follows:

- *flash.sh [device path] [firmware file path] (use opensbi, yes or no, if ommitted opensbi will not be used)*

OpenSBI is an open source loader/bootstrapper for RISC-V systems that makes developing kernels much simpler. Further information about building
custom kernels is outside the scope of this documentation.
