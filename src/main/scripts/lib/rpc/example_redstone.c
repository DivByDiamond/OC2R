#include "rpc.h"
#include <stdio.h>
#include <unistd.h>

// Blink redstone signal on all sides
// Cross-compile: riscv64-linux-gnu-gcc -static -Os -o blink.elf example_redstone.c rpc.c
// Then scp blink.elf to the VM and run it.

static const char *sides[] = {
    "down", "up", "north", "south", "west", "east", NULL
};

int main() {
    rpc_bus_t bus;
    rpc_bus_open(&bus, "/dev/hvc0");

    rpc_device_t redstone;
    if (!rpc_bus_find(&bus, "redstone", &redstone)) {
        fprintf(stderr, "redstone device not found\n");
        rpc_bus_close(&bus);
        return 1;
    }

    printf("Found redstone device\n");

    // Read current state
    for (int i = 0; sides[i]; i++) {
        int level = rpc_redstone_get_input(&redstone, sides[i]);
        printf("Input %s: %d\n", sides[i], level);
    }

    // Blink
    for (int tick = 0; tick < 20; tick++) {
        int val = (tick % 2) * 15;
        for (int i = 0; sides[i]; i++) {
            rpc_redstone_set_output(&redstone, sides[i], val);
        }
        usleep(500000); // 500ms
    }

    // Turn off
    for (int i = 0; sides[i]; i++) {
        rpc_redstone_set_output(&redstone, sides[i], 0);
    }

    rpc_bus_close(&bus);
    return 0;
}
