#include "rpc.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// Play a melody through the sound card (type "sound"). Demonstrates generic
// method invocation with JSON parameters and the type name lookup helper.
//
// Build inside the guest with TCC:
//     tcc note_block_player.c rpc.c -o note_block_player
// Cross-compile (host):
//     riscv64-linux-gnu-gcc -static -Os -o note_block_player note_block_player.c rpc.c
//
// Requires a sound card in the computer.

typedef struct {
    double frequency;
    int duration_ms;
} note_t;

static const note_t melody[] = {
    // "Twinkle, Twinkle, Little Star", C major
    { 261.63, 400 }, { 261.63, 400 }, { 392.00, 400 }, { 392.00, 400 },
    { 440.00, 400 }, { 440.00, 400 }, { 392.00, 800 },
    { 349.23, 400 }, { 349.23, 400 }, { 329.63, 400 }, { 329.63, 400 },
    { 293.66, 400 }, { 293.66, 400 }, { 261.63, 800 },
    { 392.00, 400 }, { 392.00, 400 }, { 349.23, 400 }, { 349.23, 400 },
    { 329.63, 400 }, { 329.63, 400 }, { 293.66, 800 },
    { 392.00, 400 }, { 392.00, 400 }, { 349.23, 400 }, { 349.23, 400 },
    { 329.63, 400 }, { 329.63, 400 }, { 293.66, 800 },
    { 261.63, 400 }, { 261.63, 400 }, { 392.00, 400 }, { 392.00, 400 },
    { 440.00, 400 }, { 440.00, 400 }, { 392.00, 800 },
    { 349.23, 400 }, { 349.23, 400 }, { 329.63, 400 }, { 329.63, 400 },
    { 293.66, 400 }, { 293.66, 400 }, { 261.63, 1200 },
    { 0.0, 0 } // sentinel
};

int main(void) {
    const char *bus_path = getenv("OC2R_BUS_PATH");
    if (bus_path == NULL) bus_path = "/dev/hvc0";

    rpc_bus_t bus;
    rpc_bus_open(&bus, bus_path);
    if (bus.fd < 0) {
        fprintf(stderr, "failed to open RPC bus at %s\n", bus_path);
        return 1;
    }

    rpc_device_t sound;
    if (!rpc_bus_find(&bus, "sound", &sound)) {
        fprintf(stderr, "sound device not found (sound card required)\n");
        rpc_bus_close(&bus);
        return 1;
    }

    printf("sound device found; playing melody\n");
    for (const note_t *note = melody; note->frequency > 0; note++) {
        char frequency[32];
        char duration[32];
        snprintf(frequency, sizeof(frequency), "%.2f", note->frequency);
        snprintf(duration, sizeof(duration), "%d", note->duration_ms);
        rpc_device_invoke(&sound, "playTone", frequency, duration, NULL);
        usleep((useconds_t)note->duration_ms * 1000);
    }

    rpc_bus_close(&bus);
    return 0;
}
