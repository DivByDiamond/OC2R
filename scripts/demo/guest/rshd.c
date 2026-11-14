/* SPDX-License-Identifier: MIT
 * rshd: remote line shell for the bundled Onyx guest.
 *
 * A plain TCP client (nc, ncat, socat) gets a prompt; every line it sends is run
 * by /bin/osh and the output comes back over the same socket. Usage:
 *   rshd PORT PASSWORD | rshd PORT --no-password   starts in the background
 *   rshd -f PORT PASSWORD                          stays in the foreground
 *   rshd                                           foreground, reads /etc/rshd.conf
 * The last form is what init runs for /service/rshd; the file holds one line,
 * the port and the password separated by a space. Nothing is encrypted: the
 * password and every command and output cross the network in clear text.
 *
 * One session at a time. net_recv blocks without a timeout, so a client that
 * connects and stays silent holds the service until it disconnects. Every line
 * is a fresh osh process started in the session's directory (cd is carried over
 * by rshd); osh needs the kernel to pass bound standard streams to spawn.
 */
#include "demo_net.h"
#include <net.h>
#include <errno.h>

#define RSH_SYS_READ 2
#define RSH_SYS_CLOSE 9
#define RSH_SYS_OPEN 8
#define RSH_SYS_LSEEK 10
#define RSH_SYS_SPAWN 14
#define RSH_SYS_KILL 22
#define RSH_SYS_WRITE_FD 24
#define RSH_SYS_CREATE 25
#define RSH_SYS_UNLINK 37
#define RSH_SYS_WAITPID 51
#define RSH_SYS_FCNTL 44
#define RSH_BIND_STDIO 0x4f01
#define RSH_SIGKILL 9
#define RSH_WNOHANG 1
#define RSH_O_WRONLY 1
#define RSH_O_CREAT 0x40
#define RSH_O_TRUNC 0x200
#define RSH_O_APPEND 0x400
#define RSH_LINE 250
#define RSH_CHUNK 512
#define RSH_TRIES 3
#define RSH_SEND_TIMEOUT 30
#define RSH_CMD_TIMEOUT 120
#define RSH_OUT_LIMIT (32 * 1024)
#define RSH_OUT_SAFE (40 * 1024)
#define RSH_CONF "/etc/rshd.conf"
#define RSH_OUT "/tmp/rshd.out."
#define RSH_IN "/tmp/rshd.in"
#define RSH_CMD "/tmp/rshd.cmd"
#define RSH_CWD "/tmp/rshd.cwd"

static unsigned char rx[RSH_CHUNK];
static int rx_len, rx_pos;
static char password[RSH_LINE + 1];
static int need_password = 1;
static char cwd[RSH_LINE + 1];
static long out_seq;

static void pause_ms(long ms) {
    struct timespec delay;
    delay.tv_sec = ms / 1000;
    delay.tv_nsec = (ms % 1000) * 1000000L;
    nanosleep(&delay, NULL);
}

static int rx_byte(int conn) {
    if (rx_pos == rx_len) {
        long n = net_recv(conn, rx, sizeof(rx));
        if (n <= 0) return -1;
        rx_len = (int)n;
        rx_pos = 0;
    }
    return rx[rx_pos++];
}

/* Reads one line without its terminator. Returns its length, -1 at EOF or on a
 * connection error, -2 when the line was longer than RSH_LINE (it is dropped). */
static int read_line(int conn, char *line) {
    int len = 0, c, overflow = 0;
    for (;;) {
        c = rx_byte(conn);
        if (c < 0) return -1;
        if (c == '\n') break;
        if (c == 4 && len == 0) return -1;
        if (c == '\r' || c == 0) continue;
        if (c == 8 || c == 127) { if (len > 0) len--; continue; }
        if (len < RSH_LINE) line[len++] = (char)c; else overflow = 1;
    }
    line[len] = 0;
    return overflow ? -2 : len;
}

