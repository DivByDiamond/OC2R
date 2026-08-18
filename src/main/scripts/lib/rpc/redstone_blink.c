#include "rpc.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

// Blink the redstone output on a configurable side, inverting whatever
// signal is present on the same side's input. Useful as a demo of the
// librpc API and as a stand-in for a real-time controller loop.
//
// Build inside the guest with TCC:
//     tcc redstone_blink.c rpc.c -o redstone_blink
// Cross-compile (host):
//     riscv64-linux-gnu-gcc -static -Os -o redstone_blink redstone_blink.c rpc.c
//
// Usage: redstone_blink [side]
//   side: one of up, down, north, south, west, east (default: up)

static const char *valid_sides[] = {
    "down", "up", "north", "south", "west", "east", NULL
};

static int is_valid_side(const char *side) {
    for (int i = 0; valid_sides[i]; i++)
        if (strcmp(side, valid_sides[i]) == 0)
            return 1;
    return 0;
}

int main(int argc, char **argv) {
    const char *side = argc > 1 ? argv[1] : "up";
    if (!is_valid_side(side)) {
        fprintf(stderr, "invalid side '%s'\n", side);
        return 2;
    }

    const char *bus_path = getenv("OC2R_BUS_PATH");
    if (bus_path == NULL) bus_path = "/dev/hvc0";

    rpc_bus_t bus;
    rpc_bus_open(&bus, bus_path);
    if (bus.fd < 0) {
        fprintf(stderr, "failed to open RPC bus at %s\n", bus_path);
        return 1;
    }

    rpc_device_t redstone;
    if (!rpc_bus_find(&bus, "redstone", &redstone)) {
        fprintf(stderr, "redstone device not found\n");
        rpc_bus_close(&bus);
        return 1;
    }

    printf("redstone device found; blinking %s (ctrl-c to stop)\n", side);
    for (;;) {
        int input = rpc_redstone_get_input(&redstone, side);
        int output = input > 0 ? 0 : 15;
        rpc_redstone_set_output(&redstone, side, output);
        printf("%s input=%d output=%d\n", side, input, output);
        usleep(250000); // 250ms
    }

    rpc_bus_close(&bus);
    return 0;
}
