#ifndef OC2R_RPC_RAII_HPP
#define OC2R_RPC_RAII_HPP

// C++ RAII wrapper around the librpc C API (rpc.h / rpc.c).
//
// The underlying RPC bus is an open file descriptor; this wrapper guarantees
// it is closed on scope exit, even in the presence of early returns or
// exceptions, and keeps devices bound to the bus that found them.
//
// Cross-compile with a RISC-V C++ toolchain, e.g.:
//     riscv64-linux-gnu-g++ -static -Os -std=c++17 -o app.elf app.cpp rpc.c
//
// Example:
//     rpc::Bus bus;
//     if (!bus) return 1;
//     rpc::Device redstone(bus, "redstone");
//     if (redstone) redstone.redstoneOutput("up", 15);

#include "rpc.h"

#include <cstdio>
#include <cstring>
#include <initializer_list>
#include <string>

namespace rpc {

// Builds a JSON parameter array from a list of pre-formatted JSON values,
// e.g. { "\"up\"", "15" } -> ["up",15].
inline std::string buildParams(std::initializer_list<const char *> args) {
    std::string params = "[";
    bool first = true;
    for (const char *arg : args) {
        if (!first) params += ',';
        params += arg ? arg : "";
        first = false;
    }
    params += ']';
    return params;
}

class Bus {
public:
    explicit Bus(const char *path = "/dev/hvc0") { rpc_bus_open(&bus_, path); }

    ~Bus() { rpc_bus_close(&bus_); }

    Bus(const Bus &) = delete;
    Bus &operator=(const Bus &) = delete;

    Bus(Bus &&other) noexcept : bus_(other.bus_) { other.bus_.fd = -1; }

    Bus &operator=(Bus &&other) noexcept {
        if (this != &other) {
            rpc_bus_close(&bus_);
            bus_ = other.bus_;
            other.bus_.fd = -1;
        }
        return *this;
    }

    bool valid() const noexcept { return bus_.fd >= 0; }
    explicit operator bool() const noexcept { return valid(); }

    rpc_bus_t *get() noexcept { return &bus_; }
    const rpc_bus_t *get() const noexcept { return &bus_; }

    int list() { return rpc_bus_list(&bus_); }

    bool find(const char *type_name, rpc_device_t *device) {
        return rpc_bus_find(&bus_, type_name, device);
    }

    int find_all(const char *type_name, rpc_device_t *devices, int max) {
        return rpc_bus_find_all(&bus_, type_name, devices, max);
    }

private:
    rpc_bus_t bus_{};
};

class Device {
public:
    Device(Bus &bus, const char *type_name) : bus_(&bus) {
        rpc_device_t handle{};
        if (rpc_bus_find(bus.get(), type_name, &handle)) {
            std::strncpy(device_.id, handle.id, sizeof(device_.id) - 1);
            device_.id[sizeof(device_.id) - 1] = '\0';
            device_.bus = bus.get();
            valid_ = true;
        }
    }

    bool valid() const noexcept { return valid_; }
    explicit operator bool() const noexcept { return valid_; }

    rpc_device_t *get() noexcept { return valid_ ? &device_ : nullptr; }
    const rpc_device_t *get() const noexcept { return valid_ ? &device_ : nullptr; }

    // Generic method invocation. Pass JSON values as strings, e.g.
    //   sound.play("playTone", { "440.00", "500" })
    int invoke(const char *method_name, std::initializer_list<const char *> args) {
        if (!valid_) return -1;
        return rpc_device_invoke_raw(get(), method_name, buildParams(args).c_str());
    }

    int invokeInt(const char *method_name, std::initializer_list<const char *> args) {
        if (!valid_) return 0;
        return rpc_device_invoke_int_raw(get(), method_name, buildParams(args).c_str());
    }

    double invokeDouble(const char *method_name, std::initializer_list<const char *> args) {
        if (!valid_) return 0.0;
        return rpc_device_invoke_double_raw(get(), method_name, buildParams(args).c_str());
    }

    bool invokeBool(const char *method_name, std::initializer_list<const char *> args) {
        if (!valid_) return false;
        return rpc_device_invoke_bool_raw(get(), method_name, buildParams(args).c_str());
    }

    std::string invokeString(const char *method_name, std::initializer_list<const char *> args) {
        char buffer[RPC_MAX_MESSAGE];
        if (!valid_) return "";
        rpc_device_invoke_string_raw(get(), method_name, buildParams(args).c_str(), buffer,
                                     sizeof(buffer));
        return buffer;
    }

    // Redstone convenience
    int redstoneInput(const char *side) {
        if (!valid_) return 0;
        return rpc_redstone_get_input(get(), side);
    }
    void redstoneOutput(const char *side, int value) {
        if (!valid_) return;
        rpc_redstone_set_output(get(), side, value);
    }

    // Sound convenience
    void playTone(double frequency, int duration_ms) {
        char freq[32];
        char duration[32];
        std::snprintf(freq, sizeof(freq), "%.2f", frequency);
        std::snprintf(duration, sizeof(duration), "%d", duration_ms);
        invoke("playTone", {freq, duration});
    }

private:
    Bus *bus_;
    rpc_device_t device_{};
    bool valid_ = false;
};

} // namespace rpc

#endif // OC2R_RPC_RAII_HPP
