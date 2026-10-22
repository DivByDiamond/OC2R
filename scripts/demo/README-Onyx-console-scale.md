# Onyx console presentation and passwordless login

These patches target OnyxKernel `dc3aa4b`. The existing clean owning checkout
was explicitly authorized for these edits after creation of a disposable clone
was rejected by tool policy. Its 37 original touched files and SHA-256 hashes
were backed up under `build/onyx-console-scale/original-files` and
`original-sha256.json`; 12 new paths are recorded separately. Git metadata was
not changed and no commit was made. Build outputs use an ignored target
directory in this OC2R workspace.

Apply `onyx-console.patch`, `onyx-network.patch`, and `onyx-rpc.patch` first,
then `onyx-console-scale.patch`. Apply `onyx-login-passwordless.patch` to the
same checkout before rebuilding `onyx-login`.

## Console presentation

The ANSI interpreter still paints an 80×24 logical grid on a sufficiently
large Sedna display, matching the PC UART terminal. Presentation scales that
640×384 console to the largest centered viewport with the same aspect ratio:

| Framebuffer | Console grid | Visible viewport |
| --- | --- | --- |
| 1920×1080 | 80×24 | 1800×1080, 60-pixel side margins |
| 640×480 | 80×24 | 640×384, 48-pixel top/bottom margins |
| 320×200 | 40×12 | 320×192, 4-pixel top/bottom margins |

Scaling uses nearest sampling with rational coordinates. The small display
retains the existing bounded console grid. Native framebuffer registration,
geometry ioctls, mmap and application graphics retain the full resolution.
Platforms without Sedna's serial mirror retain their existing presentation.
The existing direct-to-front fallback is retained if back-buffer allocation
fails; scaled presentation requires the normal back buffer.

Glyphs, ANSI erase operations, scrolling and alternate-screen transitions
record logical dirty rectangles. The existing 20 ms idle / 150 ms maximum
presentation batching remains in place. Only the transformed dirty rectangle
is copied to MMIO. Cursor cells are restored separately, so moving a cursor
between opposite corners does not cause a full-frame copy. The presenter does
not allocate or divide on every destination pixel; stores to the front buffer
are volatile. A complete TUI repaint necessarily updates its complete scaled
viewport, but a single changed character updates at most 1,100 pixels at
1920×1080 in the regression fixture.

The patch adds real ANSI/presenter pixel coverage to the kernel's serialized
console pipeline suite. New pure geometry/RGB565/RGB8888 tests also cover
fractional dirty boundaries, padded strides and the last TUI row/column.

## Login behavior

The default appliance account contains a hash of an empty password. Login
first verifies that empty password using the existing authentication code.
When verification succeeds, the chosen account enters its shell without a
password prompt. A protected account still gets the masked password reader
and normal verification. Existing legacy-hash migration remains unchanged.
No account, stored password, shadow entry or filesystem overlay is reset.
A missing, malformed, empty or locked shadow field remains rejected by the
existing verifier; absence of a hash is not treated as passwordlessness.

## Verification boundary

Validation completed so far:

- Full kernel suite: **198 passed**, including seven new geometry/pixel/pipeline
  regressions. The actual ANSI pipeline test uses a static valid PSF1 font.
- RV64 S-mode kernel and `onyx-login` release builds: passed.
- Standalone pixel/presenter fixture: seven passed; password reader: three passed.
- Actual RV64 candidate login: passwordless root reaches the shell without a
  password prompt; a password set by the real `passwd` tool survives a saved
  disk reboot; a wrong password is rejected and the correct one accepted;
  locked `root:!` rejects both an empty and the former password.
- Actual 1920×1080 guest `otop`: the complete physical body and footer were
  captured and inspected, with the footer at UART row 23 and a normal `q` exit.
  Capture waits for pixels on the final physical footer row, not merely for
  UART text. Native framebuffer drawing reaches the physical first/last pixels
  on all three independent displays.
- One observed first **fully visible** TUI frame used 174,944,000 guest cycles,
  3,942,283 framebuffer stores and 2.58 seconds on the development host. A full
  scaled redraw updates more pixels than the former tiny viewport; these are
  measurements, not performance guarantees. A single changed glyph remains
  bounded to its scaled dirty rectangle.

The validated binaries were installed as the current bundled resources:

| Resource | SHA-256 |
| --- | --- |
| `onyx-kernel.bin` | `7c5a06134b92f5f090819979010e7c445f7b3ca4376e2a5be752e97cc5297dda` |
| `onyxfs.img` | `a03dc9a5bceaf7fe2e65b19c05eaefbf951a35dcfe0b3a272ce753d5cb349561` |
| unchanged `onyxfs-20261016.img` | `bcbc1f870be9b14967eec802e6021c1c6e934ac1748d893430c7d62261fab31b` |

Logs, previous binaries, source backups, promotion hashes and the complete
`guest-otop-1920x1080.png` are under `build/onyx-console-scale/`. Minecraft GUI
acceptance is tracked separately in the demonstration plan. Saved sparse roots
continue selecting their pinned immutable base; promotion does not rewrite
existing users' password files or overlays.

In an authorized checkout with all five patches applied, run:

```sh
cargo test -p onyx_kernel -- --test-threads=1
cargo kbuild --features smode
cargo build --release -p onyx_init --bin onyx-login --target riscv64gc-unknown-none-elf
```

Then verify fresh and saved guests: readable login/TUI on the three resolutions,
`otop` footer and quit, cursor hide/move, native full-resolution graphics, and
both passwordless and protected users. Reuse the standalone Sedna probes under
`build/reports/terminal-qa/` for first-frame cycles and framebuffer MMIO writes.
Do not replace a running world's filesystem base merely to change its login
binary; package a versioned candidate and preserve saved-image compatibility.
