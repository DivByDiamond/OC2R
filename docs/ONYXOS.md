# Bundled OnyxOS images

The default image was rebuilt on 2026-11-09 with tablet graphics support.
New computers and tablets receive `otablet --device`, the existing `ochat`
applications, and rebuilt OC2R network/RPC tools. Existing private roots retain
their recorded immutable base and do not receive newly packaged applications.

The tablet uses a 256×160 RGB565 framebuffer and absolute pointer input. Open
the console, run `otablet --device`, then select Screen. The application shows
a full-screen desktop with Clock, Notes and System windows, a dock for those
windows plus Snake and Shell, and real guest uptime and `/proc/meminfo`
values. Notes persist in `/users/tablet-notes.txt`; Snake runs the packaged
`/bin/osnake` as a child process and returns to the desktop; Shell exits to
the console. It does not report fabricated radio or energy telemetry. Q,
Escape or right click returns to the shell. Console/Screen switching leaves
the VM running. Short button transitions are queued across guest redraws, and
framebuffer console painting is suspended while the application owns the
display. Process exit, including SIGKILL, releases that ownership. `otablet`
is built for ring 1 because the kernel currently permits file creation only
in that ring; this is required for note persistence.

The build includes OpenSBI jump firmware, the S-mode OnyxKernel, rebuilt
init/login tools and shell, the RGB565 framebuffer diagnostic, and a raw
16 MiB OnyxFS root image. The kernel is loaded at `0x80200000`.

Verified fixes include:

- The reproduced Minecraft crash after loading `init` was a failed VirtIO
  keyboard negotiation: the driver left a nonzero device base with a null
  used-ring pointer, then polled address `NULL + 2`. Input now negotiates
  `VIRTIO_F_VERSION_1`, publishes readiness only after successful setup, and
  resets/frees partial queues on failure. Evdev letters/digits and polling
  during blocking console reads are also corrected.

- FDT traversal no longer rediscovers VirtIO devices or initializes a queue
  twice. Framebuffer discovery handles `/soc` and legacy `/chosen` nodes,
  deduplicates MMIO addresses, and exposes `/dev/fb0`, `/dev/fb1`, etc.
- Framebuffer ioctls, read/write and mmap operate on the selected device.
  VFS dispatches devfs writes, and the diagnostic uses RGB565 for 16-bit screens.
- Packed framebuffer MMIO bases need not be page aligned. mmap preserves the
  physical page offset and reserves the full rounded extent; the diagnostic
  unmaps that aligned extent. First, middle and last pixels on all three
  framebuffers were checked, including after a world reload, while keyboard
  registers remained intact.
- Shell redirection binds standard streams explicitly; file writes and
  in-process commands such as `pwd > file` keep and restore their descriptors.
- VirtIO block capacity is measured in 512-byte sectors. The old calculation
  incorrectly treated `size_max` as a multiplier and could advertise space
  beyond the backing disk. The 16 MiB image now grows to 4096 OnyxFS blocks.

The original standalone Sedna 5.2.0 verification at 64 MiB RAM covered init, root login,
shell, redirection, create/read/copy/rename/delete, a saved-image reboot, and
three independently written RGB565 framebuffers. That baseline also ran 166
kernel tests, FDT regressions and the existing OnyxApps checks. Current-image
validation and network/RPC evidence are recorded with the source revisions below;
detailed development logs are in `build/reports/onyx-qa/`.

Log in as `root`; a fresh root account reaches `osh$` without a password
prompt. The tablet widget path is verified with exactly 16 MiB RAM. Other
applications may require more memory; the earlier standalone baseline used
64 MiB.

## Firmware-only boot and root storage

The OnyxOS flash selects a private writable OnyxFS root disk provided by the
computer itself, using the bundled 16 MiB image. A separate OnyxOS hard-drive item is optional. The packaged
image is shared read-only; each computer saves its modified pages in its VM
snapshot. Minux and Onyx keep separate root overlays when firmware is changed.
Existing hard-drive items retain their contents and remain attached as extra
disks. An existing Onyx hard drive is not copied into the automatic root disk.

The same `config/oc2r/onyxfs.img` override supplies the immutable base for new
Onyx roots. Saved roots retain their own base identity, as described below;
replacing an image does not migrate an existing filesystem's contents.

## Root image revisions and existing worlds

