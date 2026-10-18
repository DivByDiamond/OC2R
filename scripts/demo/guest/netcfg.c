/* SPDX-License-Identifier: MIT */
#include "demo_net.h"
static unsigned char config[32];
static void address(const char *label, int offset) {
    printf("%s %u.%u.%u.%u\n", label, config[offset], config[offset+1], config[offset+2], config[offset+3]);
}
int main(int argc, char **argv) {
    long result;
    if (argc == 7 && strcmp(argv[1], "set") == 0) {
        char *end;
        long index = strtol(argv[2], &end, 10);
        if (*end || index < 0 || index >= 8 || parse_ip(argv[3], config+4) ||
            parse_ip(argv[4], config+12) || parse_ip(argv[5], config+8) || parse_ip(argv[6], config+16)) {
            puts("netcfg: invalid interface/IP/mask/gateway/DNS"); return 1;
        }
        config[0] = (unsigned char)index;
        result = _onyx_syscall2(OC2R_NET_CONFIG, (long)config, 1);
        if (result < 0) { printf("netcfg: set failed %ld\n", result); return 1; }
        puts("Network configured; interface sockets and ARP cache cleared.");
    } else if (argc == 2 && strcmp(argv[1], "list") == 0) {
        unsigned count,index;
        result=_onyx_syscall2(OC2R_NET_CONFIG,(long)config,0);
        if(result<0){printf("netcfg: list failed %ld\n",result);return 1;}
        count=config[28];
        for(index=0;index<count;index++){
            memset(config,0,sizeof(config));config[0]=(unsigned char)index;
            result=_onyx_syscall2(OC2R_NET_CONFIG,(long)config,2);
            if(result<0){printf("netcfg: interface query failed %ld\n",result);return 1;}
            printf("NIC %u configured=%u default=%u\n",index,config[26],config[27]);
            printf("MAC %02x:%02x:%02x:%02x:%02x:%02x\n",config[20],config[21],config[22],config[23],config[24],config[25]);
            address("IP",4);address("MASK",12);address("GATEWAY",8);address("DNS",16);
        }
        return 0;
    } else if (argc != 1 && !(argc == 2 && strcmp(argv[1], "show") == 0)) {
        puts("usage: netcfg [show | list | set NIC IP MASK GATEWAY DNS]"); return 1;
    }
    result = _onyx_syscall2(OC2R_NET_CONFIG, (long)config, 0);
    if (result < 0) { printf("netcfg: read failed %ld\n", result); return 1; }
    printf("Active NIC %u; detected %u\n",config[0],config[28]);
    printf("MAC %02x:%02x:%02x:%02x:%02x:%02x\n",config[20],config[21],config[22],config[23],config[24],config[25]);
    address("IP",4); address("MASK",12); address("GATEWAY",8); address("DNS",16);
    return 0;
}
