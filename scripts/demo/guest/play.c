/* SPDX-License-Identifier: MIT
 * play: terminal DFPWM player for the bundled Onyx guest.
 *
 *   play [-r RATE] [FILE.dfpwm | DIRECTORY]
 *
 * Streams a DFPWM file to a speaker or a sound card on the OC2R device bus
 * (/dev/hvc0, see oc2rpc.h) and shows file name, progress, elapsed and total
 * time and the play/pause state. Without a file it lists the .dfpwm files of the
 * current directory (or of DIRECTORY; /usr/share/music when the current one has
 * none) and lets you pick one. RATE is the rate
 * the file was recorded at, 8000 to 48000 Hz (default 48000).
 *
 * Keys: SPACE or p pause/resume, r restart from the beginning, b back to the
 * list, q or Ctrl-C quit. In the list: UP/DOWN (or k/j) move, ENTER plays, q quits.
 *
 * Limits: the host drops a bus message longer than 4096 bytes and every audio
 * byte costs up to four characters of JSON, so each writeDfpwm call carries
 * PLAY_CHUNK bytes. Audio that is already queued cannot be cancelled: pause and
 * restart take effect after the queued lead (PLAY_LEAD_MS) has played out.
 * Memory is fixed: no allocation, one 32 KiB RPC buffer plus small tables.
 * Nothing here needs more than the ring that may open /dev/hvc0 (ring 1).
 */
#include "oc2rpc.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <termios.h>
#ifndef __onyx__
#include <sys/stat.h>
#include <dirent.h>
#endif

#define PLAY_CHUNK 900       /* 900 * 4 + envelope < 4096, the host message limit */
#define PLAY_LEAD_MS 1000    /* keep about this much audio queued, never the full two seconds */
#define PLAY_MAX_FILES 64
#define PLAY_NAME 48
#define PLAY_PATH 192
#define PLAY_ROWS 16         /* visible list rows; the screen is at least 24 rows */
#define PLAY_BAR 40
#define PLAY_DEFAULT_RATE 48000
#define PLAY_MUSIC "/usr/share/music"   /* listed when the current directory has no .dfpwm file */

enum { RESULT_QUIT, RESULT_BACK };

static oc2_bus bus;
static char device[37];
static struct termios saved_mode;
static int raw_active;
static int rate = PLAY_DEFAULT_RATE;
static char directory[PLAY_PATH];
static char names[PLAY_MAX_FILES][PLAY_NAME];
static int file_count;
static unsigned char chunk[PLAY_CHUNK];
static char parameters[PLAY_CHUNK * 4 + 40];
static char screen[2048];

static void pause_ms(long ms) {
    struct timespec delay;
    delay.tv_sec = ms / 1000;
    delay.tv_nsec = (ms % 1000) * 1000000L;
    nanosleep(&delay, NULL);
}

static long clock_ms(void) {
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return 0;
    return ts.tv_sec * 1000L + ts.tv_nsec / 1000000L;
}

static void put(const char *text) {
    long length = (long)strlen(text), done = 0, n;
    while (done < length) {
        n = write(1, text + done, (size_t)(length - done));
        if (n > 0) done += n;
        else if (n < 0) break;
    }
}

static void raw_on(void) {
    struct termios mode;
    if (tcgetattr(0, &saved_mode) != 0) return;
    mode = saved_mode;
    mode.c_iflag &= ~(IXON | ICRNL | INLCR | IGNCR);
    mode.c_lflag &= ~(ECHO | ICANON | ISIG | IEXTEN);
    mode.c_cc[VMIN] = 0;   /* read() returns at once with whatever key bytes are queued */
    mode.c_cc[VTIME] = 0;
    if (tcsetattr(0, 0, &mode) == 0) raw_active = 1;
}

static void raw_off(void) {
    if (raw_active) tcsetattr(0, 0, &saved_mode);
    raw_active = 0;
    put("\x1b[0m\x1b[?25h\x1b[2J\x1b[H");
}

