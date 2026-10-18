/* Build this host fixture against the patched OnyxApps response.c, using test net.h. */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static unsigned calls;
long net_recv(int connection, void *buffer, size_t capacity) {
    static const char reply[] = "HTTP/1.0 200 OK\r\nConnection: close\r\n\r\nHTTP-EOF-PASS";
    (void) connection;
    if (++calls == 1) {
        assert(capacity >= sizeof(reply) - 1);
        memcpy(buffer, reply, sizeof(reply) - 1);
        return sizeof(reply) - 1;
    }
    if (calls == 2) return 0;
    /* An orderly EOF must terminate the reader, not trigger another busy poll. */
    exit(17);
}

int ohttp_read_response(int connection, const char *output);
int main(int argc, char **argv) {
    char body[32] = {0};
    assert(argc == 2);
    assert(ohttp_read_response(0, argv[1]) == 0);
    FILE *input = fopen(argv[1], "rb");
    assert(input);
    assert(fread(body, 1, sizeof(body), input) == strlen("HTTP-EOF-PASS"));
    assert(!strcmp(body, "HTTP-EOF-PASS"));
    assert(fclose(input) == 0);
    assert(calls == 2);
    puts("OHTTP_PEER_EOF_PASS");
    return 0;
}
