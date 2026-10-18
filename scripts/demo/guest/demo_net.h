/* SPDX-License-Identifier: MIT
 * OC2R demo extension for the bundled Onyx guest; see kernel-network.patch.
 * These operations require root; interfaces use IPv4 longest-prefix routing.
 */
#ifndef OC2R_DEMO_NET_H
#define OC2R_DEMO_NET_H
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <syscalls.h>
#define OC2R_NET_CONFIG 91
#define OC2R_UDP_BIND 92
#define OC2R_UDP_SENDTO 93
#define OC2R_UDP_RECV 94
#define OC2R_UDP_CLOSE 95

static int parse_ip(const char *text, unsigned char *ip) {
    int i;
    for (i = 0; i < 4; i++) {
        char *end;
        long part;
        if (*text < '0' || *text > '9') return -1;
        part = strtol(text, &end, 10);
        if (part < 0 || part > 255) return -1;
        ip[i] = (unsigned char)part;
        if (i == 3) return *end ? -1 : 0;
        if (*end != '.') return -1;
        text = end + 1;
    }
    return -1;
}
static long now_seconds(void) {
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return -1;
    return ts.tv_sec;
}
#endif
