/* SPDX-License-Identifier: MIT */
#include "oc2rpc.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>

static const char *space(const char *p) {
    while (*p == ' ' || *p == '\t' || *p == '\r' || *p == '\n') p++;
    return p;
}
/* Find the end of one JSON value. Strings protect braces/commas inside
 * names, descriptions and RPC error messages; nesting is bounded. */
static int digit(char c) { return c >= '0' && c <= '9'; }
static int hexadecimal(char c) {
    return digit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
}
static const char *value_end(const char *p, int depth) {
    char close;
    if (depth > 32) return NULL;
    p = space(p);
    if (*p == '"') {
        p++;
        while (*p && *p != '"') {
            if ((unsigned char)*p < 32) return NULL;
            if (*p == '\\') {
                p++;
                if (*p == 'u') {
                    int i;
                    for (i = 1; i <= 4; i++) if (!hexadecimal(p[i])) return NULL;
                    p += 4;
                } else if (!*p || !strchr("\"\\/bfnrt", *p)) return NULL;
            }
            p++;
        }
        return *p == '"' ? p + 1 : NULL;
    }
    if (*p == '{' || *p == '[') {
        close = *p == '{' ? '}' : ']';
        p = space(p + 1);
        if (*p == close) return p + 1;
        while (*p) {
            if (close == '}') {
                if (*p != '"') return NULL;
                p = value_end(p, depth + 1);
                if (!p || *space(p) != ':') return NULL;
                p = space(space(p) + 1);
            }
            p = value_end(p, depth + 1);
            if (!p) return NULL;
            p = space(p);
            if (*p == close) return p + 1;
            if (*p != ',') return NULL;
            p = space(p + 1);
        }
        return NULL;
    }
    if (!strncmp(p,"true",4)) return p + 4;
    if (!strncmp(p,"false",5)) return p + 5;
    if (!strncmp(p,"null",4)) return p + 4;
    if (*p == '-') p++;
    if (*p == '0') p++;
    else {
        if (*p < '1' || *p > '9') return NULL;
        while (digit(*p)) p++;
    }
    if (*p == '.') {
        p++;
        if (!digit(*p)) return NULL;
        while (digit(*p)) p++;
    }
    if (*p == 'e' || *p == 'E') {
        p++;
        if (*p == '+' || *p == '-') p++;
        if (!digit(*p)) return NULL;
        while (digit(*p)) p++;
    }
    return p;
}
static int json_string_equal(const char *p, const char *text) {
    int n = (int)strlen(text);
    return p && p[0] == '"' && strncmp(p + 1, text, n) == 0 && p[n+1] == '"';
}
static const char *member(const char *object, const char *key) {
    const char *p = space(object), *end, *name;
    if (*p != '{') return NULL;
    p = space(p + 1);
    while (*p && *p != '}') {
        name = p;
        end = value_end(name, 0);
        if (!end || *space(end) != ':') return NULL;
        p = space(space(end) + 1);
        if (json_string_equal(name, key)) return p;
        end = value_end(p, 0);
        if (!end) return NULL;
        p = space(end);
        if (*p != ',') return NULL;
        p = space(p + 1);
    }
    return NULL;
}
static long milliseconds(void) {
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return -1;
    return ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
static void pause_io(void) {
    struct timespec ts;
    ts.tv_sec = 0; ts.tv_nsec = 1000000;
    nanosleep(&ts, NULL);
}
static int fail(oc2_bus *bus, const char *reason) {
    snprintf(bus->error, sizeof(bus->error), "%s", reason);
    return -1;
}
static int write_bytes(oc2_bus *bus, const char *bytes, int count, long deadline) {
    int offset = 0;
    while (offset < count) {
        long n = write(bus->fd, bytes + offset, count - offset);
        if (n > 0) offset += n;
        else if (n < 0 && errno != EAGAIN && errno != EINTR) return fail(bus, "hvc0 write failed");
        else pause_io();
        if (milliseconds() > deadline) return fail(bus, "hvc0 write timeout");
    }
    return 0;
}
static int read_frame(oc2_bus *bus, long deadline) {
    int inside = 0, length = 0;
    while (milliseconds() <= deadline) {
        if (bus->receive_pos == bus->receive_len) {
            bus->receive_pos = 0;
            bus->receive_len = (int)read(bus->fd, bus->receive, sizeof(bus->receive));
            if (bus->receive_len < 0 && errno != EAGAIN && errno != EINTR) return fail(bus, "hvc0 read failed");
            if (bus->receive_len <= 0) { bus->receive_len = 0; pause_io(); continue; }
        }
        while (bus->receive_pos < bus->receive_len) {
            char byte = bus->receive[bus->receive_pos];
            bus->receive_pos = bus->receive_pos + 1;
            if (!byte) {
                if (inside && length) { bus->response[length] = 0; return 0; }
                inside = 1;
            } else if (inside) {
                if (length >= OC2_RPC_CAPACITY - 1) return fail(bus, "RPC response exceeds 32767 bytes");
                bus->response[length] = byte;
                length = length + 1;
            }
        }
    }
    return fail(bus, "RPC response timeout (5s); check bus connection and power");
}
static const char *request(oc2_bus *bus, const char *json, const char *expected) {
    long now = milliseconds(), deadline;
    const char *type, *data, *end;
    if (bus->fd < 0) { fail(bus, "bus is not open"); return NULL; }
    if (now < 0) { fail(bus, "monotonic clock unavailable"); return NULL; }
    deadline = now + 5000;
    bus->error[0] = 0;
    if (write_bytes(bus, "", 1, deadline) || write_bytes(bus, json, (int)strlen(json), deadline) || write_bytes(bus, "", 1, deadline)) return NULL;
    while (read_frame(bus, deadline) == 0) {
        end = value_end(bus->response, 0);
        if (!end || *space(end)) { fail(bus, "malformed JSON reply"); return NULL; }
        type = member(bus->response, "type");
        data = member(bus->response, "data");
        if (json_string_equal(type, "event")) continue; /* unsolicited device event */
        if (!data && json_string_equal(type, "result") && !strcmp(expected, "result")) {
            strcpy(bus->response, "null"); return bus->response; /* Gson omits void/null data. */
        }
        if (!data) { fail(bus, "reply has no data"); return NULL; }
        if (json_string_equal(type, "error")) { fail(bus, data); return NULL; }
        if (!json_string_equal(type, expected)) { fail(bus, "unexpected RPC reply type"); return NULL; }
        end = value_end(data, 0);
        if (!end) { fail(bus, "malformed reply data"); return NULL; }
        /* Isolate data in the owning response buffer, removing envelope. */
        int n = (int)(end - data);
        memmove(bus->response, data, n); bus->response[n] = 0;
        return bus->response;
    }
    return NULL;
}
int oc2_open(oc2_bus *bus, const char *path) {
    memset(bus, 0, sizeof(*bus));
    bus->fd = open(path, O_RDWR, 0);
    return bus->fd < 0 ? fail(bus, "cannot open raw /dev/hvc0; install RPC kernel patch") : 0;
}
void oc2_close(oc2_bus *bus) { if (bus->fd >= 0) close(bus->fd); bus->fd = -1; }
const char *oc2_list(oc2_bus *bus) { return request(bus, "{\"type\":\"list\"}", "list"); }
int oc2_find(oc2_bus *bus, const char *type, char id[37]) {
    const char *list = oc2_list(bus), *p, *end, *names, *name, *uuid;
    if (!list) return -1;
    if (*list != '[') return fail(bus, "device list is not an array");
    p = space(list + 1);
    while (*p && *p != ']') {
        end = value_end(p, 0);
        if (!end) return fail(bus, "malformed device entry");
        uuid = member(p, "deviceId"); names = member(p, "typeNames");
        if (uuid && names && *names == '[') {
            name = space(names + 1);
            while (*name && *name != ']') {
                if (json_string_equal(name, type)) {
                    const char *uend = value_end(uuid, 0);
                    if (!uend || uend - uuid != 38 || *uuid != '"') return fail(bus, "invalid device UUID");
                    memcpy(id, uuid + 1, 36); id[36] = 0; return 0;
                }
                name = value_end(name, 0);
                if (!name) return fail(bus, "malformed device type");
                name = space(name);
                if (*name != ',') break;
                name = space(name + 1);
            }
        }
        p = space(end);
        if (*p != ',') break;
        p = space(p + 1);
    }
    return fail(bus, "requested device type is not connected");
}
static int identifier(const char *text, int maximum) {
    int i;
    for (i = 0; text[i]; i++) {
        char c = text[i];
        if (i >= maximum || !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_')) return 0;
    }
    return i > 0;
}
const char *oc2_methods(oc2_bus *bus, const char *id) {
    char message[128];
    if (!identifier(id, 36)) { fail(bus, "invalid device UUID"); return NULL; }
    snprintf(message, sizeof(message), "{\"type\":\"methods\",\"data\":\"%s\"}", id);
    return request(bus, message, "methods");
}
const char *oc2_call(oc2_bus *bus, const char *id, const char *method, const char *parameters) {
    char message[8192];
    const char *end = value_end(parameters, 0);
    int n;
    if (!identifier(id, 36) || !identifier(method, 96)) { fail(bus, "invalid UUID or method name"); return NULL; }
    if (*space(parameters) != '[' || !end || *space(end)) { fail(bus, "parameters must be a JSON array"); return NULL; }
    n = snprintf(message, sizeof(message), "{\"type\":\"invoke\",\"data\":{\"deviceId\":\"%s\",\"name\":\"%s\",\"parameters\":%s}}", id, method, parameters);
    if (n < 0 || n >= (int)sizeof(message)) { fail(bus, "request exceeds 8191 bytes"); return NULL; }
    return request(bus, message, "result");
}
int oc2_robot_action(oc2_bus *bus, const char *id, const char *method, const char *direction) {
    char parameters[64], action[48];
    const char *result;
    long deadline;
    if (!identifier(direction, 20) || (strcmp(method, "move") && strcmp(method, "turn"))) return fail(bus, "invalid robot action");
    snprintf(parameters, sizeof(parameters), "[\"%s\"]", direction);
    result = oc2_call(bus, id, method, parameters);
    if (!result) return -1;
    if (strcmp(result, "true")) return fail(bus, "robot rejected action queue request");
    result = oc2_call(bus, id, "getLastActionId", "[]");
    if (!result) return -1;
    snprintf(action, sizeof(action), "[%s]", result);
    deadline = milliseconds() + 15000;
    while (milliseconds() <= deadline) {
        result = oc2_call(bus, id, "getActionResult", action);
        if (!result) return -1;
        if (!strcmp(result, "\"SUCCESS\"")) return 0;
        if (strcmp(result, "\"INCOMPLETE\"")) return fail(bus, "robot action failed or action history unavailable");
        pause_io();
    }
    return fail(bus, "robot movement completion timeout (15s)");
}
