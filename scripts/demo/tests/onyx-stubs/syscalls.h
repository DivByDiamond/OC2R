/* SPDX-License-Identifier: MIT
 * Syntax-only stub of the Onyx guest <syscalls.h>.
 *
 * The guest tools are built by OnyxCC against the real Onyx headers (see
 * scripts/demo/build-guest-tools.sh). CI has no Onyx toolchain, so this stub
 * lets a standard host C compiler parse and type-check the same sources.
 * It is NOT an implementation: nothing declared here ever runs, and the
 * syscall numbers are placeholders.
 *
 * Keep the prototypes in sync with the Onyx syscall ABI; a mismatch makes the
 * syntax check weaker, not the guest build.
 */
#ifndef OC2R_ONYX_STUB_SYSCALLS_H
#define OC2R_ONYX_STUB_SYSCALLS_H

/* Onyx syscall numbers are opaque here; only the yield trap is named. */
#define SYS_yield 0

long _onyx_syscall0(long syscall);
long _onyx_syscall1(long syscall, long a1);
long _onyx_syscall2(long syscall, long a1, long a2);
long _onyx_syscall3(long syscall, long a1, long a2, long a3);
long _onyx_syscall4(long syscall, long a1, long a2, long a3, long a4);

#endif
