# Tablet

> **Status: design, not implemented.** No tablet item exists yet. This page records the agreed design
> and the contract for software authors so that applications and operating systems written today
> keep working when the item lands. Items below the "Planned" markers are targets, not features.
> Progress is tracked in [roadmap/tablet-radio.md](roadmap/tablet-radio.md).

## What it is

The tablet is a pocket computer: an item that holds a small virtual machine running OnyxOS by default.
It is the same machine model as a computer or robot, with less power, one screen and no cables.
It is meant for small graphical tools: dashboards, a messenger, a calculator, a search tool,
simple programs written by players. Heavy work stays on PCs; the tablet talks to them over the
[radio network](RADIO-NETWORK.md).

## Hardware (planned)

- **Processor slot.** One CPU item. The tablet caps the clock below a PC (the first target is a quarter
  of the PC frequency) whatever CPU is inserted.
- **Disk slot.** One hard drive or flash item holds the operating system and files. It is the only
  part players insert and remove freely.
- **RAM and screen.** Fixed when the tablet is crafted. The RAM tier follows the crafting ingredient and is stored
  in the item. First targets: 256 x 160 pixel screen, 16 MiB of RAM for the basic craft.
- **Power.** None. The tablet does not use energy.

These numbers are starting values and may change after performance testing.

## Running, state and lifetime (planned)

- Every tablet has a UUID stored in the item. The machine state (memory, device state, terminal,
  the writable layer of the root disk) is saved in world data under that UUID, the way a robot's state is saved.
  The item itself stays small.
- The machine runs only while the tablet is in a player's hand or its screen is open. Otherwise it is suspended
  and its state is kept. Dropping the item, dying or logging out suspends it without losing data.
- Background work while carried in the inventory is not part of the first version.

## Screen and input (planned)

- Held in hand, the tablet shows a small live copy of its screen.
- Right click opens the large screen. All keys of a QWERTY keyboard and the mouse go to the guest, so any program can read
  typing, shortcuts and clicks.
- Input is released with `CaptureKeyBinding.RELEASE` (Right Ctrl by default, rebindable), the same single key
  as the monitor and keyboard capture. There is no second binding.

## Network (planned)

The tablet is one more radio device. It attaches to an access point in range (tower 45 blocks,
robot dock station 10 blocks), is admitted with an invitation that the network owner approves
from a PC, and is then addressed by an ORA address such as `7K3F:02Q9`.

Inside the guest the tablet uses the same device bus as a computer: the JSON RPC on `/dev/hvc0`
(see [ONYX_RPC.md](ONYX_RPC.md)). The `ochat` and `ochat-gui` messenger already work this way, so the first
tablet applications can send messages, list devices and ping without new system software.
To make a PC do something for a tablet, run a program on the PC and address it by its ORA address and
service name; permissions (telemetry, command, message) are checked on every send and delivery.

## Reflashing

The operating system lives on the disk or flash item, not in the tablet. To replace it:

1. Take the disk or flash out of the tablet.
2. Put it in the flash writer. The writer offers the bundled OnyxOS and Minux images and a file chooser for your own image.
3. Put it back into the tablet and turn the tablet on.

A tablet without a disk does not boot. The default tablet comes with a flash carrying the tablet build of OnyxOS.

## Writing software for the tablet

### C programs for OnyxOS

Applications are C99 programs built with OnyxCC from the OnyxApps repository.

- Put the program in `apps/<name>/` and build it with the existing Makefile or `scripts/build-apps.py`.
- Talk to the radio network with `lib/oc2rpc` (`oc2_open`, `oc2_call` and related functions). Use `apps/ochat`
  as a worked example; see the OnyxOS document `docs/dev/ochat.md`.
- Draw with the framebuffer and mouse frontend in `apps/otablet`, which provides labels, buttons, progress indicators
  and bounded data binding.
- Only one process should own `/dev/hvc0` at a time.
- Programs run in the user ring. The bus device is opened by uid 0; do not ask for ring 1.

### Lua

OnyxOS ships standard Lua 5.5 (`lua` and `luac`). Scripts run from the shell:

```sh
lua -e 'print("hello", 6 * 7)'
lua myscript.lua
```

Pure Lua `require` searches `/usr/share/lua/5.5/` and the current directory. A minimal script that works today:

```lua
-- counter.lua: counts to ten and writes the result to a file
local total = 0
for i = 1, 10 do total = total + i end
print("sum", total)
local f = assert(io.open("/tmp/sum.txt", "w"))
f:write(tostring(total), "\n")
f:close()
```

Lua has no radio or RPC binding yet. The planned first scope is a minimal example, not a scripting platform:
a small explicit host API to format values and react to UI events, with limits on instructions, memory and
how often it may send commands. No access to host files or Minecraft internals. This page will document the
API once it exists.

### Your own operating system

Any system that boots the same way OnyxOS does can run on the tablet:

- Firmware is OpenSBI jump firmware followed by a flat RISC-V binary (not an ELF) placed so the S-mode payload
  starts at `0x80200000`.
- The root disk is a virtio block device. The device bus is a virtio console. See [ONYXOS.md](ONYXOS.md)
  for the bundled layout, root image rules and how images are replaced.
- Write the image to a disk or flash item with the flash writer, then insert it into the tablet.

Keep ordinary Ethernet compatibility in mind: the tablet and PCs share the same transport rules,
and the radio services sit above them.