static int send_all(int conn, const char *data, long length) {
    long sent = 0, part, start = now_seconds(), now;
    while (sent < length) {
        part = net_send(conn, data + sent, (size_t)(length - sent));
        if (part < 0) return -1;
        if (part > 0) { sent += part; start = now_seconds(); continue; }
        now = now_seconds();
        if (now < start || now - start >= RSH_SEND_TIMEOUT) return -1;
        pause_ms(20);
    }
    return 0;
}

static int send_text(int conn, const char *text) {
    return send_all(conn, text, (long)strlen(text));
}

static int same_password(const char *line) {
    size_t i, a = strlen(line), b = strlen(password);
    unsigned char diff = (unsigned char)(a != b);
    for (i = 0; i < RSH_LINE; i++) diff |= (unsigned char)((i < a ? line[i] : 0) ^ (i < b ? password[i] : 0));
    return diff == 0;
}

static int authenticate(int conn) {
    char line[RSH_LINE + 1];
    int tries, n;
    if (!need_password) return 1;
    for (tries = 0; tries < RSH_TRIES; tries++) {
        if (send_text(conn, "Password: ")) return 0;
        n = read_line(conn, line);
        if (n == -1) return 0;
        if (n >= 0 && same_password(line)) return 1;
        pause_ms(1000);
        if (send_text(conn, "Permission denied.\n")) return 0;
    }
    return 0;
}

static long file_open(const char *path, long flags) {
    return _onyx_syscall3(RSH_SYS_OPEN, (long)path, flags, 0);
}

static int file_put(const char *path, const char *text) {
    long fd = _onyx_syscall3(RSH_SYS_CREATE, (long)path, 0644, 0);
    if (fd < 0) return -1;
    _onyx_syscall3(RSH_SYS_WRITE_FD, fd, (long)text, (long)strlen(text));
    _onyx_syscall1(RSH_SYS_CLOSE, fd);
    return 0;
}

static void load_cwd(void) {
    char text[RSH_LINE + 1];
    long fd = file_open(RSH_CWD, 0), n;
    size_t i;
    if (fd < 0) return;
    n = _onyx_syscall3(RSH_SYS_READ, fd, (long)text, RSH_LINE);
    _onyx_syscall1(RSH_SYS_CLOSE, fd);
    if (n <= 0 || text[0] != '/') return;
    for (i = 0; i < (size_t)n && text[i] != '\n' && text[i] != '\r'; i++) cwd[i] = text[i];
    cwd[i] = 0;
}

/* Sends what the command wrote since *sent. Returns -1 when the client is gone. */
static int drain(int conn, const char *path, long *sent) {
    char chunk[RSH_CHUNK];
    long fd = file_open(path, 0), n;
    int status = 0;
    if (fd < 0) return 0;
    _onyx_syscall3(RSH_SYS_LSEEK, fd, *sent, 0);
    while ((n = _onyx_syscall3(RSH_SYS_READ, fd, (long)chunk, sizeof(chunk))) > 0) {
        *sent += n;
        if (send_all(conn, chunk, n)) { status = -1; break; }
    }
    _onyx_syscall1(RSH_SYS_CLOSE, fd);
    return status;
}

/* Runs one line in a new osh whose standard streams are files. Returns -1 when
 * the connection failed while the output was being delivered. */
