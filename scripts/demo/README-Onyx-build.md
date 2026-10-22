# Rebuilding Onyx kernel and userspace candidates

Source bases:

- OnyxKernel: `dc3aa4bdfaa8d86d1b241d0056248685b1feac48`.
- OnyxApps: `ef5d9cd8d1a7116670d32af4b4441ac89da7a9b3`.
- OnyxCompiller: `f9f78d88d6ecd16aee1ef4ab00cee291265d9428`.
- OnyxShell: `636fef68547f3431fc9ed8eb3ddcd06900567e0a`.
- OnyxOS: `888f69a5d7dccfdad382bc93641e8ea03ae6a8de`.

The kernel's original integrated patch set is `onyx-console.patch`,
`onyx-network.patch`, then `onyx-rpc.patch`. The complete candidate adds
`onyx-console-scale.patch`, `onyx-login-passwordless.patch`, then
`onyx-open-relative.patch`. The login patch changes the Rust login tool as well
as the kernel checkout; rebuild and package login too. Check `docs/ONYXOS.md`
for the installed image hashes and runtime checks; building a candidate does
not promote it.

Use Rust 1.97 with the `riscv64gc-unknown-none-elf` target, a host C toolchain,
and `riscv64-elf-objcopy`. Use disposable sibling checkouts. The example clones
local repositories; substitute source URLs if those local paths are unavailable.
The work directory must be new: `git clone` will fail if its destination exists.
No command below replaces the packaged or a running world's filesystem base.

```sh
oc2r_repo=/path/to/oc2r
onyx_sources=/path/to/owning/Onyx
onyx_work=/tmp/oc2r-onyx-rebuild
mkdir -p "$onyx_work/repos"
for repo in OnyxKernel OnyxApps OnyxCompiller OnyxShell OnyxOS; do
  git clone --no-hardlinks "$onyx_sources/$repo" "$onyx_work/repos/$repo"
done
git -C "$onyx_work/repos/OnyxKernel" checkout --detach dc3aa4bdfaa8d86d1b241d0056248685b1feac48
git -C "$onyx_work/repos/OnyxApps" checkout --detach ef5d9cd8d1a7116670d32af4b4441ac89da7a9b3
git -C "$onyx_work/repos/OnyxCompiller" checkout --detach f9f78d88d6ecd16aee1ef4ab00cee291265d9428
git -C "$onyx_work/repos/OnyxShell" checkout --detach 636fef68547f3431fc9ed8eb3ddcd06900567e0a
git -C "$onyx_work/repos/OnyxOS" checkout --detach 888f69a5d7dccfdad382bc93641e8ea03ae6a8de
for patch in onyx-console.patch onyx-network.patch onyx-rpc.patch onyx-console-scale.patch onyx-login-passwordless.patch onyx-open-relative.patch; do
  git -C "$onyx_work/repos/OnyxKernel" apply --check "$oc2r_repo/scripts/demo/$patch"
  git -C "$onyx_work/repos/OnyxKernel" apply "$oc2r_repo/scripts/demo/$patch"
done
for patch in onyxcc-large-member.patch onyx-libc-stdio-init.patch; do
  git -C "$onyx_work/repos/OnyxCompiller" apply --check "$oc2r_repo/scripts/demo/$patch"
  git -C "$onyx_work/repos/OnyxCompiller" apply "$oc2r_repo/scripts/demo/$patch"
done
git -C "$onyx_work/repos/OnyxApps" apply --check "$oc2r_repo/scripts/demo/onyx-ohttp-eof.patch"
git -C "$onyx_work/repos/OnyxApps" apply "$oc2r_repo/scripts/demo/onyx-ohttp-eof.patch"
make -C "$onyx_work/repos/OnyxCompiller"
```

The six-kernel-patch sequence above was checked against a fresh archive of the
listed kernel revision. OnyxCC compiles/links libc sources adjacent to its own
executable, so both compiler/libc patches are required. Existing application
binaries do not acquire these fixes until rebuilt.

Rebuild all seven C apps and all seven native demonstration tools:

