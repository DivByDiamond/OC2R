/* SPDX-License-Identifier: MIT
 * Native Onyx regression: this must be the first stdio operation.
 * A preceding printf would initialize streams and hide the old deadlock.
 */
#include <stdio.h>
int main(void) {
    puts("ONYX_STDIO_FIRST_PUTS_OK");
    return 0;
}
