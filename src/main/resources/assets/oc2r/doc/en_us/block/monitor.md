# Monitor
![Monitor](block:oc2r:monitor)

The monitor, like the projector, provides a framebuffer device to [computers](computer.md). Its resolution is selected by the installed [GPU](../item/gpu.md): 320x200, 640x400, 1024x768 or 1920x1080. Without a GPU it stays dark. The framebuffer uses RGB565 (5 red, 6 green and 5 blue bits).

Monitors need to be powered directly to function. The device bus cannot provide enough energy on its own. When insufficiently powered, this is indicated by a red error text on the screen.

On a Linux system, monitors will typically appear as */dev/fbX* devices. To send data to the framebuffer, it is possible to write to these devices. For example, to clear a framebuffer one might pipe zeros to the device like so: *cat /dev/zero > /dev/fb0*.

The monitor provides a built-in keyboard for usage with the default Linux distro, this feature may be interrupted by having another keyboard device connected to the same computer.

Computers *have to be shut down* before installing or removing this component. Installing it while the computer is running will have no effect, removing it may lead to system errors.

## Multiblock Monitors

Monitors can be combined into larger screens by placing additional monitor blocks next to an existing monitor (similar to OpenComputers 1.12.2). When two monitors with the same facing are placed adjacent to each other, they automatically merge into a single larger display.

The multiblock can be grown up to 8 blocks wide and 8 blocks tall. The framebuffer content at the GPU resolution is scaled to fill the entire multiblock area, so a 2x2 monitor displays the same image as a 1x1 monitor but four times larger.

Only one of the blocks in a multiblock (the master) hosts the actual device bus connection and framebuffer device — the others are inert casing blocks. Interacting with any block of the multiblock (right-click, shift-right-click) is redirected to the master.

Breaking any block of the multiblock with a pickaxe or wrench will break the entire multiblock and drop one monitor item per block at the broken position.