New private Onyx roots record the SHA-256 identity of their immutable base in
`baseImageId`, alongside the sparse writable pages. The host pins that base in
`config/oc2r/root-images/<sha256>.img`. Keep this directory with server backups,
particularly when using a custom `config/oc2r/onyxfs.img` override. The cache must
be writable to create a new private root; an unavailable cache does not prevent
reading a retained legacy root or using a complete hard-drive image. After an image
update, saved machines select their original base before their writable pages
are restored; newly created roots use the updated image.

Only the newest image is bundled: `src/main/resources/onyxos/` holds one
`onyxfs.img` and the matching `onyx-kernel.bin`. When the image changes, delete
the old image and replace it; do not add dated copies. Computers saved on an
older base keep working while that base is still pinned in
`config/oc2r/root-images/` on the same installation. A fresh installation or a
wiped cache has no copy of the old base, so those computers stop with the
missing-base error below until the cache file is restored. Snapshots created
before `baseImageId` existed have no base identity and are mounted over the
current image. Updating the default image does not install tools into existing
private roots.

If a saved base is missing or its bytes do not match its identity, the VM stops
with an error and retains its original snapshot, including all writable pages.
Restore the corresponding cache image and reload the machine/world; it must not
silently mount those pages over a different filesystem. For a disposable demo
machine, a complete image can alternatively be imported through its private
root output stream, persisting every shadow page independently of the base.

### Updating the stock font in an existing root

An existing private root also retains its installed `/font/default.psf`.
Updating the JAR does not replace that file or normalize its old glyphs. To
update only the stock font, program a new hard-drive item with the current
OnyxOS image, attach it as an additional disk, and cold-boot the guest. Keep
the original private root; do not import the new image over it.

Check `ls /mnt` and the boot log for the secondary mount. The verified setup
mounted the update disk at `/mnt/blk1`; other device layouts may use another
`blkN`. After confirming that this is the new disk and that the guest is still
using its original root, log in as `root` and run:

```text
ls /mnt
ls /mnt/blk1/font
cp /font/default.psf /font/default.psf.before-update
cp /mnt/blk1/font/default.psf /font/default.psf
```

Substitute the actual secondary mount path and use an unused backup filename.
Then stop and start the computer with its power controls for a cold guest boot.
Do not replace a customized font unless that replacement is intentional. This
is an explicit file copy, not automatic migration; the archived base and other
user files remain intact. The kernel loads the font at boot, so restoring a
running VM snapshot or reopening the world alone does not reload it.

Standalone Sedna QA used the previous 2026-10-18 image as a private root
and the current image as the secondary disk. After the guest copy and a fresh
VM boot, the installed font matched the new 5,124-byte PSF exactly, the backup
matched the old PSF, and a user marker survived. Comparing the contents of all existing
regular files, only `/font/default.psf` changed. The stock PSF SHA-256 changed from
`bcaba4723102d4d0d0e3618e420efd2cd5bb07d204611f6d4563567e9e211f55` to
`3b02a883ee1e741499b99e3c9a85ef115948149b4d663591e407e438824d4133`.
The probe, logs and byte-level checks are retained in the ignored development
directory `build/reports/onyx-font-manual-update/`.

## Importing an OS inside Minecraft

Insert a flash chip or writable hard drive into the flash writer and use it
with an empty hand. Its screen offers bundled Minux/Onyx firmware and a local
file chooser. Shift-use ejects the item. A running guest must release the
writer before it can be programmed from this screen.

Custom flash images must contain flat RISC-V machine code for the normal RAM
entry address, `0x80000000`. For the OpenSBI jump layout, place OpenSBI at file
offset zero and the S-mode kernel at offset `0x200000`. Convert ELF executables
to a suitable flat boot image before importing. Import a raw filesystem/disk
`.img` into a hard-drive item if that OS needs its own disk. Selecting bundled
Onyx firmware supplies its root image automatically.

Local imports are bounded by the inserted item's capacity and a 64 MiB image
limit. The server receives ordered chunks and checks the final SHA-256 before
replacing the item's data. Interrupted or rejected uploads retain the original
item. Files are stored as world-local blobs rather than image bytes in item NBT;
no datapack or server-side path is required.


## Multiple screens

