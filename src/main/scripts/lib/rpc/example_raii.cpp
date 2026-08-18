#include "rpc_raii.hpp"

#include <chrono>
#include <cstdio>
#include <thread>

// Demo of the C++ RAII wrapper (rpc_raii.hpp). The rpc::Bus and rpc::Device
// destructors clean up automatically; no explicit rpc_bus_close() needed.
//
// Cross-compile:
//     riscv64-linux-gnu-g++ -static -Os -std=c++17 -o example_raii example_raii.cpp rpc.c
//
// Requires a redstone interface and (optionally) a sound card.

int main() {
    rpc::Bus bus;
    if (!bus) {
        std::fprintf(stderr, "failed to open RPC bus\n");
        return 1;
    }

    rpc::Device redstone(bus, "redstone");
    if (!redstone) {
        std::fprintf(stderr, "redstone device not found\n");
        return 1;
    }

    std::printf("redstone device found\n");

    // Read current input state on all sides (generic invocation).
    for (const char *side : {"down", "up", "north", "south", "west", "east"}) {
        std::string side_arg = "\"" + std::string(side) + "\"";
        int level = redstone.invokeInt("getRedstoneInput", {side_arg.c_str()});
        std::printf("input %s: %d\n", side, level);
    }

    // Blink on "up" using the typed RAII convenience method.
    for (int tick = 0; tick < 20; tick++) {
        redstone.redstoneOutput("up", (tick % 2) * 15);
        std::this_thread::sleep_for(std::chrono::milliseconds(250));
    }
    redstone.redstoneOutput("up", 0);

    // If a sound card is present, play a short beep through it.
    rpc::Device sound(bus, "sound");
    if (sound) {
        std::printf("sound device found; beeping\n");
        sound.invoke("playTone", {"880.00", "250"});
    }

    return 0;
}
