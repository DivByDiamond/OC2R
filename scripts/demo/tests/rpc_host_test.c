/* SPDX-License-Identifier: MIT */
#include "oc2rpc.h"
#include <stdio.h>
#include <string.h>
static oc2_bus bus;
int main(int argc,char **argv) {
 (void)argc; char id[37]; const char *r; int i;
 const char *bad[] = {"[not_json]", "[01]", "[1e]", "[\"\\q\"]", "[\"\\u12xx\"]", "[+1]", "[truefalse]", "[1,]"};
 if(oc2_open(&bus,argv[1])) return 1;
 if(oc2_find(&bus,"robot",id)||strcmp(id,"01234567-89ab-cdef-0123-456789abcdef")) return 2;
 r=oc2_call(&bus,id,"getPosition","[]"); if(!r||strcmp(r,"{\"x\":3,\"note\":\"a},b[\\\"\"}")) {printf("%s %s\n",r ? r : "(null)",bus.error);return 3;}
 r=oc2_call(&bus,id,"setSelectedSlot","[0]"); if(!r||strcmp(r,"null"))return 4;
 r=oc2_call(&bus,id,"badMethod","[]"); if(r||!strstr(bus.error,"unknown"))return 5;
 r=oc2_call(&bus,id,"badMethod","[0] garbage"); if(r)return 6;
 for(i=0;i<(int)(sizeof(bad)/sizeof(bad[0]));i++) { r=oc2_call(&bus,id,"badMethod",bad[i]); if(r||!strstr(bus.error,"parameters"))return 9; }
 r=oc2_call(&bus,id,"invalidReply","[]"); if(r||!strstr(bus.error,"malformed"))return 10;
 r=oc2_call(&bus,id,"deep","[]"); if(r||!strstr(bus.error,"malformed"))return 7;
 r=oc2_call(&bus,id,"huge","[]"); if(r||!strstr(bus.error,"exceeds"))return 8;
 puts("RPC_NATIVE_FRAMING_PARSER_OK");oc2_close(&bus);return 0;
}