A connected projector and each monitor appear as separate native framebuffer
nodes. Their numbering follows device discovery; inspect `/dev` and each
framebuffer's ioctl metadata rather than assigning a physical role by index.
The guest format is `r5g6b5`, with a two-byte pixel and the exported row stride.

The bundled diagnostic can address a single device and choose a color:

```text
run /bin/fb-draw /dev/fb0 red
run /bin/fb-draw /dev/fb1 green
run /bin/fb-draw /dev/fb2 blue
```

Omit the color for a gradient. The console uses its primary framebuffer;
applications can write or map the other devices independently. The basic
framebuffer path does not require the RPC library.


## Native network and device tools

New root images include `netcfg`, `udpdemo`, `udpserve`, `tcpdemo`, `tcpserve`,
`tcpstream`, `oc2ctl`, `play` (the music player, see below) and `rshd` (the remote shell, see below). Their source is in `scripts/demo/guest/` and they are
built with `--ring1` for the required privileged operations. Existing private
roots retain their files and base image; updating the mod does not silently
replace those roots or install tools over user data.

The network tools configure individual NICs and demonstrate UDP/TCP traffic.
Use `netcfg list` to identify interfaces before configuring Internet and LAN
addresses; see `scripts/demo/README-Onyx-network.md`. The bundled `ohttp` implements minimal plain HTTP GETs with a numeric IPv4
address or DNS-resolved hostname. It does not implement HTTPS/TLS, redirects or
chunked transfer decoding. The browser's networking has not been independently
validated by the HTTP checks described below. `ohttp` uses root-ring permissions
because the current kernel restricts creation of new files to that ring; other
normal-ring C apps retain that restriction. `open` resolves relative paths
against the process working directory.

`oc2ctl` uses raw `/dev/hvc0` JSON RPC for discovery, inspection, inventory,
sound and robot actions. Keep the robot's front clear and place the item chest
above it for `inventory-demo`; that demo requires an empty selected slot so it
cannot return pre-existing inventory contents by mistake. Device movement is
queued, so the tool checks the action result rather than treating acceptance as
completion. Native framebuffer applications address `/dev/fbN` independently.

All bundled C apps and native tools were rebuilt against the corrected libc:
its previous `puts`/`fputs`/`fwrite` paths could use uninitialized standard
streams and hang on their first output. A compiler fix also materializes large
structure-member offsets correctly. Merely replacing a compiler or libc file
would not repair already compiled applications.

## Remote shell (`rshd`)

The guest has no sshd and no TLS, so a PC cannot get a real SSH session. `rshd` is the smallest
substitute: a TCP listener on the guest's own stack that gives a plain `nc`/`ncat`/`socat` client a
line shell. It is a ring-1 tool built like `tcpserve` (`scripts/demo/guest/rshd.c`).

```text
rshd PORT PASSWORD          start in the background and return the prompt (prints the pid); needs memory for two copies
rshd PORT --no-password     the same without a password check; the log line says NO PASSWORD
rshd -f PORT PASSWORD       stay in the foreground (takes the console; this is what fits a 16 MiB computer)
rshd                        foreground, reads /etc/rshd.conf ("PORT PASSWORD", one line)
```

A client connects, answers `Password: `, and then sees `osh$ `. Every line it sends is run by
`/bin/osh` (builtins, `/bin` programs, redirection and pipes, all with the output returned over the
socket); `exit`, `quit`, `logout`, Ctrl-D or closing the connection ends the session and the service
waits for the next client. `cd` is remembered between lines. Three wrong passwords drop the
connection, with a one-second delay after each. Lines are limited to 250 characters, the output of one
command to 32 KiB (the command is killed beyond that) and its run time to 120 seconds.

To start it at boot, enable the init service that ships in the image (disabled by default):

```text
echo 2323 mysecret > /etc/rshd.conf
echo on > /etc/init/rshd.enabled
```

then restart the computer. init starts `/service/rshd` as root and the console shows
`[init] + service rshd` and `rshd: listening on TCP port 2323`. Only images built from this change
have `/service/rshd` and a kernel whose `mkdir` keeps `/etc/init` (see below); an existing private
root keeps its own files, and copying `/bin/rshd` there by hand does not work because the shell's
`cp` cannot write files above 45056 bytes.