static int run_line(int conn, const char *line) {
    char script[3 * RSH_LINE + 64], *argv[3], path[40];
    long out, in, pid, sent = 0, start, now, status = 0, waited = 0;
    int result = 0;
    snprintf(script, sizeof(script), "cd %s\n%s\npwd > " RSH_CWD "\n", cwd, line);
    /* A new output file per command: OnyxFS cannot truncate or remove a file that
     * has grown past its ten direct blocks (40 KiB) without corrupting the volume,
     * so output files are never reused and only small ones are deleted. */
    for (;;) {
        snprintf(path, sizeof(path), RSH_OUT "%ld", ++out_seq);
        in = file_open(path, 0);
        if (in < 0) break;
        _onyx_syscall1(RSH_SYS_CLOSE, in);
    }
    out = file_open(path, RSH_O_WRONLY | RSH_O_CREAT | RSH_O_TRUNC | RSH_O_APPEND);
    in = file_open(RSH_IN, 0);
    if (file_put(RSH_CMD, script) || out < 0 || in < 0) {
        if (out >= 0) _onyx_syscall1(RSH_SYS_CLOSE, out);
        if (in >= 0) _onyx_syscall1(RSH_SYS_CLOSE, in);
        return send_text(conn, "rshd: cannot create the files in /tmp\n");
    }
    argv[0] = "/bin/osh"; argv[1] = RSH_CMD; argv[2] = NULL;
    _onyx_syscall3(RSH_SYS_FCNTL, in, RSH_BIND_STDIO, 0);
    _onyx_syscall3(RSH_SYS_FCNTL, out, RSH_BIND_STDIO, 1);
    _onyx_syscall3(RSH_SYS_FCNTL, out, RSH_BIND_STDIO, 2);
    pid = _onyx_syscall3(RSH_SYS_SPAWN, (long)"/bin/osh", (long)argv, 1);
    _onyx_syscall3(RSH_SYS_FCNTL, -1, RSH_BIND_STDIO, 0);
    _onyx_syscall3(RSH_SYS_FCNTL, -1, RSH_BIND_STDIO, 1);
    _onyx_syscall3(RSH_SYS_FCNTL, -1, RSH_BIND_STDIO, 2);
    _onyx_syscall1(RSH_SYS_CLOSE, out);
    _onyx_syscall1(RSH_SYS_CLOSE, in);
    if (pid <= 0) return send_text(conn, "rshd: cannot start /bin/osh\n");
    start = now_seconds();
    for (;;) {
        waited = _onyx_syscall3(RSH_SYS_WAITPID, pid, (long)&status, RSH_WNOHANG);
        if (waited != 0) break;
        if (drain(conn, path, &sent)) { result = -1; break; }
        now = now_seconds();
        if (sent > RSH_OUT_LIMIT || now < start || now - start >= RSH_CMD_TIMEOUT) {
            _onyx_syscall2(RSH_SYS_KILL, pid, RSH_SIGKILL);
            _onyx_syscall3(RSH_SYS_WAITPID, pid, (long)&status, 0);
            if (send_text(conn, sent > RSH_OUT_LIMIT ? "\nrshd: output limit reached, command killed\n"
                                                    : "\nrshd: command timed out, killed\n")) result = -1;
            break;
        }
        pause_ms(50);
    }
    if (result == 0 && drain(conn, path, &sent)) result = -1;
    if (sent <= RSH_OUT_SAFE) _onyx_syscall1(RSH_SYS_UNLINK, (long)path);
    else if (send_text(conn, "rshd: large output file left in /tmp, remove it with rm\n")) result = -1;
    load_cwd();
    return result;
}

static void session(int conn) {
    char line[RSH_LINE + 1];
    int n;
    if (!authenticate(conn)) { puts("rshd: login failed or client left"); return; }
    puts("rshd: session opened");
    if (send_text(conn, "OnyxOS remote shell. Type exit to leave.\n")) return;
    for (;;) {
        if (send_text(conn, "osh$ ")) return;
        n = read_line(conn, line);
        if (n == -1) return;
        if (n == -2) { if (send_text(conn, "rshd: line too long\n")) return; continue; }
        if (n == 0) continue;
        if (!strcmp(line, "exit") || !strcmp(line, "quit") || !strcmp(line, "logout")) {
            send_text(conn, "bye\n");
            return;
        }
        if (run_line(conn, line)) return;
    }
}

