# Tablet

> **Status: terminal and guest graphics are implemented.** The built-in 256 × 160 screen displays
> the guest framebuffer. Screen and Console tabs share the same running machine. A live screen
> on the held item remains planned. Radio enrollment and guest RPC are implemented.

## Implemented now

- Item `oc2r:tablet` (stack of one) with a crafting recipe; right click opens the screen.
- One processor slot and one flash slot, edited from the inventory button of the screen. A new tablet comes with
  16 MiB of memory (no slot) and an OnyxOS flash. Without a flash it reports a missing firmware and does not boot.
- The processor clock is capped to a quarter of the computer clock whatever processor is inserted. No energy use.
- The machine state is written to `<world>/data/oc2r_tablets/<uuid>.dat` under the id stored in the item.
- The machine runs only while the tablet is in a hand or its screen is open. Two seconds after that it is saved and
  released, so dropping, dying and logging out lose nothing. A server crash loses what happened since the last save.
- Screen displays a fixed 256 × 160 RGB565 guest framebuffer; Console displays the UART terminal.
  Switching views keeps the running application and machine. Graphics use absolute pointer coordinates
  and evdev keyboard input; Console keeps the existing UART keyboard path. Input is released with
  `CaptureKeyBinding.RELEASE`. Closing the screen, changing views or losing focus releases held input.
- The guest device bus is on `/dev/hvc0` like in a computer. The built-in `tablet` device exposes radio RPC.
  It also advertises the `gps_tower` compatibility type used by the bundled `ochat` discovery code;
  this alias provides radio calls, not tower GPS functions.

## What it is

The tablet is a pocket computer: an item that holds a small virtual machine running OnyxOS by default.
It is the same machine model as a computer or robot, with less power, one screen and no cables.
It is meant for small graphical tools: dashboards, a messenger, a calculator, a search tool,
simple programs written by players. Heavy work stays on PCs; the tablet talks to them over the
[radio network](RADIO-NETWORK.md).

## Hardware and lifetime

The current tablet accepts one CPU and one flash. RAM is fixed at 16 MiB and the CPU clock is
limited to a quarter of the computer clock. It uses no energy. Hard drives and selectable RAM tiers
remain planned.

Each tablet UUID identifies one saved machine. A live holder or an open screen keeps it running;
after 40 ticks without either, it saves and stops. Carrying it in an inventory does not keep it running.

## Screen and input

- A live screen on the item held in hand remains planned.
- Right click opens the tablet screen. Screen routes QWERTY keys and mouse clicks to the built-in
  VirtIO input device; Console routes typing to the UART terminal. Both views use the same VM.
- Input is released with `CaptureKeyBinding.RELEASE` (Right Ctrl by default, rebindable), the same single key
  as the monitor and keyboard capture. There is no second binding.

## Network

The tablet is one more radio device. It attaches to an access point in range (tower 45 blocks,
robot dock station 10 blocks), is admitted with an invitation that the network owner approves
from a PC, and is then addressed by an ORA address such as `7K3F:02Q9`.

Inside the guest the tablet uses the same device bus as a computer: the JSON RPC on `/dev/hvc0`
(see [ONYX_RPC.md](ONYX_RPC.md)). The `ochat` and `ochat-gui` messenger already work this way, so the first
tablet applications can send messages, list devices and ping without new system software.
To make a PC do something for a tablet, run a program on the PC and address it by its ORA address and
service name; permissions (telemetry, command, message) are checked on every send and delivery.

The device exposes `getRadioEndpointId`, `joinRadioNetwork`, `getRadioAddress`,
`getRadioStatus`, messaging, service discovery, telemetry, commands and ping using the existing radio RPC contract.
Approve an invitation for `getRadioEndpointId()` on a network administrator's PC, then call
`joinRadioNetwork(invite, secret)` on the tablet. The administrator must grant the required scoped permissions.

The current credential is stored in a server-only persistent item component, never in terminal updates or
client item synchronization. Activating a suspended identity rotates the credential. A second held copy
cannot use the active tablet's terminal, inventory or radio; older credential generations are rejected.
A copy made after the latest rotation can claim an inactive identity first, at which point its new credential
invalidates the other copy. Copies are not independent computers.

Radio disconnects as soon as the item has no live holder, independently of the machine's two-second save grace.
Transfer, death, logout and dimension changes release the session. Radio enrollment is dimension-scoped;
roaming within the network keeps the ORA address, while another dimension cannot use the old enrollment. The local terminal stays available when revoked
or outside the enrollment dimension. A successful new enrollment retires the previous dimension's credential,
so a copy from the former network cannot reclaim the machine.
Tower and dock coverage use the existing circular horizontal radii without height limits or chunk loading.

## Reflashing

The operating system lives on the flash item. To replace it:

1. Take the flash out of the tablet.
2. Put it in the flash writer. The writer offers the bundled OnyxOS and Minux images and a file chooser for your own image.
3. Put it back into the tablet and turn the tablet on.

A tablet without a flash does not boot. The default tablet comes with a flash carrying the tablet build of OnyxOS.

## Writing software for the tablet

### Trying the graphical screen

In Console, log in as `root` and run `otablet --device`. Select Screen to see the guest-rendered
widgets. The counter button changes the counter and progress bar; Console shows `OTABLET_CLICK=1`.
Switching views leaves the application running. Return to Screen and press Q, Escape or right click
to exit the demo. Existing saved flash images need the updated OnyxOS build for this demo.

The built-in display is a `simple-framebuffer` FDT device: 256 × 160 pixels, RGB565 little endian,
512-byte stride. The frame-present extension matches monitors. The built-in VirtIO input device
advertises absolute X/Y ranges 0–255 / 0–159, Linux evdev keyboard codes and three pointer buttons.
Framebuffer pixels and input device state are saved with the tablet machine.

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