What it does **not** protect: nothing is encrypted. The password, every command and all output cross
the network in clear text, and the password also shows in the shell history and in the `rshd`
argument list. Whoever knows the password has a root-ring shell in the guest. Do not expose the
port to untrusted networks; the VXLAN hub is meant for a private peer. Limits that follow from the
guest stack: one session at a time (a client that connects and stays silent holds the service until
it disconnects, because `net_recv` blocks with no timeout), no terminal (no history, tab completion
or full-screen programs; a program that reads standard input sees end of file), and no encryption or
host key to check. Over `nc` the password is echoed by your own terminal; use `stty -echo` first.

Memory: each `rshd` image is about 420 KB. On a 16 MiB computer (the largest memory item) the background
start fails with `cannot start /bin/rshd (-12)`, because the launcher and the service would both be resident;
use `rshd -f` or the boot service. While the service runs there, shell builtins (`echo`, `ls`, `cat`, `cd`,
`pwd`, `cp`, ...) work, but programs from `/bin` fail with `osh: spawn: Out of memory`; they need a guest
with more memory (verified with 64 MiB). The shell's own `cmd &` is not an alternative: in this osh it faults
for external programs.

Using it needed three kernel fixes, committed in OnyxKernel: `spawn` now passes the standard-stream
bindings to the child (`51963a4`, otherwise the output of external programs went to the console),
`O_APPEND` writes at the end of the file on every write (`49bc246`) and `mkdir` of an existing name
answers `EEXIST` instead of replacing the directory (`302e484`; init's own `mkdir /etc/init` used to
erase the service enable markers on every boot). The matching kernel is the packaged
`onyx-kernel.bin`; there is no patch file, the source is OnyxKernel `main`.

## Music player (`play`)

`play` (`scripts/demo/guest/play.c`, linked with `oc2rpc.c`, built with `--ring1` like `oc2ctl`) streams a DFPWM
file to a speaker or a sound card through `/dev/hvc0`:

```text
play [-r RATE] FILE.dfpwm   play one file (RATE 8000-48000, default 48000; it must match how the file was made)
play [-r RATE] [DIRECTORY]  list the .dfpwm files of DIRECTORY, the current directory, or /usr/share/music, and pick one
```

The screen shows the file name, the rate, a progress bar, elapsed and total time (total is the file size times eight
divided by the rate) and PLAYING, PAUSED or FINISHED. Keys: Space or `p` pause and resume, `r` start over, `b` back to
the list, `q` or Ctrl-C quit; in the list Up/Down (or `k`/`j`) move, Enter plays. It uses what the terminal really has:
termios with `ECHO`, `ICANON` and `ISIG` off and `VMIN=0`, so `read` returns at once, and plain ANSI (home, clear, erase
to end of line, reverse video). It needs no TLS, no heap and no extra privilege beyond opening `/dev/hvc0`; memory is
one 32 KiB RPC buffer plus small fixed tables (the binary is about 450 KB).

Limits that shape it. The host discards a guest bus message longer than 4096 bytes and a JSON byte costs up to four
characters, so every `writeDfpwm` call carries 900 bytes (0.15 s at 48 kHz). The player keeps about one second queued
(`getBufferedMs`) and waits when the call answers `false`; audio already queued cannot be cancelled, so pause and restart
are heard up to a second late. The elapsed time is the sent time minus the queue, so it follows what has really played.
A child process does not inherit the shell's `cd`, which is why the list falls back to `/usr/share/music`. The image
ships a three-second test tune there. A user cannot add files larger than the free space of the 16 MiB root (about
0.3 MB remain), so bring longer music in on a disk or through the file card.

Two things found while building it. OnyxCC reads `"\033"` as NUL followed by `33` (its lexer returns NUL for any escape
starting with `0`), so the source uses `\x1b`. The `oc2_call` limit of 8191 bytes is not the real limit; the host's 4096 is.

## Input

The computer terminal uses UART input. Native keyboard/mouse input uses VirtIO
queues: Onyx now initializes up to 32 input devices and polls them fairly.
Discovery scans up to 64 total VirtIO nodes shared with disks, serial and
network devices. Independent key/button ownership and serialized dispatch keep
modifier and release events ordered across queues and harts. A standalone real
Sedna guest verifies ten separately initialized keyboards with a fresh command
from each. Pointer snapshots retain all eight button bits in their existing ABI.

