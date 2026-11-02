/* Independent golden authoring oracle: libvterm, never OC2R's parser. */
#include <vterm.h>
#include <stdio.h>
#include <stdlib.h>

static void color(VTermColor c) {
    if (VTERM_COLOR_IS_DEFAULT_FG(&c) || VTERM_COLOR_IS_DEFAULT_BG(&c)) printf("default");
    else if (VTERM_COLOR_IS_INDEXED(&c)) printf("index:%u", c.indexed.idx);
    else printf("#%02X%02X%02X", c.rgb.red, c.rgb.green, c.rgb.blue);
}

int main(int argc, char **argv) {
    if (argc != 4) return 2;
    int width = atoi(argv[1]), height = atoi(argv[2]);
    FILE *input = fopen(argv[3], "rb");
    if (!input) return 3;
    VTerm *vt = vterm_new(height, width);
    vterm_set_utf8(vt, 1);
    VTermScreen *screen = vterm_obtain_screen(vt);
    vterm_screen_enable_altscreen(screen, 1);
    vterm_screen_reset(screen, 1);
    char bytes[4096];
    size_t n;
    while ((n = fread(bytes, 1, sizeof(bytes), input))) vterm_input_write(vt, bytes, n);
    fclose(input);
    vterm_screen_flush_damage(screen);
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            VTermScreenCell cell;
            vterm_screen_get_cell(screen, (VTermPos){y, x}, &cell);
            unsigned int style = cell.attrs.bold | (cell.attrs.underline ? 4 : 0)
                | (cell.attrs.blink ? 8 : 0) | (cell.attrs.reverse ? 16 : 0)
                | (cell.attrs.conceal ? 32 : 0) | (cell.attrs.italic ? 64 : 0)
                | (cell.attrs.strike ? 128 : 0);
            unsigned int attr = cell.attrs.dhl ? cell.attrs.dhl + 1 : cell.attrs.dwl;
            printf("%d\t%d\t%u\t%02X\t%u\t", y, x, cell.chars[0] ? cell.chars[0] : 32, style, attr);
            color(cell.fg); printf("\t"); color(cell.bg); printf("\n");
        }
    }
    vterm_free(vt);
    return 0;
}