```sh
bash "$oc2r_repo/scripts/demo/rebuild-onyx-userspace.sh" \
  "$onyx_work/repos/OnyxApps" "$onyx_work/repos/OnyxCompiller/onyxcc" \
  "$onyx_work/guest-tools"
```

This forces rebuilding because the upstream Makefile does not track
compiler/libc changes. The six network tools and `oc2ctl` use `--ring1` for
privileged operations. `ohttp` is separately rebuilt with `--ring1`: the current
kernel permits creation of new output files only to root-ring applications.
Its EOF patch recognizes zero from `net_recv` as orderly EOF, including
close-delimited HTTP bodies. Other OnyxApps retain their normal build settings;
this does not fix their ability to create new files under that kernel policy.

Build and test Rust components, including the changed login, with isolated
staging paths. The upstream script resets its staging account databases; never
point `ONYX_OUTPUT` at user files or at an existing world's disks.

```sh
(cd "$onyx_work/repos/OnyxKernel" && cargo test -p onyx_kernel -- --test-threads=1)
rustc --edition=2024 --test "$onyx_work/repos/OnyxKernel/init/src/login/password.rs" \
  -o "$onyx_work/login-password-tests"
"$onyx_work/login-password-tests"
ONYX_REPOS="$onyx_work/repos" ONYX_OUTPUT="$onyx_work/staging" \
  bash "$onyx_work/repos/OnyxOS/scripts/build-oc2r.sh"
ONYX_MKIMAGE="$onyx_work/repos/OnyxKernel/target/release/mkimage" \
  bash "$oc2r_repo/scripts/demo/build-onyx-demo-image.sh" \
  "$onyx_work/staging/manifest.txt" "$onyx_work/guest-tools" \
  "$onyx_work/onyxfs-candidate.img" "$onyx_work/repos/OnyxApps/build"
sha256sum "$onyx_work/staging/onyx-kernel.bin" "$onyx_work/staging/login.onx" \
  "$onyx_work/onyxfs-candidate.img"
```

The manifest is whitespace-delimited; use staging paths without spaces. The
candidate is exactly 16 MiB. Test the candidate kernel and root image together
in real RV64 guests before promotion. Login verification must cover the seeded
empty-password hash and protected, wrong-password, locked and malformed account
records; kernel tests alone do not establish login behavior. Console scaling
also needs actual framebuffer/TUI checks, including the smaller robot display.

Preserve `src/main/resources/onyxos/onyxfs-20261016.img` byte for byte. Saved
private roots select their original base identity and will not acquire a new
login or HTTP client by updating the default image. A tested candidate may
replace only the current `onyxfs.img` and the matching current kernel; retain
all base-cache files used by existing snapshots. Filesystem timestamps can make
freshly repacked images differ even when their file contents are identical.

A host HTTP-reader regression supplies a close-delimited response and then EOF;
it checks the exact persisted body and rejects polling beyond EOF:

```sh
mkdir -p "$onyx_work/http-test-include"
printf '#include <stddef.h>\nlong net_recv(int, void *, size_t);\n' > "$onyx_work/http-test-include/net.h"
cc -Wall -Wextra -I "$onyx_work/http-test-include" \
  "$oc2r_repo/scripts/demo/tests/ohttp_peer_eof.c" \
  "$onyx_work/repos/OnyxApps/apps/ohttp/http/response.c" -o "$onyx_work/ohttp-eof-test"
"$onyx_work/ohttp-eof-test" "$onyx_work/http-response.txt"
```

This checks the userspace EOF contract. The host adapter has separate regression
tests (`DefaultSessionLayerEofTest` and `EstablishedStateTest`). The packaged
kernel also resolves relative `open` paths against the process working
directory, allowing `ohttp ... internet.txt`; the current Minecraft 26.1.2
demo fetched the public response and read the saved body back with `cat`.
Development evidence is under `build/reports/onyx-qa/`,
`build/reports/terminal-qa/`, and the ignored `build/onyx-http-debug/` and
`build/onyx-console-scale/` directories.