The combined OC2R keyboard/mouse block can be paired with a computer, monitor
or projector. Shift+right-click selects it; right-click an endpoint to link.
Right-click the keyboard to capture controls. Right Ctrl releases capture by
default and can be changed in Minecraft Controls. Escape and mouse buttons go
to the guest; changing focus or ending capture releases held guest inputs.

## Console and TUI behavior

On Sedna, the kernel mirrors stdout to the PC's UART terminal and the primary
framebuffer. The shared console grid is capped at 80 columns and 24 rows,
bounded by smaller physical screens. Other platforms keep their framebuffer
console dimensions. Native `/dev/fbN` applications continue to use each device's
full resolution; this console limit does not reduce monitor or projector modes.

The console patch fixes ANSI line erasure (`EL 1` and `EL 2`), moves routine
file-open/ELF-segment traces to debug level, and publishes only the console
viewport during console refreshes. Console publication negotiates the optional [frame completion aperture](FRAME_PRESENT.md),
then issues BEGIN, draws the viewport and cursor, COMMIT, and DISABLE. The
published snapshot remains unchanged during drawing, while unrelated legacy
framebuffer applications retain immediate writes. A full console scroll still
transfers the changed scrolling region; this is not a host-side blit command.

The host does not impose a retro baud-rate
delay: reducing the VM's 1,000-cycle service interval made the measured guest
slower and was rejected. Heavy TUI redraws still execute inside the emulated
CPU and consume its time quota.

The original geometry fix was developed as a patch against OnyxKernel and is now part
of the OnyxKernel history. The later scaling candidate and rebuild notes are described below. Standalone RV64 tests with a
1920×1080 primary framebuffer reproduced an `otop` footer on row 66 before the
geometry fix and row 23 afterwards. In one controlled comparison of the
original unscaled viewport against full-frame presentation, framebuffer stores fell from 1,036,928 to 123,008 and the first frame
fell from about 1,108 ms to 933 ms. These are fixture measurements, not a frame
rate guarantee inside Minecraft or measurements of the later scaled candidate.
Logs are in `build/reports/terminal-qa/`.

## Verified source revisions

### 2026-11-14 image (OnyxFS large files, music player)

- Kernel: OnyxKernel `83c07b1` (the L7 fix: OnyxFS files grow past ten blocks, truncate and are rewritten, on top of
  `302e484`). SHA-256 `8307ef4306848861cd2ddea3377d52ea51569c7c217d494164d7ec49c067947f`, built with
  `cargo kbuild --features smode` and `riscv64-elf-objcopy -O binary`.
- Root: SHA-256 `5a084cf48f875a9b8c5ac5be498366576ffca36058b2f8d525be76f0b84eda85`. It is the previous root
  (`b4447b92...`) with `/bin/play` and `/usr/share/music/demo.dfpwm` added by `assemble-onyx-stock.py
  --reuse-previous-root --data demo.dfpwm=/usr/share/music/demo.dfpwm`; the other files, including the eight earlier tools, are
  byte for byte the previous ones. `play` was built with OnyxCompiller `bd14e5d`. Payload 16,445,440 of 16,777,216 bytes.
- Checked in QEMU (virt, 256 MiB) with a host-side peer on a virtio console standing in for the device bus: `play` starts,
  draws the list and the player, sends 900-byte `writeDfpwm` calls (largest message 3.3 KB) with unsigned byte values and
  the rate, honours `false`, pauses, resumes, restarts, quits and restores the terminal; `cp` of 150 KB and 650 KB files (the
  latter twice, overwriting) gives identical SHA-256 sums; `rshd` background start and sessions with `ncat`-style client.
- Not checked: a real Minecraft speaker or sound card (the peer is a mock; nothing was heard), the framebuffer console
  and the Sedna keyboard path for the keys, a tablet with 16 MiB of RAM, game tests with this image.

### 2026-11-13 image (remote shell)

- Kernel: OnyxKernel `302e484` (the three fixes above on top of `7ea63bf`), built with
  `cargo kbuild --features smode` and `objcopy -O binary`. SHA-256
  `559bd5bede66e7d832a00984e338bada41fda716567578f59132ae7b30116b65`.
