# Getting Started

Start with a [computer](block/computer.md), one [CPU](item/cpu.md), and four 8 MiB [memory](item/memory.md) modules (32 MiB total is a practical starting configuration). The survival computer recipe includes a Linux [firmware chip](item/flash_memory.md); a plain computer obtained in another way may need that chip installed separately.

Place the computer and use a [scrench](item/wrench.md) to open its inventory. Install the CPU, RAM and firmware in their dedicated slots. The preconfigured creative computer already contains components. Supply energy and check the energy bar's tooltip for stored energy and consumption. Then open the terminal and press its power button. Wait for the login prompt and log in as *root*.

The bundled Linux boots from a built-in writable root filesystem. An extra hard drive and the old wrench-prepared "Linux disk" are not required. Extra [hard drives](item/hard_drive.md) are data disks; formatting them erases their contents. The firmware loads the kernel from the built-in root disk.

The computer's own terminal works without a GPU. For a [monitor](block/monitor.md), install a [GPU](item/gpu.md), connect the monitor through [bus interfaces](block/bus_interface.md) and [cables](block/bus_cable.md), and power the display. A [projector](block/projector.md) is a separate fixed-resolution framebuffer device.

Shut down before replacing native components. If boot fails, check CPU, RAM, firmware, energy and the error shown in the terminal. Changing the server's component/energy settings can change the required setup.

See [basics](basics.md), [scripting](scripting.md), [networking](networking.md) and [robotics](robotics.md) for the next steps.
