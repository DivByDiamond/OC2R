#ifndef RPC_H
#define RPC_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#define RPC_MAX_DEVICES 32
#define RPC_MAX_TYPENAMES 8
#define RPC_MAX_METHODS 64
#define RPC_MAX_MESSAGE 16384
#define RPC_DELIMITER '\0'

typedef struct {
    char id[37]; // UUID string
    char type_names[RPC_MAX_TYPENAMES][64];
    int type_count;
} rpc_device_info_t;

typedef struct {
    int fd;
    rpc_device_info_t devices[RPC_MAX_DEVICES];
    int device_count;
    char recv_buf[RPC_MAX_MESSAGE];
    int recv_len;
} rpc_bus_t;

typedef struct {
    rpc_bus_t *bus;
    char id[37];
} rpc_device_t;

// Bus lifecycle
void rpc_bus_open(rpc_bus_t *bus, const char *path);
void rpc_bus_close(rpc_bus_t *bus);
void rpc_bus_flush(rpc_bus_t *bus);

// Device discovery
int rpc_bus_list(rpc_bus_t *bus);
bool rpc_bus_find(rpc_bus_t *bus, const char *type_name, rpc_device_t *device);
int rpc_bus_find_all(rpc_bus_t *bus, const char *type_name, rpc_device_t *devices, int max);

// Method invocation
int rpc_device_invoke(rpc_device_t *device, const char *method_name,
                      ...); // args: alternating const char *json_value, ... (sentinel NULL)
int rpc_device_invoke_int(rpc_device_t *device, const char *method_name, ...);
double rpc_device_invoke_double(rpc_device_t *device, const char *method_name, ...);
bool rpc_device_invoke_bool(rpc_device_t *device, const char *method_name, ...);
void rpc_device_invoke_string(rpc_device_t *device, const char *method_name,
                              char *out, size_t out_size, ...);

// Raw invocation with a pre-built JSON parameter array (e.g. "[\"up\",15]").
// The variadic helpers above build this array for you; the *_raw variants let
// callers assemble it programmatically (used by the C++ RAII wrapper).
int rpc_device_invoke_raw(rpc_device_t *device, const char *method_name,
                          const char *params_json);
int rpc_device_invoke_int_raw(rpc_device_t *device, const char *method_name,
                              const char *params_json);
double rpc_device_invoke_double_raw(rpc_device_t *device, const char *method_name,
                                    const char *params_json);
bool rpc_device_invoke_bool_raw(rpc_device_t *device, const char *method_name,
                                const char *params_json);
void rpc_device_invoke_string_raw(rpc_device_t *device, const char *method_name,
                                  const char *params_json, char *out, size_t out_size);

// High-level redstone helpers
int rpc_redstone_get_input(rpc_device_t *device, const char *side);
void rpc_redstone_set_output(rpc_device_t *device, const char *side, int value);

#ifdef __cplusplus
}
#endif

#endif // RPC_H
