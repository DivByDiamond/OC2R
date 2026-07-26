#include "rpc.h"

#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <poll.h>
#include <errno.h>

// ── helpers ─────────────────────────────────────────────────────────────────

static void json_escape(const char *in, char *out, size_t out_size) {
    size_t j = 0;
    for (const char *p = in; *p && j + 6 < out_size; p++) {
        switch (*p) {
            case '"':  out[j++] = '\\'; out[j++] = '"';  break;
            case '\\': out[j++] = '\\'; out[j++] = '\\'; break;
            case '\n': out[j++] = '\\'; out[j++] = 'n';  break;
            case '\r': out[j++] = '\\'; out[j++] = 'r';  break;
            case '\t': out[j++] = '\\'; out[j++] = 't';  break;
            default:   out[j++] = *p;                     break;
        }
    }
    out[j] = '\0';
}

static const char *skip_ws(const char *s) {
    while (*s && (unsigned char)*s <= ' ') s++;
    return s;
}

static const char *json_find_key(const char *json, const char *key) {
    size_t klen = strlen(key);
    const char *p = json;
    int depth = 0;
    while (*p) {
        p = skip_ws(p);
        if (*p == '{' || *p == '[') { depth++; p++; continue; }
        if (*p == '}' || *p == ']') { if (depth) depth--; p++; continue; }
        if (depth > 1) { p++; continue; }
        if (*p == '"') {
            const char *ks = p + 1;
            const char *ke = strchr(ks, '"');
            if (!ke) return NULL;
            size_t nk = ke - ks;
            p = ke + 1;
            p = skip_ws(p);
            if (*p == ':') {
                p = skip_ws(p + 1);
                if (nk == klen && strncmp(ks, key, klen) == 0) return p;
            }
        } else {
            p++;
        }
    }
    return NULL;
}

static int json_extract_value(const char *value_ptr, char *out, int out_size) {
    if (!value_ptr || !*value_ptr) return 0;
    const char *p = skip_ws(value_ptr);
    if (!*p) return 0;
    if (*p == '"') {
        const char *end = p + 1;
        while (*end && *end != '"') {
            if (*end == '\\') end++;
            if (*end) end++;
        }
        if (*end != '"') return 0;
        int len = end - p + 1;
        if (len >= out_size) len = out_size - 1;
        strncpy(out, p, len);
        out[len] = '\0';
        return len;
    } else if (*p == '{' || *p == '[') {
        char open = *p;
        char close = (open == '{') ? '}' : ']';
        int depth = 1;
        const char *end = p + 1;
        while (*end && depth > 0) {
            if (*end == open) depth++;
            if (*end == close) depth--;
            if (depth > 0) end++;
        }
        if (depth != 0) return 0;
        int len = end - p + 1;
        if (len >= out_size) len = out_size - 1;
        strncpy(out, p, len);
        out[len] = '\0';
        return len;
    } else {
        const char *end = p;
        while (*end && !isspace((unsigned char)*end) &&
               *end != ',' && *end != ']' && *end != '}') end++;
        int len = end - p;
        if (len >= out_size) len = out_size - 1;
        strncpy(out, p, len);
        out[len] = '\0';
        return len;
    }
}

static int json_extract_string(const char *value_ptr, char *out, int out_size) {
    char buf[4096];
    int len = json_extract_value(value_ptr, buf, sizeof(buf));
    if (len <= 0) return 0;
    if (buf[0] == '"') {
        int slen = strlen(buf);
        if (slen >= 2 && buf[slen - 1] == '"') {
            buf[slen - 1] = '\0';
            strncpy(out, buf + 1, out_size - 1);
            out[out_size - 1] = '\0';
            return strlen(out);
        }
    }
    strncpy(out, buf, out_size - 1);
    out[out_size - 1] = '\0';
    return strlen(out);
}

// ── serial port ─────────────────────────────────────────────────────────────

