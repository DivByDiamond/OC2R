# OpenComputers II: Modern

**OpenComputers II: Modern** is a fork and continuation of [OpenComputers II](https://www.curseforge.com/minecraft/mc-mods/oc2), originally made by Sangar, ported to modern Minecraft and heavily maintained. The original mod was the spiritual successor to [OpenComputers](https://www.curseforge.com/minecraft/mc-mods/opencomputers).

Works out of the box with **Create: Aeronautics** - computers keep running on your flying airships, with no ID conflicts.

Like the original OC2, the computers are based on a 64-bit RISC-V emulation layer called [Sedna](https://github.com/fnuecke/sedna), written entirely in Java. Our fork keeps the same solid foundation but adds a full VT100-compliant terminal, tiered hardware, restored recipes and dozens of upstream bug fixes.

[GitHub](https://github.com/TumRedSun/OC2R)

## **Features**

- **Real RISC-V emulation** - full 64-bit RISC-V virtual machine that boots an actual Linux image, bundled with the mod (no extra downloads)
- **High-level Lua API** - control in-game devices without writing kernel drivers
- **Custom hardware** - computers, keyboards, monitors, cables, network hubs, disk drives, PCI card cages and more
- **Native networking** - cross-platform library with binaries for macOS, Windows, Linux and Android
- **Persistence** - computer state survives world reloads
- **Addon API** - simple Java API for adding your own virtual devices

## **What Makes This Fork Modern**

- **VT100-compliant terminal** - full ANSI palette, cursor control, scroll regions and tab handling, fixed to pass the vttest suite
- **GPU tiers T1–T4** - re-textured cards and tiered performance
- **OnyxOS firmware** - OpenSBI + OnyxKernel running in S-mode
- **Cable energy network** (1024/t) and a sound card with tone/PCM output
- **HDD/flash memory tiers** with configurable sizes
- **Missing recipes restored** - internet card, PCI card cage, VXLAN hub and internet gateway are craftable again
- **Creative tab fixed** - all peripherals obtainable in survival without `/give`

## **Server Owners/Managers**

This fork is being actively maintained and tested for NeoForge 1.21.1. Computers run real Linux inside a RISC-V VM, so keep an eye on RAM usage on large servers - each running computer consumes resources proportional to its workload.

## **Getting Started**

[Image 1]

The mod provides stationary computers and, in the future, mobile robots. Computers connect to other in-world devices via bus cables and extension cards, and can communicate with each other using network cards and cables.

To get started:

1. Craft a computer and place it in the world
2. **Shift + Right-click** (empty hand) → power on
3. **Right-click** (empty hand) → open terminal
4. **Right-click with a wrench** → open inventory to install components

The default Linux image is bundled with the mod - nothing extra to download.

## **Operation**

The default operating system used by the mod is Linux. It ships with well-known utilities such as the text editors *vi* and *nano*. [Buildroot](https://buildroot.org/) is used to create the kernel and root filesystem images.

For easy scripting, Lua is included. Minecraft-specific devices - inventories, the *Redstone Interface Block* and general mod interoperability - expose a high-level API intended to be used through Lua. This eases both adding integrations with other mods and using these APIs when scripting in the game.

Here's an example snippet that sends a redstone signal through a *Redstone Interface Device*:

```lua
require("devices"):find("redstone"):setRedstoneOutput("up", 15)
```

## **Modularity**

Computers can be configured using various device items. Shared device types include memory (RAM), firmware, CPUs, and hard drives. Computers additionally allow installation of expansion cards, such as the *Network Interface Card*.

## **Integrations**

| Mod | Status |
|-----|--------|
| Create | ✅ Supported |
| Create: Aeronautics | ✅ Supported |
| Valkyrien Skies | ✅ Supported |
| ProjectRed Transmission | ✅ Bundled redstone |
| JEI | ✅ Recipe viewer integration |
| Any mod with `WRENCHES` tag | ✅ Automatic wrench support |

## **Requirements**

- **Minecraft**: 1.21.1
- **Loader**: NeoForge 21.1.243+
- **Java**: 21+

## **Contributors**

- [TumRedSun](https://github.com/TumRedSun) - maintainer
- [loki5512344](https://codeberg.org/loki5512344) - maintainer
- [pocketprobe](https://github.com/pocketprobe) - contributor (upstream bug fixes)

## **Bug Reports**

Found a bug? Report it on our [GitHub repository](https://github.com/TumRedSun/OC2R/issues)

## **License**

GNU General Public License v3.0. Not an official OpenComputers project.

---

**Made with ❤️ by TumRedSun, loki5512344 & contributors**
