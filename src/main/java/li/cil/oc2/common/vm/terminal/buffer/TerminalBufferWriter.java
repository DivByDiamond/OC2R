package li.cil.oc2.common.vm.terminal.buffer;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;

public class TerminalBufferWriter {
    private final Terminal terminal;

    public TerminalBufferWriter(final Terminal terminal) {
        this.terminal = terminal;
    }

    public void putChar(final int ch) {
        if (Character.isISOControl(ch)) return;

        // Deferred autowrap: if the previous printable filled the last column it left the
        // cursor at width-1 with autowrapPending set. Fire the wrap now, on THIS printable,
        // not the one that filled the margin — so the margin char survives and any control
        // char (BS/CR/Tab) between the two prints can clear the pending wrap instead.
        if (terminal.autowrapPending) {
            if (terminal.currentPrivateModeState.DECAWM) {
                NEL.execute(terminal); // moves to (0, y+1) and clears pending via setCursorPos
            } else {
                terminal.autowrapPending = false; // DECAWM off: overwrite the last column
            }
        }

        // IRM (Insert Mode): shift characters right before placing new char
        if (terminal.currentModeState.IRM) {
            terminal.bufferManager.insertChars(terminal.y, terminal.x, 1);
        }

        final int mapped = mapDecSpecialGraphics(ch);
        setChar(terminal.x, terminal.y, mapped);
        terminal.lastPrintedChar = mapped; // remember for REP (CSI Ps b) — xterm repeats with current charset
        // Fill the last column: arm the pending wrap and hold the cursor at width-1
        // (never advance to a phantom width). Otherwise advance normally.
        if (terminal.x == terminal.width - 1) {
            if (terminal.currentPrivateModeState.DECAWM) {
                terminal.autowrapPending = true;
            }
        } else {
            terminal.x++;
        }
    }

    private void setChar(final int x, final int y, final int ch) { // NOPMD: data-driven foreground color-mode switch
        final boolean altBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int index = altBuffer
                ? x + y * terminal.width
                : x + (y + terminal.lastRowToDisplayMax - terminal.height) * terminal.width;

        // Write the character
        if (altBuffer) {
            terminal.altBuffer[index] = ch;
        } else {
            terminal.buffer[index] = ch;
        }

        // Write foreground color
        final TerminalColors.ColorData fgColor;
        switch (terminal.currentForegroundColorMode) {
            case SIXTEEN_COLOR -> fgColor = terminal.sixteenColor.copy();
            case TWO_FIFTY_SIX_COLOR -> fgColor = terminal.twoFiftySixColor.copy();
            case TRUE_COLOR -> fgColor = terminal.foregroundColor.copy();
            case SIXTEEN_COLOR_BRIGHT -> fgColor = terminal.sixteenColorBright.copy();
            case DEFAULT_FOREGROUND -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.copy();
            default -> fgColor = TerminalColors.DEFAULT_FOREGROUND_COLOR.copy();
        }
        if (altBuffer) {
            terminal.altColors[index] = fgColor;
        } else {
            terminal.colors[index] = fgColor;
        }

        // Write background color — the shared SGR-background resolution; copied so the cell
        // keeps this value even if SGR later mutates the color fields in place.
        final TerminalColors.ColorData bgColor = terminal.currentBackgroundColor().copy();
        if (altBuffer) {
            terminal.altColorsBackground[index] = bgColor;
            terminal.altStyles[index] = terminal.style;
        } else {
            terminal.colorsBackground[index] = bgColor;
            terminal.styles[index] = terminal.style;
        }

        // Mark dirty — alt buffer uses y directly, main buffer needs scrollback offset
        // (= lastRowToDisplayMax + y - lastRowToDisplay; the heights cancel).
        final int dirtyLine = altBuffer
                ? y
                : terminal.height + terminal.lastRowToDisplayMax - (terminal.height - y) - terminal.lastRowToDisplay;
        markDirtyLine(terminal, dirtyLine);
    }

    // DEC Special Graphics maps only 0x60..0x7E (0x5F/'_' is handled separately as a blank;
    // 0x5E/'^' and 0x7F/DEL are not part of the VT100 SPECIAL_GRAPHICS charset — xterm
    // charsets.c leaves them untranslated (return ch as-is), and our guard ch < 0x60 ||
    // ch > 0x7E mirrors that. Source: xterm-411 fontutils.c dec2ucs (lines 4973-5007),
    // charsets.c: '_' -> ' ' mapping, cross-checked with Wikipedia "DEC Special Graphics".
    private static final int[] DEC_SPECIAL_GRAPHICS = {
        0x25C6, // ` -> ◆
        0x2592, // a -> ▒
        0x2409, // b -> ␉ (HT)
        0x240C, // c -> ␌ (FF)
        0x240D, // d -> ␍ (CR)
        0x240A, // e -> ␊ (LF)
        0x00B0, // f -> °
        0x00B1, // g -> ±
        0x2424, // h -> ␤ (NL)
        0x240B, // i -> ␋ (VT)
        0x2518, // j -> ┘
        0x2510, // k -> ┐
        0x250C, // l -> ┌
        0x2514, // m -> └
        0x253C, // n -> ┼
        0x23BA, // o -> ⎺ scan 1
        0x23BB, // p -> ⎻ scan 3
        0x2500, // q -> ─
        0x23BC, // r -> ⎼ scan 7
        0x23BD, // s -> ⎽ scan 9
        0x251C, // t -> ├
        0x2524, // u -> ┤
        0x2534, // v -> ┴
        0x252C, // w -> ┬
        0x2502, // x -> │
        0x2264, // y -> ≤
        0x2265, // z -> ≥
        0x03C0, // { -> π
        0x2260, // | -> ≠
        0x00A3, // } -> £
        0x00B7, // ~ -> ·
    };

    private int mapDecSpecialGraphics(final int ch) {
        final int mode = terminal.useG0 ? terminal.drawingModeG0 : terminal.drawingModeG1;
        if (mode != TerminalColors.DrawingMode.SPECIAL_GRAPHICS) {
            return ch;
        }
        if (ch == 0x5F) return ' '; // '_' → blank (xterm charsets.c)
        if (ch < 0x60 || ch > 0x7E) return ch;
        return DEC_SPECIAL_GRAPHICS[ch - 0x60];
    }

    /**
     * Sets the dirty bit for a single screen row, addressed by {@code dirtyLine} in
     * {@code [0, height-1]}. The dirty mask is a 64-bit {@code long}, but {@code dirtyLine} can
     * exceed that range while the view is scrolled back into scrollback (§36 M3): the write
     * lands off-screen, {@code 1L << dirtyLine} would silently wrap modulo 64 and flip an
     * unrelated bit, so a row we can't address in the mask instead forces a full redraw.
     */
    static void markDirtyLine(final Terminal terminal, final int dirtyLine) {
        if (dirtyLine >= 0 && dirtyLine < terminal.height) {
            terminal.markDirty(1L << dirtyLine);
        } else {
            terminal.markAllDirty();
        }
    }

    public static int getDirtyRow(final Terminal terminal, final int y) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            return y;
        }
        int globalY = terminal.lastRowToDisplayMax - (terminal.height - y);
        return terminal.height + globalY - terminal.lastRowToDisplay; // = lrdMax + y - lrd
    }
}
