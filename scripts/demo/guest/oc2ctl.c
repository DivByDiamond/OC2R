/* SPDX-License-Identifier: MIT */
#include "oc2rpc.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
static oc2_bus bus;
static char id[37];
static int show(const char *value) {
    if (!value) { printf("oc2ctl: %s\n", bus.error); return 1; }
    puts(value); return 0;
}
static int robot_status(void) {
    if (show(oc2_call(&bus,id,"getPosition","[]"))) return 1;
    if (show(oc2_call(&bus,id,"getFacing","[]"))) return 1;
    if (show(oc2_call(&bus,id,"getEnergyStored","[]"))) return 1;
    return show(oc2_call(&bus,id,"getStackInSlot","[0]"));
}
static int inventory_demo(void) {
    char robot[37], selected[48];
    const char *value;
    if (oc2_find(&bus,"robot",robot)) { printf("oc2ctl: %s\n",bus.error); return 1; }
    value = oc2_call(&bus,robot,"getSelectedSlot","[]");
    if (!value) return show(value);
    snprintf(selected,sizeof(selected),"[%s]",value);
    value = oc2_call(&bus,robot,"getStackInSlot",selected);
    if (!value) return show(value);
    if (strcmp(value,"null")) { puts("oc2ctl: empty the selected robot inventory slot before inventory-demo"); return 1; }
    value = oc2_call(&bus,id,"take","[1,\"up\"]");
    if (!value) return show(value);
    if (strcmp(value,"1")) { puts("oc2ctl: put an item chest directly above the robot and leave inventory space"); return 1; }
    value = oc2_call(&bus,id,"drop","[1,\"up\"]");
    if (!value) return show(value);
    if (strcmp(value,"1")) { puts("oc2ctl: return transfer failed; item remains in robot inventory"); return 1; }
    puts("INVENTORY_DEMO_OK: took one item from chest above and returned it");
    return 0;
}
int main(int argc, char **argv) {
    int result = 0;
    const char *command = argc > 1 ? argv[1] : "help";
    if (!strcmp(command,"help")) {
        puts("oc2ctl list | methods TYPE | call TYPE METHOD [JSON_ARRAY]");
        puts("oc2ctl robot-status | robot-demo | scanner | inspect | inventory | inventory-demo | sound-demo");
        puts("RPC bus: /dev/hvc0. Device type can be TYPE or TYPE:UUID for explicit selection.");
        puts("robot-demo moves forward/back and turns right/left; keep surrounding cells clear. inventory-demo needs an item chest above.");
        return 0;
    }
    if (oc2_open(&bus,"/dev/hvc0")) { printf("oc2ctl: %s\n",bus.error); return 1; }
    if (!strcmp(command,"list")) result = show(oc2_list(&bus));
    else {
        const char *type = NULL;
        if ((!strcmp(command,"call") && argc >= 4) || (!strcmp(command,"methods") && argc == 3)) type = argv[2];
        else if (!strcmp(command,"robot-demo") || !strcmp(command,"robot-status")) type = "robot";
        else if (!strcmp(command,"scanner") || !strcmp(command,"inspect")) type = "scanner";
        else if (!strcmp(command,"inventory") || !strcmp(command,"inventory-demo")) type = "inventory_operations";
        else if (!strcmp(command,"sound-demo")) type = "sound";
        if (!type) { puts("oc2ctl: invalid arguments; run oc2ctl help"); result = 1; }
        else {
            const char *colon = strchr(type,':');
            if (colon && strlen(colon + 1) == 36) { memcpy(id,colon + 1,37); }
            else if (oc2_find(&bus,type,id)) { printf("oc2ctl: %s (%s)\n",bus.error,type); result = 1; }
            if (!result) {
                if (!strcmp(command,"call")) result = show(oc2_call(&bus,id,argv[3],argc > 4 ? argv[4] : "[]"));
                else if (!strcmp(command,"methods")) result = show(oc2_methods(&bus,id));
                else if (!strcmp(command,"robot-status")) result = robot_status();
                else if (!strcmp(command,"robot-demo")) {
                    result = robot_status();
                    if (!result && oc2_robot_action(&bus,id,"move","forward")) result = 1;
                    if (!result && oc2_robot_action(&bus,id,"move","backward")) result = 1;
                    if (!result && oc2_robot_action(&bus,id,"turn","right")) result = 1;
                    if (!result && oc2_robot_action(&bus,id,"turn","left")) result = 1;
                    if (!result) { puts("ROBOT_DEMO_OK: every queued action completed SUCCESS"); result = robot_status(); }
                    else printf("oc2ctl: %s\n",bus.error);
                }
                else if (!strcmp(command,"scanner")) result = show(oc2_call(&bus,id,"scan","[]"));
                else if (!strcmp(command,"inspect")) result = show(oc2_call(&bus,id,"inspect","[\"front\"]"));
                else if (!strcmp(command,"inventory-demo")) result = inventory_demo();
                else if (!strcmp(command,"inventory")) result = show(oc2_methods(&bus,id));
                else if (!strcmp(command,"sound-demo")) result = show(oc2_call(&bus,id,"beep","[880,300]"));
            }
        }
    }
    oc2_close(&bus);
    return result;
}