/* Keys: letters as themselves, 'U' up, 'D' down, 0 none. A lone ESC is ignored. */
static int read_key(void) {
    unsigned char b[8];
    long n = read(0, b, sizeof(b));
    if (n <= 0) return 0;
    if (b[0] == 0x1b) {
        if (n >= 3 && b[1] == '[') return b[2] == 'A' ? 'U' : b[2] == 'B' ? 'D' : 0;
        return 0;
    }
    if (b[0] == 3) return 'q';
    if (b[0] == '\r' || b[0] == '\n') return '\n';
    return b[0];
}

static int die(const char *what, const char *detail) {
    raw_off();
    printf("play: %s%s%s\n", what, detail ? ": " : "", detail ? detail : "");
    return 1;
}

static int ends_with_dfpwm(const char *name) {
    size_t n = strlen(name);
    return n > 6 && strcmp(name + n - 6, ".dfpwm") == 0;
}

static void sort_names(void) {
    char tmp[PLAY_NAME];
    int i, j;
    for (i = 1; i < file_count; i++) {
        memcpy(tmp, names[i], PLAY_NAME);
        for (j = i - 1; j >= 0 && strcmp(names[j], tmp) > 0; j--) memcpy(names[j + 1], names[j], PLAY_NAME);
        memcpy(names[j + 1], tmp, PLAY_NAME);
    }
}

static void add_name(const char *name) {
    if (!ends_with_dfpwm(name) || strlen(name) >= PLAY_NAME || file_count >= PLAY_MAX_FILES) return;
    strcpy(names[file_count++], name);
}

/* Fills names[] from directory. The Onyx readdir syscall walks one entry per call
 * and must be called until it returns 0 so the kernel's cursor starts over. */
static int scan_directory(void) {
    file_count = 0;
#ifdef __onyx__
    {
        char entry[256];
        long r;
        while ((r = _onyx_readdir(directory, entry, sizeof(entry))) > 0) add_name(entry);
        if (r < 0) return -1;
    }
#else
    {
        DIR *dir = opendir(directory);
        struct dirent *entry;
        if (!dir) return -1;
        while ((entry = readdir(dir)) != NULL) add_name(entry->d_name);
        closedir(dir);
    }
#endif
    sort_names();
    return 0;
}

static void format_time(char *out, long seconds) {
    if (seconds < 0) seconds = 0;
    if (seconds > 599999) seconds = 599999;
    snprintf(out, 12, "%d:%02d", (int)(seconds / 60), (int)(seconds % 60));
}

static void clip_name(char *out, const char *name) {
    size_t n = strlen(name);
    if (n > 60) snprintf(out, 64, "...%s", name + n - 57);
    else strcpy(out, name);
}

static void draw_player(const char *name, const char *state, long played_ms, long total_ms, int has_list) {
    char shown[64], now[12], total[12], bar[PLAY_BAR + 1];
    int i, filled = total_ms > 0 ? (int)((long)played_ms * PLAY_BAR / total_ms) : 0;
    if (filled > PLAY_BAR) filled = PLAY_BAR;
    for (i = 0; i < PLAY_BAR; i++) bar[i] = i < filled ? '#' : '-';
    bar[PLAY_BAR] = 0;
    format_time(now, played_ms / 1000);
    format_time(total, total_ms / 1000);
    clip_name(shown, name);
    snprintf(screen, sizeof(screen),
        "\x1b[H\x1b[1m OC2R DFPWM player\x1b[0m\x1b[K\n\x1b[K\n"
        " File:  %s\x1b[K\n"
        " Rate:  %d Hz\x1b[K\n\x1b[K\n"
        " [%s] %3d%%\x1b[K\n"
        " %s / %s   %s\x1b[K\n\x1b[K\n"
        " SPACE pause/resume   r restart   %sq quit\x1b[K\n",
        shown, rate, bar, total_ms > 0 ? (int)((long)played_ms * 100 / total_ms) : 0,
        now, total, state, has_list ? "b list   " : "");
    put(screen);
}

static int json_milliseconds(const char *text) {
    int value = 0;
    if (!text) return -1;
    while (*text >= '0' && *text <= '9') value = value * 10 + (*text++ - '0');
    return value;
}

