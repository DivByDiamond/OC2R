/* SPDX-License-Identifier: MIT
 * Native Onyx /dev/hvc0 OC2R JSON RPC. One owner/process per bus at a time.
 * This is device RPC, not the Ethernet or host UART console.
 */
#ifndef OC2RPC_H
#define OC2RPC_H
#define OC2_RPC_CAPACITY 32768

typedef struct {
    int fd;
    char receive[512];
    int receive_pos, receive_len;
    char error[256];
    /* Keep large arrays last: the current OnyxCC member-address emitter
     * truncates struct field offsets above the signed 12-bit ADDI range. */
    char response[OC2_RPC_CAPACITY];
} oc2_bus;

int oc2_open(oc2_bus *bus, const char *path);
void oc2_close(oc2_bus *bus);
/* Returned JSON resides in bus->response and is valid until the next call.
 * NULL means failure; bus->error explains the transport/protocol error.
 * RPC errors are failures, never silently converted to false/zero.
 */
const char *oc2_list(oc2_bus *bus);
int oc2_find(oc2_bus *bus, const char *type, char id[37]);
const char *oc2_methods(oc2_bus *bus, const char *id);
const char *oc2_call(oc2_bus *bus, const char *id, const char *method, const char *json_parameters);
/* Wait for the specific queued movement/turn action, checking SUCCESS.
 * Return 0 on success and -1 for FAILURE, missing history or timeout.
 */
int oc2_robot_action(oc2_bus *bus, const char *id, const char *method, const char *direction);
#endif