/* /etc/rshd.conf holds one line: the port, a space, then the password. */
static int load_conf(long *port) {
    char text[RSH_LINE + 32];
    long fd = file_open(RSH_CONF, 0), n, i = 0, len = 0;
    char *end;
    if (fd < 0) { puts("rshd: no arguments and no " RSH_CONF); return -1; }
    n = _onyx_syscall3(RSH_SYS_READ, fd, (long)text, sizeof(text) - 1);
    _onyx_syscall1(RSH_SYS_CLOSE, fd);
    if (n <= 0) return -1;
    text[n] = 0;
    while (text[i] && text[i] != ' ' && text[i] != '\n') i++;
    if (text[i] != ' ') return -1;
    text[i++] = 0;
    *port = strtol(text, &end, 10);
    if (*end) return -1;
    for (; text[i + len] && text[i + len] != '\n' && text[i + len] != '\r'; len++) password[len] = text[i + len];
    password[len] = 0;
    return len == 0 || len > RSH_LINE ? -1 : 0;
}

/* Starts a detached copy of this tool. The shell's own `&` runs a forked copy of
 * the shell that shares its address space and faults, so the service is spawned
 * as an independent process instead. */
static int start_detached(char **argv) {
    static char port_arg[16], password_arg[RSH_LINE + 1];
    char *child[5];
    long pid;
    /* The kernel stops copying an argv at a pointer into the caller's stack, so
     * the strings handed to spawn live in static storage. */
    strncpy(port_arg, argv[1], sizeof(port_arg) - 1);
    strncpy(password_arg, argv[2], sizeof(password_arg) - 1);
    child[0] = "/bin/rshd"; child[1] = "-f"; child[2] = port_arg; child[3] = password_arg; child[4] = NULL;
    pid = _onyx_syscall3(RSH_SYS_SPAWN, (long)child[0], (long)child, 1);
    if (pid <= 0) {
        printf("rshd: cannot start /bin/rshd (%ld); with little memory run rshd -f instead\n", pid);
        return 1;
    }
    printf("rshd: started in the background, pid %ld (stop it with: kill %ld)\n", pid, pid);
    return 0;
}

int main(int argc, char **argv) {
    long port = 0, listener, conn;
    int foreground = argc <= 1;
    char *end;
    if (argc > 1 && !strcmp(argv[1], "-f")) { foreground = 1; argc--; argv++; }
    if (argc <= 1) {
        if (load_conf(&port)) { puts("rshd: " RSH_CONF " must read: PORT PASSWORD"); return 1; }
    } else {
        if (argc != 3) {
            puts("usage: rshd [-f] PORT PASSWORD | rshd [-f] PORT --no-password | rshd (service: reads " RSH_CONF ")");
            puts("without -f the service starts in the background; the password is sent in clear text");
            return 1;
        }
        port = strtol(argv[1], &end, 10);
        if (*end) { puts("rshd: bad port"); return 1; }
        if (!strcmp(argv[2], "--no-password")) need_password = 0;
        else if (strlen(argv[2]) == 0 || strlen(argv[2]) > RSH_LINE) { puts("rshd: bad password length"); return 1; }
        else strcpy(password, argv[2]);
    }
    if (port < 1 || port > 65535) { puts("rshd: port must be 1..65535"); return 1; }
    if (!foreground) return start_detached(argv);
    if (file_put(RSH_IN, "") || file_put(RSH_CWD, "/\n")) { puts("rshd: cannot write to /tmp"); return 1; }
    listener = _onyx_syscall1(100, port);
    if (listener < 0) { printf("rshd: listen failed %ld\n", listener); return 1; }
    printf("rshd: listening on TCP port %ld (%s)\n", port, need_password ? "password required" : "NO PASSWORD");
    for (;;) {
        conn = _onyx_syscall1(101, listener);
        if (conn == -ENOENT) { pause_ms(20); continue; }
        if (conn < 0) { printf("rshd: accept failed %ld\n", conn); break; }
        rx_len = rx_pos = 0;
        strcpy(cwd, "/");
        session((int)conn);
        net_close((int)conn);
        puts("rshd: session closed");
    }
    _onyx_syscall1(102, listener);
    return 1;
}