/* Builds "[[b0,b1,...],RATE]" for n bytes of chunk. */
static void build_parameters(int n) {
    int i, p = 0, value, digits;
    char text[4];
    parameters[p++] = '[';
    parameters[p++] = '[';
    for (i = 0; i < n; i++) {
        value = chunk[i];
        digits = 0;
        do { text[digits++] = (char)('0' + value % 10); value /= 10; } while (value);
        if (i) parameters[p++] = ',';
        while (digits) parameters[p++] = text[--digits];
    }
    parameters[p++] = ']';
    p += snprintf(parameters + p, 24, ",%d]", rate);
    parameters[p] = 0;
}

static int buffered_ms(void) {
    return json_milliseconds(oc2_call(&bus, device, "getBufferedMs", "[]"));
}

/* Returns 1 queued, 0 queue full, -1 error. */
static int send_chunk(int n) {
    const char *result;
    build_parameters(n);
    result = oc2_call(&bus, device, "writeDfpwm", parameters);
    if (!result) return -1;
    return strcmp(result, "true") == 0;
}

static long file_size(int fd) {
    long size = lseek(fd, 0, SEEK_END);
    if (size < 0 || lseek(fd, 0, SEEK_SET) < 0) return -1;
    return size;
}

/* Plays one file. Returns RESULT_QUIT or RESULT_BACK, negative on failure. */
static int play_file(const char *path, const char *name, int has_list) {
    long size, total_ms, sent = 0, sent_ms, played_ms, last_draw = -1000, now, lead;
    int fd, have = 0, have_len = 0, paused = 0, finished = 0, key, queued = 0, status, n;
    const char *state;
    fd = open(path, O_RDONLY, 0);
    if (fd < 0) return -2;
    size = file_size(fd);
    if (size < 0) { close(fd); return -2; }
    total_ms = (long)((long)size * 8000 / rate);
    put("\x1b[2J");
    for (;;) {
        key = read_key();
        if (key == 'q') { close(fd); return RESULT_QUIT; }
        if (key == 'b' && has_list) { close(fd); return RESULT_BACK; }
        if (key == ' ' || key == 'p') { if (!finished) paused = !paused; last_draw = -1000; }
        if (key == 'r') {
            if (lseek(fd, 0, SEEK_SET) < 0) { close(fd); return -2; }
            sent = 0; have = 0; finished = 0; paused = 0; last_draw = -1000;
        }
        if (!finished && !paused) {
            if (!have) {
                n = (int)read(fd, chunk, sizeof(chunk));
                if (n < 0) { close(fd); return -2; }
                if (n == 0) finished = 1;
                else { have = 1; have_len = n; }
            }
            if (have) {
                lead = buffered_ms();
                if (lead < 0) { close(fd); return -1; }
                queued = (int)lead;
                if (queued < PLAY_LEAD_MS) {
                    status = send_chunk(have_len);
                    if (status < 0) { close(fd); return -1; }
                    if (status) { sent += have_len; have = 0; queued += (int)((long)have_len * 8000 / rate); }
                    else pause_ms(60);   /* queue full: wait for it to drain */
                } else pause_ms(60);     /* deep enough: wait for it to drain */
            }
        } else {
            lead = buffered_ms();
            if (lead < 0) { close(fd); return -1; }
            queued = (int)lead;
            pause_ms(100);
        }
        sent_ms = (long)((long)sent * 8000 / rate);
        played_ms = sent_ms - queued;
        if (finished && queued == 0) played_ms = total_ms;
        if (played_ms < 0) played_ms = 0;
        if (played_ms > total_ms) played_ms = total_ms;
        state = finished ? (queued == 0 ? "FINISHED" : "PLAYING (last chunk)") : paused ? "PAUSED" : "PLAYING";
        now = clock_ms();
        if (now - last_draw >= 250 || now < last_draw) {
            draw_player(name, state, played_ms, total_ms, has_list);
            last_draw = now;
        }
    }
}