static int serial_open(const char *path) {
    int fd = open(path, O_RDWR | O_NOCTTY | O_SYNC);
    if (fd < 0) return -1;
    struct termios tty;
    if (tcgetattr(fd, &tty) != 0) { close(fd); return -1; }
    cfsetospeed(&tty, B115200);
    cfsetispeed(&tty, B115200);
    tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8 | CLOCAL | CREAD;
    tty.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    tty.c_oflag &= ~OPOST;
    tty.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    tty.c_cflag &= ~(PARENB | PARODD);
    tty.c_cflag &= ~CSTOPB;
    tty.c_cc[VMIN]  = 1;
    tty.c_cc[VTIME] = 1;
    if (tcsetattr(fd, TCSANOW, &tty) != 0) { close(fd); return -1; }
    return fd;
}

static int serial_write(int fd, const char *data, int len) {
    int total = 0;
    while (total < len) {
        int n = write(fd, data + total, len - total);
        if (n < 0) return n;
        total += n;
    }
    return total;
}

static int serial_read(int fd, char *buf, int max, int timeout_ms) {
    struct pollfd pfd = { .fd = fd, .events = POLLIN };
    int ret = poll(&pfd, 1, timeout_ms);
    if (ret <= 0) return ret;
    return (int)read(fd, buf, max);
}

// ── message helpers ──────────────────────────────────────────────────────────

static int send_message(rpc_bus_t *bus, const char *json_msg) {
    char buf[RPC_MAX_MESSAGE];
    int n = snprintf(buf, sizeof(buf), "%c%s%c", RPC_DELIMITER, json_msg, RPC_DELIMITER);
    return serial_write(bus->fd, buf, n);
}

static int read_message(rpc_bus_t *bus, char *type_out, int type_size,
                        char *data_out, int data_size) {
    char *delim = NULL;
    int msg_start = 0;
    for (int i = 0; i < bus->recv_len; i++) {
        if (bus->recv_buf[i] == RPC_DELIMITER) {
            if (delim == NULL) {
                delim = &bus->recv_buf[i];
                msg_start = i + 1;
            } else {
                int msg_len = i - msg_start;
                if (msg_len > 0) {
                    bus->recv_buf[i] = '\0';
                    const char *msg = &bus->recv_buf[msg_start];
                    const char *tv = json_find_key(msg, "type");
                    if (tv) json_extract_string(tv, type_out, type_size);
                    const char *dv = json_find_key(msg, "data");
                    if (dv) json_extract_value(dv, data_out, data_size);
                    int remaining = bus->recv_len - i - 1;
                    if (remaining > 0)
                        memmove(bus->recv_buf, &bus->recv_buf[i + 1], remaining);
                    bus->recv_len = remaining;
                    return 1;
                }
                delim = NULL;
                msg_start = i + 1;
            }
        }
    }
    if (delim != NULL) {
        int shift = delim - bus->recv_buf;
        int remaining = bus->recv_len - shift;
        memmove(bus->recv_buf, delim, remaining);
        bus->recv_len = remaining;
        msg_start = 1;
    } else if (bus->recv_len > RPC_MAX_MESSAGE / 2) {
        bus->recv_len = 0;
    }
    int max_read = RPC_MAX_MESSAGE - bus->recv_len - 1;
    if (max_read <= 0) { bus->recv_len = 0; max_read = RPC_MAX_MESSAGE - 1; }
    int n = serial_read(bus->fd, bus->recv_buf + bus->recv_len, max_read, 5000);
    if (n <= 0) return n;
    bus->recv_len += n;
    bus->recv_buf[bus->recv_len] = '\0';
    return read_message(bus, type_out, type_size, data_out, data_size);
}

// ── internal invoke (shared by typed wrappers) ──────────────────────────────

static int internal_invoke(rpc_bus_t *bus, const char *device_id,
                           const char *method_name, const char *params_json,
                           char *out_type, int type_size,
                           char *out_data, int data_size) {
    char json_id[80];
    json_escape(device_id, json_id, sizeof(json_id));
    char json_method[128];
    json_escape(method_name, json_method, sizeof(json_method));

    char msg[RPC_MAX_MESSAGE];
    snprintf(msg, sizeof(msg),
             "{\"type\":\"invoke\",\"data\":{"
             "\"deviceId\":\"%s\","
             "\"name\":\"%s\","
             "\"parameters\":%s}}",
             json_id, json_method, params_json);

    bus->recv_len = 0;
    if (send_message(bus, msg) < 0) return -1;
    return read_message(bus, out_type, type_size, out_data, data_size);
}

