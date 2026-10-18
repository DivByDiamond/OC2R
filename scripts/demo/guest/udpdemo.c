/* SPDX-License-Identifier: MIT */
#include "demo_net.h"
#include <errno.h>
static unsigned char remote[8],received[8];
static unsigned char ip[4];
static char buffer[1025];
int main(int argc, char **argv) {
    long port, result;
    char *end;
    if (argc == 5 && strcmp(argv[1], "call") == 0) {
        long socket,start,now;
        if(parse_ip(argv[2],remote))return 1;
        port=strtol(argv[3],&end,10);
        if(*end || port<1 || port>65535 || !*argv[4] || strlen(argv[4])>1024)return 1;
        remote[4]=(unsigned char)(port>>8);remote[5]=(unsigned char)port;
        socket=_onyx_syscall1(OC2R_UDP_BIND,40000);
        if(socket<0){printf("UDP client bind failed %ld\n",socket);return 1;}
        result=_onyx_syscall4(104,socket,(long)remote,(long)argv[4],strlen(argv[4]));
        if(result<0){_onyx_syscall1(OC2R_UDP_CLOSE,socket);printf("UDP request failed %ld\n",result);return 1;}
        start=now_seconds();
        do {
            result=_onyx_syscall4(103,socket,(long)buffer,1024,(long)received);
            if(result>=0 && memcmp(remote,received,6)==0){
                buffer[result]=0;printf("UDP ROUNDTRIP: %s\n",buffer);
                _onyx_syscall1(OC2R_UDP_CLOSE,socket);return 0;
            }
            if(result<0 && result!=-ENOENT)break;
            _onyx_syscall0(SYS_yield);now=now_seconds();
        }while(start>=0 && now>=start && now-start<60);
        _onyx_syscall1(OC2R_UDP_CLOSE,socket);printf("UDP response missing (%ld)\n",result);return 1;
    }
    if (argc == 5 && strcmp(argv[1], "send") == 0) {
        if (parse_ip(argv[2], ip)) { puts("udpdemo: invalid IPv4"); return 1; }
        port = strtol(argv[3], &end, 10);
        if (*end || port < 1 || port > 65535 || !*argv[4] || strlen(argv[4]) > 1024) return 1;
        result = _onyx_syscall4(OC2R_UDP_SENDTO, (long)ip, port, (long)argv[4], strlen(argv[4]));
        if (result < 0) { printf("UDP send failed %ld\n", result); return 1; }
        printf("UDP sent %ld bytes\n", result); return 0;
    }
    if (argc == 3 && strcmp(argv[1], "recv") == 0) {
        long socket, start, now;
        port = strtol(argv[2], &end, 10);
        if (*end || port < 1 || port > 65535) return 1;
        socket = _onyx_syscall1(OC2R_UDP_BIND, port);
        if (socket < 0) { printf("UDP bind failed %ld\n", socket); return 1; }
        start = now_seconds();
        if (start < 0) { _onyx_syscall1(OC2R_UDP_CLOSE,socket); return 1; }
        printf("Waiting on UDP %ld for 60 seconds...\n",port);
        do {
            result = _onyx_syscall3(OC2R_UDP_RECV,socket,(long)buffer,1024);
            if (result > 0) {
                buffer[result] = 0; printf("UDP RECEIVED: %s\n",buffer);
                _onyx_syscall1(OC2R_UDP_CLOSE,socket); return 0;
            }
            if (result < 0) break;
            _onyx_syscall0(SYS_yield);
            now = now_seconds();
        } while (now >= start && now - start < 60);
        _onyx_syscall1(OC2R_UDP_CLOSE,socket);
        printf("UDP receive timed out or failed (%ld)\n",result); return 1;
    }
    puts("usage: udpdemo recv PORT | udpdemo send|call IP PORT MESSAGE");
    return 1;
}