- Root: SHA-256 `b4447b921c796065267af306f302d43232e93514c41cbd7ccac16581e2057fde`. It is the previous stock root
  (`e1963b0d...`) with `/bin/rshd` and `/service/rshd` added, assembled with
  `assemble-onyx-stock.py --reuse-previous-root`; every other file, including the seven older tools, the shell,
  login and init, is byte-for-byte the previous one (a rebuild of those tools with the current OnyxCompiler gives
  different bytes, so they were not rebuilt). `rshd` was built by `build-guest-tools.sh` with OnyxCompiller `bd14e5d`.
  The payload is 15,962,112 bytes of the 16 MiB.
- Checked: OnyxKernel unit tests (70 + 3 + 288 passed), strict clippy for host and riscv64, QEMU guests (login,
  `rshd` background start, sessions through `ncat`, wrong passwords, long lines, a 33 KiB output cut at the limit,
  boot service after a reboot, 16 MiB and 64 MiB RAM), the real guest in Sedna behind the production
  `TunnelManager` and a Linux kernel `vxlan` device (`scripts/demo/run-vxlan-shell.sh`: ping 3/3, `ncat` session),
  and the whole NeoForge 1.21.1 game test suite including `RemoteShellTests`.
- Not checked: Fabric and 26.x game tests and the client acceptance test with this image, the hub block entity
  with a Linux peer (the in-game test uses a Java peer over the same datagrams), the real hub from a real
  server reached by a PC outside a namespace, any other host than the development machine.

### Earlier images


The 2026-11-09 image is built from these selected clean source revisions:

- OnyxKernel: `7b2d6554e792ada9d28b6b2bdc286923cdba468f`.
- OnyxApps: `5130d7ab8711647283bbdd2f4ca98bfcf779695f`.
- OnyxOS: `8c8d3fa` (records the exact component lock).
- OnyxShell: `e25e912561f18fb2a7bfe52b56cfe6524d17f185`.
- OnyxCompiller: `9f9ecb5a25378d7009937b2df94f1507cda25252`.
- OnyxBoot: `9d572e3301d6f9711c3d0cdaafe8f7c1f0d2f19e`.
- Kernel SHA-256: `34b38649d85c373b3194d96f8e8fcd34303b8c9d5b579cf5c07d58528476a95d`.
- Root SHA-256: `e1963b0d4983c6b5e84b21bae33f22c2eb7b751533995d28f312a2bf13e544d0`.

All ten current OnyxApps programs are rebuilt. The seven OC2R tools are rebuilt
from `scripts/demo/guest/`: netcfg, udpdemo, tcpdemo, tcpserve, udpserve,
tcpstream and oc2ctl. The previous stock root
`f9371dbaf9da5ddf12adea6d0316962be3fcf3d016dc827e81f45d22cb60d71b`
supplies only its otherwise missing `/bin/dnstest`, `/bin/oed` and `/bin/ohttp`
binaries, retained byte-for-byte through later stock rebuilds. No previous user directory or account data is imported. The assembled
filesystem uses 16,121,856 bytes and is padded to 16 MiB.

Build the locked source image using OnyxOS `scripts/build-oc2r.sh`, then assemble
the OC2R stock root outside the resource directory:

```sh
bash scripts/demo/build-guest-tools.sh "$ONYX_REPOS/OnyxCompiller" "$TOOLS_OUTPUT"
python3 scripts/demo/assemble-onyx-stock.py \
  --fresh-manifest "$ONYX_OUTPUT/manifest.txt" --kernel "$ONYX_OUTPUT/onyx-kernel.bin" \
  --previous-root "$PREVIOUS_STOCK_ROOT" --tools "$TOOLS_OUTPUT" \
  --mkimage "$ONYX_REPOS/OnyxKernel/target/release/mkimage" --output "$CANDIDATE_OUTPUT"
```

The assembler retains only missing regular `/bin` files, verifies every selected
binary against the packed filesystem, refuses payloads over 16 MiB, and writes
an `assembly.json` inventory with hashes. Keep the preceding stock root outside
the source tree when reproducing this overlay. Filesystem timestamps mean
separate builds need not have identical image hashes.

Actual Sedna 5.4 checks with exactly 16 MiB RAM verify login, guest RGB565
widgets, a 40 ms absolute click overlapping redraw, cancellation on release
outside, absence of console overwrite, Q exit and a responding shell. The
assembled stock image also verifies the actual counter digit glyph and executes all seven rebuilt tools' usage/help paths.
A separate QA image verifies SIGKILL of the actual widget process followed by
successful framebuffer ownership reclamation. No host widgets are involved.