// ── public API ───────────────────────────────────────────────────────────────

void rpc_bus_open(rpc_bus_t *bus, const char *path) {
    memset(bus, 0, sizeof(*bus));
    bus->fd = serial_open(path);
}

void rpc_bus_close(rpc_bus_t *bus) {
    if (bus->fd >= 0) close(bus->fd);
    bus->fd = -1;
}

void rpc_bus_flush(rpc_bus_t *bus) {
    bus->recv_len = 0;
    char buf[256];
    while (serial_read(bus->fd, buf, sizeof(buf), 0) > 0);
}

int rpc_bus_list(rpc_bus_t *bus) {
    bus->device_count = 0;
    bus->recv_len = 0;
    if (send_message(bus, "{\"type\":\"list\"}") < 0) return -1;
    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = read_message(bus, type, sizeof(type), data, sizeof(data));
    if (ret <= 0) return ret;
    const char *p = data;
    while (*p) {
        const char *id_val = json_find_key(p, "deviceId");
        const char *tn_val = json_find_key(p, "typeNames");
        if (!id_val || !tn_val) break;
        if (bus->device_count >= RPC_MAX_DEVICES) break;
        json_extract_string(id_val, bus->devices[bus->device_count].id,
                            sizeof(bus->devices[bus->device_count].id));
        char tn_raw[4096];
        json_extract_value(tn_val, tn_raw, sizeof(tn_raw));
        if (tn_raw[0] == '[') {
            int tc = 0;
            const char *tp = tn_raw + 1;
            while (*tp && *tp != ']' && tc < RPC_MAX_TYPENAMES) {
                tp = skip_ws(tp);
                if (*tp == '"') {
                    const char *ts = tp + 1;
                    const char *te = strchr(ts, '"');
                    if (!te) break;
                    int tlen = te - ts;
                    if (tlen > 63) tlen = 63;
                    strncpy(bus->devices[bus->device_count].type_names[tc], ts, tlen);
                    bus->devices[bus->device_count].type_names[tc][tlen] = '\0';
                    tc++;
                    tp = te + 1;
                } else if (*tp == ',') {
                    tp++;
                } else {
                    tp++;
                }
            }
            bus->devices[bus->device_count].type_count = tc;
        }
        const char *brace = strchr(p, '}');
        if (brace) p = brace + 1; else break;
        bus->device_count++;
    }
    return bus->device_count;
}

bool rpc_bus_find(rpc_bus_t *bus, const char *type_name, rpc_device_t *device) {
    if (bus->device_count == 0)
        if (rpc_bus_list(bus) <= 0) return false;
    for (int i = 0; i < bus->device_count; i++) {
        for (int j = 0; j < bus->devices[i].type_count; j++) {
            if (strcmp(bus->devices[i].type_names[j], type_name) == 0) {
                device->bus = bus;
                strncpy(device->id, bus->devices[i].id, sizeof(device->id) - 1);
                device->id[sizeof(device->id) - 1] = '\0';
                return true;
            }
        }
    }
    return false;
}

int rpc_bus_find_all(rpc_bus_t *bus, const char *type_name,
                     rpc_device_t *devices, int max) {
    if (bus->device_count == 0)
        if (rpc_bus_list(bus) <= 0) return 0;
    int found = 0;
    for (int i = 0; i < bus->device_count && found < max; i++) {
        for (int j = 0; j < bus->devices[i].type_count; j++) {
            if (strcmp(bus->devices[i].type_names[j], type_name) == 0) {
                devices[found].bus = bus;
                strncpy(devices[found].id, bus->devices[i].id,
                        sizeof(devices[found].id) - 1);
                devices[found].id[sizeof(devices[found].id) - 1] = '\0';
                found++;
                break;
            }
        }
    }
    return found;
}

// ── variadic parameter builder ───────────────────────────────────────────────

