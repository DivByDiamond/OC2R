/* SPDX-License-Identifier: MIT */
#include "demo_net.h"
#include <net.h>
#include <errno.h>
static unsigned char ip[4];
static char request[1024];
static char response[1025];
int main(int argc, char **argv) {
    long port, start, now, n;
    size_t sent = 0, length;
    int conn;
    char *end;
    if (argc != 4 || parse_ip(argv[1],ip)) {
        puts("usage: tcpdemo IP PORT MESSAGE"); return 1;
    }
    port = strtol(argv[2], &end, 10);
    length = strlen(argv[3]);
    if (*end || port < 1 || port > 65535 || !length || length > 1022) return 1;
    memcpy(request,argv[3],length);
    request[length++] = '\n';
    conn = net_connect(ip,(int)port);
    if (conn < 0) { printf("TCP connect failed errno=%d\n",errno); return 1; }
    start = now_seconds();
    if (start < 0) { net_close(conn); return 1; }
    while (sent < length) {
        n = net_send(conn,request+sent,length-sent);
        if (n < 0) { printf("TCP send failed errno=%d\n",errno); net_close(conn); return 1; }
        sent += (size_t)n;
        now = now_seconds();
        if (now < start || now-start >= 60) { puts("TCP send timeout"); net_close(conn); return 1; }
        _onyx_syscall0(SYS_yield);
    }
    do {
        n = net_recv(conn,response,1024);
        if (n > 0) {
            response[n] = 0;
            printf("TCP RECEIVED: %s\n",response);
            net_close(conn); return 0;
        }
        if (n == 0 || (errno != ENOENT && errno != EAGAIN)) break;
        _onyx_syscall0(SYS_yield);
        now = now_seconds();
    } while (now >= start && now-start < 60);
    printf("TCP response missing (n=%ld errno=%d)\n",n,errno);
    net_close(conn); return 1;
}
