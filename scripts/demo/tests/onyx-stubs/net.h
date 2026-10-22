/* SPDX-License-Identifier: MIT
 * Syntax-only stub of the Onyx guest <net.h>.
 *
 * See syscalls.h in this folder: the real header comes from the Onyx
 * toolchain, this one only exists so that a host C compiler can parse the
 * guest tools in CI.
 */
#ifndef OC2R_ONYX_STUB_NET_H
#define OC2R_ONYX_STUB_NET_H

#include <stddef.h>

int net_connect(const unsigned char ip[4], int port);
long net_send(int connection, const void *data, size_t length);
long net_recv(int connection, void *data, size_t length);
void net_close(int connection);

#endif