Historical source checkpoints below predate this rebuild:

- Previous OnyxKernel base: `dc3aa4b`, plus the console, network, RPC transport, console-scale,
  passwordless-login and relative-open patches (all merged into OnyxKernel since). The base follows the original
  OC2R boot/framebuffer fixes in `1f39a22309d5f38a87f314b2a9a8aa898c440b4f`.
- OnyxApps: `ef5d9cd8d1a7116670d32af4b4441ac89da7a9b3`, all seven C apps rebuilt; `ohttp` carried an end-of-file fix (the app has since been removed) and was built with `--ring1` to create its output file.
- OnyxCompiller: `f9f78d88d6ecd16aee1ef4ab00cee291265d9428`, plus
  the large-member and libc stream-initialisation fixes (merged into OnyxCompiller since).
- OnyxShell: `636fef68547f3431fc9ed8eb3ddcd06900567e0a`.
- OnyxOS: `888f69a5d7dccfdad382bc93641e8ea03ae6a8de`.
- Kernel SHA-256: `1a108abad44ad30ae8d70d637704ba262f75212b163dde36e07a1a0211cbb90a`.
- Historical root image SHA-256: `81c28f41486d64d6258aed1121ed49f8925b131b9d878ecb1bf06cc2fdc31cff`.
- Retained 2026-10-18 root SHA-256: `a03dc9a5bceaf7fe2e65b19c05eaefbf951a35dcfe0b3a272ce753d5cb349561`.
- Retained legacy root SHA-256: `bcbc1f870be9b14967eec802e6021c1c6e934ac1748d893430c7d62261fab31b`.

The earlier integrated checkpoint (kernel `d8c700cd…`, root image `adc8de68…`)
passed 191 host kernel tests, fresh RV64 boot/login, filesystem operations and
redirection, real VirtIO keyboard input, independent RGB565 writes to three packed framebuffers, and a fresh boot
of the saved filesystem with its marker preserved. The 1920×1080 TUI fixture
also returns cleanly to the shell. A two-board/two-NIC guest fixture verifies UDP
and TCP request/reply, an 8 KiB TCP stream across receive-window reopening, and
routing to the Internet NIC. Raw VirtIO RPC verifies a fragmented 6 KiB device
list, discovery and invocation. Those checks describe that checkpoint, rather
than a rerun of every fixture against each later root-image repack. The separate historical HTTP check is
recorded below; it was not repeated for the tablet-image repack. Minecraft integration evidence is recorded separately.

Real Minecraft 26.1.2 checks cover flash-only Onyx boot, login and file operations,
three independent outputs, monitor GUI, separate Minux/Onyx root writes through
firmware changes and world reload, and imported media surviving reload in the
player inventory. Warm CPU/RAM continuation and automatic framebuffer retention
across reload remain separate checks; these checks log in and redraw after boot.

A separate HTTP reader regression reproduces the old busy poll after orderly
EOF and checks a close-delimited response body after the fix. A fresh RV64
client booted from the previous candidate also fetched an HTTP response from
another real VirtIO-networked guest, saved it and printed the exact
`HTTP-MOCK-PASS` body. In that Minecraft 26.1.2 checkpoint, the kernel then fetched
`http://<demo-server>:18080/oc2r` through the host adapter, wrote the 53-byte
body to the relative path `internet.txt`, and `cat internet.txt` printed both
expected lines. The same world verified TCP echo between Onyx guests over the
visible blue cable. Logs and screenshots are under `build/demo-qa/`. The real
demo server address and the guest commands that reach it are kept in the local,
untracked `DEMO_LOCAL.md`.

The host TCP EOF regression was verified separately: all three
`DefaultSessionLayerEofTest` cases failed before the adapter fix (immediate EOF
with data, empty EOF, and a full buffer with a later EOF), then passed together
with nine `EstablishedStateTest` cases. This verifies buffer preservation and
FIN ordering in unit tests; it is not a Minecraft HTTP acceptance result.

The console-scale/login/relative-open candidate added the console-scale,
passwordless-login and relative-open changes after the three original kernel changes.
Installed checksums above identify
which kernel/root pair is currently packaged. Do not infer that an existing
saved root acquired a rebuilt login binary from a mod update.
