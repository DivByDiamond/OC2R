/* SPDX-License-Identifier: MIT
 * Integration probe: delay reading until the 2048-byte receive window fills,
 * then verify 8192 patterned bytes across repeated window updates/ring wraps.
 */
#include "demo_net.h"
#include <net.h>
#include <errno.h>
static char buffer[512];
static unsigned char ip[4];
int main(int argc,char **argv) {
    long listener=-1,conn=-1,port,n,start,now;char *end;int sender,i;size_t total=0;
    sender=argc==3 && !strcmp(argv[1],"serve");
    if(!sender && !(argc==4 && !strcmp(argv[1],"recv") && !parse_ip(argv[2],ip))){puts("usage: tcpstream serve PORT | tcpstream recv IP PORT");return 1;}
    port=strtol(argv[sender?2:3],&end,10);if(*end || port<1 || port>65535)return 1;
    start=now_seconds();if(start<0)return 1;
    if(sender){
        listener=_onyx_syscall1(100,port);if(listener<0)return 1;
        puts("TCP_STREAM_LISTENING");
        do {conn=_onyx_syscall1(101,listener);if(conn>=0)break;if(conn!=-ENOENT)break;_onyx_syscall0(SYS_yield);now=now_seconds();}while(now>=start && now-start<60);
        _onyx_syscall1(102,listener);
    } else conn=net_connect(ip,(int)port);
    if(conn<0){puts("TCP_STREAM_CONNECT_FAILED");return 1;}
    if(!sender){struct timespec delay;delay.tv_sec=1;delay.tv_nsec=0;nanosleep(&delay,NULL);}
    while(total<8192){
        if(sender){for(i=0;i<512;i++)buffer[i]=(char)('A'+((total+(size_t)i)%26));n=net_send((int)conn,buffer,512);}
        else n=net_recv((int)conn,buffer,512);
        if(n>0){if(!sender)for(i=0;i<n;i++)if(buffer[i]!=(char)('A'+((total+(size_t)i)%26))){puts("TCP_STREAM_DATA_MISMATCH");net_close((int)conn);return 1;}total+=(size_t)n;}
        else if(n<0 && errno!=ENOENT && errno!=EAGAIN)break;
        else if(!sender && n==0)break;
        _onyx_syscall0(SYS_yield);now=now_seconds();if(now<start || now-start>=60)break;
    }
    net_close((int)conn);
    if(total!=8192){printf("TCP_STREAM_FAILED bytes=%lu errno=%d\n",(unsigned long)total,errno);return 1;}
    puts(sender?"TCP_STREAM_SENT_8192":"TCP_STREAM_VERIFIED_8192");return 0;
}