static void draw_list(int selected, int top) {
    char line[96];
    int i, last = top + PLAY_ROWS;
    snprintf(screen, sizeof(screen), "\x1b[H\x1b[1m OC2R DFPWM player\x1b[0m\x1b[K\n Directory: %.150s\x1b[K\n\x1b[K\n", directory);
    put(screen);
    if (last > file_count) last = file_count;
    for (i = top; i < top + PLAY_ROWS; i++) {
        if (i < last) {
            snprintf(line, sizeof(line), " %s %-46s", i == selected ? "\x1b[7m>" : " ", names[i]);
            put(line);
            put("\x1b[0m\x1b[K\n");
        } else put("\x1b[K\n");
    }
    put("\x1b[K\n UP/DOWN select   ENTER play   q quit\x1b[K\n");
}

static int choose_file(void) {
    int selected = 0, top = 0, key;
    put("\x1b[2J");
    if (file_count == 0) { put("\x1b[H No .dfpwm files in "); put(directory); put("\n"); return -1; }
    for (;;) {
        if (selected < top) top = selected;
        if (selected >= top + PLAY_ROWS) top = selected - PLAY_ROWS + 1;
        draw_list(selected, top);
        while (!(key = read_key())) pause_ms(50);
        if (key == 'q') return -1;
        if ((key == 'U' || key == 'k') && selected > 0) selected--;
        if ((key == 'D' || key == 'j') && selected < file_count - 1) selected++;
        if (key == '\n') return selected;
    }
}

static int is_directory(const char *path) {
    struct stat info;
    return stat(path, &info) == 0 && (info.st_mode & 0170000) == 0040000;
}

int main(int argc, char **argv) {
    const char *argument = NULL, *name;
    char path[PLAY_PATH + PLAY_NAME + 2];
    int i, result, choice;
    for (i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-r") && i + 1 < argc) {
            char *end;
            long value = strtol(argv[++i], &end, 10);
            if (*end || value < 8000 || value > 48000) { puts("play: RATE must be 8000 to 48000"); return 1; }
            rate = (int)value;
        } else if (argument || argv[i][0] == '-') {
            puts("usage: play [-r RATE] [FILE.dfpwm | DIRECTORY]");
            return 1;
        } else argument = argv[i];
    }
    if (oc2_open(&bus, "/dev/hvc0")) { printf("play: %s\n", bus.error); return 1; }
    if (oc2_find(&bus, "speaker", device) && oc2_find(&bus, "sound", device)) {
        printf("play: a speaker or a sound card is required (%s)\n", bus.error);
        return 1;
    }
    if (argument && !is_directory(argument)) {
        if (strlen(argument) >= PLAY_PATH) { puts("play: path too long"); return 1; }
        name = strrchr(argument, '/');
        raw_on();
        result = play_file(argument, name ? name + 1 : argument, 0);
        if (result == -1) return die("device error", bus.error);
        if (result == -2) return die("cannot read", argument);
        raw_off();
        return 0;
    }
    if (argument) {
        if (strlen(argument) >= PLAY_PATH) { puts("play: path too long"); return 1; }
        strcpy(directory, argument);
    } else if (!getcwd(directory, sizeof(directory))) strcpy(directory, "/");
    if (scan_directory() != 0) { printf("play: cannot list %s\n", directory); return 1; }
    if (!argument && file_count == 0) {   /* a child does not inherit the shell's cd */
        strcpy(directory, PLAY_MUSIC);
        if (scan_directory() != 0) file_count = 0;
    }
    raw_on();
    for (;;) {
        choice = choose_file();
        if (choice < 0) break;
        snprintf(path, sizeof(path), "%s%s%s", directory, strcmp(directory, "/") ? "/" : "", names[choice]);
        result = play_file(path, names[choice], 1);
        if (result == -1) return die("device error", bus.error);
        if (result == -2) return die("cannot read", path);
        if (result == RESULT_QUIT) break;
    }
    raw_off();
    if (file_count == 0) printf("play: no .dfpwm files in %s\n", directory);
    return 0;
}