static int build_params(char *buf, int buf_size, const char *method_name,
                        va_list *args) {
    int len = snprintf(buf, buf_size, "[");
    const char *arg = va_arg(*args, const char *);
    bool first = true;
    while (arg) {
        if (!first) {
            if (len + 1 >= buf_size) break;
            buf[len++] = ',';
        }
        int alen = strlen(arg);
        if (len + alen >= buf_size) break;
        memcpy(buf + len, arg, alen);
        len += alen;
        first = false;
        arg = va_arg(*args, const char *);
    }
    if (len + 1 >= buf_size) return -1;
    buf[len++] = ']';
    buf[len] = '\0';
    return len;
}

// ── typed invoke wrappers ────────────────────────────────────────────────────

int rpc_device_invoke(rpc_device_t *device, const char *method_name, ...) {
    char params[RPC_MAX_MESSAGE];
    va_list args;
    va_start(args, method_name);
    int plen = build_params(params, sizeof(params), method_name, &args);
    va_end(args);
    if (plen < 0) return -1;

    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = internal_invoke(device->bus, device->id, method_name,
                              params, type, sizeof(type), data, sizeof(data));
    if (ret <= 0) return ret;
    if (strcmp(type, "error") == 0) {
        fprintf(stderr, "RPC error: %s\n", data);
        return -1;
    }
    return 0;
}

int rpc_device_invoke_int(rpc_device_t *device, const char *method_name, ...) {
    char params[RPC_MAX_MESSAGE];
    va_list args;
    va_start(args, method_name);
    build_params(params, sizeof(params), method_name, &args);
    va_end(args);

    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = internal_invoke(device->bus, device->id, method_name,
                              params, type, sizeof(type), data, sizeof(data));
    if (ret <= 0 || strcmp(type, "error") == 0) return 0;
    return atoi(data);
}

double rpc_device_invoke_double(rpc_device_t *device, const char *method_name, ...) {
    char params[RPC_MAX_MESSAGE];
    va_list args;
    va_start(args, method_name);
    build_params(params, sizeof(params), method_name, &args);
    va_end(args);

    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = internal_invoke(device->bus, device->id, method_name,
                              params, type, sizeof(type), data, sizeof(data));
    if (ret <= 0 || strcmp(type, "error") == 0) return 0.0;
    return atof(data);
}

bool rpc_device_invoke_bool(rpc_device_t *device, const char *method_name, ...) {
    char params[RPC_MAX_MESSAGE];
    va_list args;
    va_start(args, method_name);
    build_params(params, sizeof(params), method_name, &args);
    va_end(args);

    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = internal_invoke(device->bus, device->id, method_name,
                              params, type, sizeof(type), data, sizeof(data));
    if (ret <= 0 || strcmp(type, "error") == 0) return false;

    // JSON booleans are lowercase "true" / "false"
    const char *p = skip_ws(data);
    return *p == 't';
}

void rpc_device_invoke_string(rpc_device_t *device, const char *method_name,
                              char *out, size_t out_size, ...) {
    char params[RPC_MAX_MESSAGE];
    va_list args;
    va_start(args, out_size);
    build_params(params, sizeof(params), method_name, &args);
    va_end(args);

    char type[64] = {0};
    char data[RPC_MAX_MESSAGE] = {0};
    int ret = internal_invoke(device->bus, device->id, method_name,
                              params, type, sizeof(type), data, sizeof(data));
    if (ret <= 0 || strcmp(type, "error") == 0) {
        out[0] = '\0';
        return;
    }
    json_extract_string(data, out, out_size);
}

// ── high-level redstone helpers ──────────────────────────────────────────────

int rpc_redstone_get_input(rpc_device_t *device, const char *side) {
    char json_side[32];
    snprintf(json_side, sizeof(json_side), "\"%s\"", side);
    return rpc_device_invoke_int(device, "getRedstoneInput", json_side, NULL);
}

void rpc_redstone_set_output(rpc_device_t *device, const char *side, int value) {
    char json_side[32];
    snprintf(json_side, sizeof(json_side), "\"%s\"", side);
    char json_val[16];
    snprintf(json_val, sizeof(json_val), "%d", value);
    rpc_device_invoke(device, "setRedstoneOutput", json_side, json_val, NULL);
}
