/* SPDX-License-Identifier: MIT
 * Native RV64 regression: run inside Onyx, not with the host C compiler.
 * Old OnyxCC folds this member address through a raw signed-12-bit ADDI,
 * yielding offset 0. The corrected materializer must preserve 32768.
 */
#include <stdio.h>
struct large_record { char payload[32768]; long tail; };
static struct large_record record;
static long member_offset(struct large_record *value) {
    return (long)&value->tail - (long)value;
}
int main(void) {
    long offset = member_offset(&record);
    printf("LARGE_MEMBER_OFFSET %ld\n", offset);
    if (offset != 32768) return 1;
    puts("ONYXCC_LARGE_MEMBER_OK");
    return 0;
}
