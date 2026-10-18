/* SPDX-License-Identifier: MIT */
#include "oc2rpc.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
static int transfers;
int oc2_open(oc2_bus *bus,const char *path) { (void)path; memset(bus,0,sizeof(*bus)); return 0; }
void oc2_close(oc2_bus *bus) {
    (void)bus;
    if(transfers != (getenv("MOCK_EMPTY_SLOT") ? 2 : 0)) abort();
    puts("INVENTORY_TRANSFER_PRECONDITION_OK");
}
const char *oc2_list(oc2_bus *bus) { (void)bus; return "[]"; }
int oc2_find(oc2_bus *bus,const char *type,char id[37]) { (void)bus; snprintf(id,37,"%s",type); return 0; }
const char *oc2_methods(oc2_bus *bus,const char *id) { (void)bus; (void)id; return "[]"; }
int oc2_robot_action(oc2_bus *bus,const char *id,const char *method,const char *direction) {
    (void)bus; (void)id; (void)method; (void)direction; return 0;
}
const char *oc2_call(oc2_bus *bus,const char *id,const char *method,const char *parameters) {
    (void)bus;
    if(!strcmp(method,"getSelectedSlot")) return "3";
    if(!strcmp(method,"getStackInSlot")) {
        if(strcmp(id,"robot") || strcmp(parameters,"[3]")) abort();
        return getenv("MOCK_EMPTY_SLOT") ? "null" : "{\"id\":\"minecraft:stone\",\"Count\":1}";
    }
    if(!strcmp(method,"take") || !strcmp(method,"drop")) {
        if(!getenv("MOCK_EMPTY_SLOT") || strcmp(id,"inventory_operations") || strcmp(parameters,"[1,\"up\"]")) abort();
        transfers++; return "1";
    }
    abort();
}
