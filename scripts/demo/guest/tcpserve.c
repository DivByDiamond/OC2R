/* SPDX-License-Identifier: MIT */
#include "demo_net.h"
#include <net.h>
#include <errno.h>
static char buffer[1025];
static char reply[]="ONYX-TCP-ECHO\n";
int main(int argc,char **argv) {
    long port,listener,conn=-1,start,now,n;char *end;
    if(argc!=2){puts("usage: tcpserve PORT (one client, 60-second wait)");return 1;}
    port=strtol(argv[1],&end,10);if(*end || port<1 || port>65535)return 1;
    listener=_onyx_syscall1(100,port);if(listener<0){printf("listen failed %ld\n",listener);return 1;}
    start=now_seconds();puts("TCP listening; waiting for a client...");
    do {
        conn=_onyx_syscall1(101,listener);
        if(conn>=0)break;
        if(conn!=-ENOENT)break;
        _onyx_syscall0(SYS_yield);now=now_seconds();
    }while(start>=0 && now>=start && now-start<60);
    _onyx_syscall1(102,listener);
    if(conn<0){printf("accept failed/timeout %ld\n",conn);return 1;}
    start=now_seconds();
    do {
        n=net_recv((int)conn,buffer,1024);
        if(n>0){buffer[n]=0;printf("TCP SERVER RECEIVED: %s\n",buffer);break;}
        if(n==0 || errno!=ENOENT)break;
        _onyx_syscall0(SYS_yield);now=now_seconds();
    }while(start>=0 && now>=start && now-start<60);
    if(n>0){
        size_t sent=0;
        while(sent<sizeof(reply)-1){
            long part=net_send((int)conn,reply+sent,sizeof(reply)-1-sent);
            if(part<0)break;sent+=(size_t)part;
            now=now_seconds();if(now<start || now-start>=60)break;
            _onyx_syscall0(SYS_yield);
        }
        if(sent==sizeof(reply)-1){net_close((int)conn);puts("TCP reply sent.");return 0;}
    }
    net_close((int)conn);puts("TCP request/reply failed.");return 1;
}
