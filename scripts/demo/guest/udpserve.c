/* SPDX-License-Identifier: MIT */
#include "demo_net.h"
#include <errno.h>
static char data[1473];
static unsigned char peer[8];
static char reply[]="ONYX-UDP-ECHO";
int main(int argc,char **argv) {
    long port,socket,start,now,n;char *end;
    if(argc!=2){puts("usage: udpserve PORT (one datagram, 60-second wait)");return 1;}
    port=strtol(argv[1],&end,10);if(*end || port<1 || port>65535)return 1;
    socket=_onyx_syscall1(OC2R_UDP_BIND,port);
    if(socket<0){printf("UDP bind failed %ld\n",socket);return 1;}
    start=now_seconds();puts("UDP listening; waiting for a datagram...");
    do {
        n=_onyx_syscall4(103,socket,(long)data,1472,(long)peer);
        if(n>=0){
            data[n]=0;printf("UDP SERVER RECEIVED: %s\n",data);
            printf("from %u.%u.%u.%u:%u NIC %u\n",peer[0],peer[1],peer[2],peer[3],((unsigned)peer[4]<<8)|peer[5],peer[6]);
            n=_onyx_syscall4(104,socket,(long)peer,(long)reply,sizeof(reply)-1);
            _onyx_syscall1(OC2R_UDP_CLOSE,socket);
            if(n<0){printf("UDP reply failed %ld\n",n);return 1;}puts("UDP reply sent.");return 0;
        }
        if(n!=-ENOENT)break;
        _onyx_syscall0(SYS_yield);now=now_seconds();
    }while(start>=0 && now>=start && now-start<60);
    _onyx_syscall1(OC2R_UDP_CLOSE,socket);printf("UDP receive failed/timeout %ld\n",n);return 1;
}
