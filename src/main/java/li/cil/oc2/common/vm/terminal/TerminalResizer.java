package li.cil.oc2.common.vm.terminal;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

/**
 * Buffer (re)allocation for a {@link Terminal}'s width changes: {@link #setWidth} (DECCOLM's
 * destructive clear) and {@link #resizeWidth} (DECSCPP, content-preserving). Height changes are
 * {@link TerminalHeightResizer}, split out purely for file size. Extracted from {@link Terminal}
 * (А1); operates directly on the terminal's buffer/geometry fields, same pattern as
 * {@link li.cil.oc2.common.vm.terminal.buffer.TerminalBuffer} and
 * {@link li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter}.
 */
final class TerminalResizer {
    private final Terminal terminal;

    TerminalResizer(final Terminal terminal) {
        this.terminal = terminal;
    }

    void setWidth(final int newWidth) {
        // Guard against degenerate widths: width-1 feeds Math.clamp as a max everywhere,
        // so a zero/negative width would throw IAE on the next cursor movement. Oversized
        // widths are likewise refused here — the escape handlers police their own params,
        // but TerminalDiff.apply on the client calls this with whatever the snapshot said.
        if (newWidth != Math.clamp(newWidth, 1, Terminal.MAX_WIDTH)) {
            return;
        }
        terminal.geometryVersion.incrementAndGet();
        terminal.width = newWidth;

        // Erase color: DECCOLM clears with the current SGR background (VT510 erase
        // character), matching bufferManager.clear(). RIS resets the modes before
        // calling setWidth, so it still fills with defaults.
        final ColorData background = terminal.currentBackgroundColor();

        // Reallocate main buffer arrays
        final int mainSize = newWidth * terminal.height * Terminal.SCROLL_BACK_COUNT;
        terminal.buffer = new int[mainSize];
        terminal.colors = new ColorData[mainSize];
        terminal.colorsBackground = new ColorData[mainSize];
        terminal.styles = new byte[mainSize];
        Arrays.fill(terminal.buffer, ' ');
        Arrays.fill(terminal.colors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(terminal.colorsBackground, background.copy());
        Arrays.fill(terminal.styles, TerminalColors.DEFAULT_STYLE);

        // Reallocate alt buffer arrays
        final int altSize = newWidth * terminal.height;
        terminal.altBuffer = new int[altSize];
        terminal.altColors = new ColorData[altSize];
        terminal.altColorsBackground = new ColorData[altSize];
        terminal.altStyles = new byte[altSize];
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(terminal.altColorsBackground, background.copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);

        // Reset line attributes to single for destructive width change (DECCOLM/RIS)
        terminal.lineAttrs = new byte[terminal.height * Terminal.SCROLL_BACK_COUNT];
        terminal.altLineAttrs = new byte[terminal.height];
        Arrays.fill(terminal.lineAttrs, Terminal.LINE_ATTR_SINGLE);
        Arrays.fill(terminal.altLineAttrs, Terminal.LINE_ATTR_SINGLE);

        // Reset tab stops
        terminal.tabs = new boolean[newWidth];
        terminal.altTabs = new boolean[newWidth];
        for (int i = 1; i < newWidth; i++) {
            if (i % TerminalColors.TAB_WIDTH == 0) {
                terminal.tabs[i] = true;
                terminal.altTabs[i] = true;
            }
        }

        // DECCOLM spec: clear screen, reset margins, home cursor
        terminal.scrollFirst = 0;
        terminal.scrollLast = terminal.height - 1;
        terminal.scrollColFirst = 0;
        terminal.scrollColLast = newWidth - 1;
        terminal.lastRowToDisplay = terminal.height;
        terminal.lastRowToDisplayMax = terminal.height;
        terminal.setCursorPos(0, 0);

        // Mark all rows dirty
        terminal.geometryVersion.incrementAndGet();
        terminal.renderersLock.lock();
        try {
            terminal.renderers.forEach(model -> model.getDirtyMask().set(-1L));
        } finally {
            terminal.renderersLock.unlock();
        }
    }

    /**
     * Non-destructive width change (DECSCPP, {@code CSI Pn $ |}): reallocates the width-dependent
     * buffers at the new column count while COPYING existing contents into the surviving columns,
     * instead of clearing them as {@link Terminal#setWidth} does. Per DEC VT510-RM and xterm-410
     * {@code CASE_DECSCPP}: DECSCPP does not clear page memory, reset scrolling regions, reset
     * SGR, or reset tab stops — it only changes the column count, clamping the cursor if it now
     * sits beyond the new width. Columns beyond the new width are lost (132→80); new columns
     * (80→132) are default-initialized (blank, default fg/bg, no style) — a resize, not a clear,
     * so unlike {@link Terminal#setWidth} (DECCOLM's destructive clear, which fills with the
     * current SGR erase background) the new cells get defaults, matching xterm's {@code calloc}-
     * zero on {@code Reallocate}.
     *
     * <p>The caller (CH13) sets the DECCOLM flag to match — this method is flag-agnostic so it
     * can be reused by a future DECNCSM-gated non-destructive DECCOLM path.
     *
     * <p>Layout: the buffers are flat row-major with {@code width} as the stride (no circular
     * pointer — {@code lastRowToDisplay(Max)} are row-count windows, width-independent), so each
     * row is copied with a stride-aware {@link System#arraycopy}. The shared default object used
     * to fill new columns is safe because the write path ({@code TerminalBufferWriter.putChar})
     * REPLACES the {@code colors[idx]} reference rather than mutating it in place — the same
     * property {@link Terminal#setWidth} already relies on.
     */
    void resizeWidth(final int newWidth) {
        // Guard: degenerate widths would break Math.clamp; a no-op resize avoids a pointless
        // reallocation (DECSCPP to the current width does nothing). Oversized widths are
        // refused at this boundary too (see setWidth). The width field is only assigned at
        // commit time below — if an allocation fails, the terminal keeps a consistent old
        // width and old buffers instead of a bogus stride over live data.
        if (newWidth != Math.clamp(newWidth, 1, Terminal.MAX_WIDTH) || newWidth == terminal.width) {
            return;
        }
        terminal.geometryVersion.incrementAndGet();
        final int oldWidth = terminal.width;
        final int copyCols = Math.min(oldWidth, newWidth);

        // New columns are default-initialized (blank, default fg/bg, no style) — DECSCPP is a
        // resize, not a clear, so the new cells get defaults rather than the current SGR erase
        // background that setWidth (DECCOLM's destructive clear) uses. Matches xterm's calloc-
        // zero on Reallocate. Surviving columns keep their actual colors via the arraycopy below.
        final ColorData defaultBackground = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();

        // Main buffer (incl. scrollback): reallocate at the new stride, fill with defaults, then
        // copy the surviving columns of every row. mainRows is width-independent, so the row
        // count and the lastRowToDisplay(Max) window are preserved as-is.
        final int mainRows = terminal.height * Terminal.SCROLL_BACK_COUNT;
        final int[] newBuffer = new int[newWidth * mainRows];
        final ColorData[] newColors = new ColorData[newWidth * mainRows];
        final ColorData[] newColorsBackground = new ColorData[newWidth * mainRows];
        final byte[] newStyles = new byte[newWidth * mainRows];
        Arrays.fill(newBuffer, ' ');
        Arrays.fill(newColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newColorsBackground, defaultBackground);
        Arrays.fill(newStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < mainRows; r++) {
            final int src = r * oldWidth;
            final int dst = r * newWidth;
            System.arraycopy(terminal.buffer, src, newBuffer, dst, copyCols);
            System.arraycopy(terminal.colors, src, newColors, dst, copyCols);
            System.arraycopy(terminal.colorsBackground, src, newColorsBackground, dst, copyCols);
            System.arraycopy(terminal.styles, src, newStyles, dst, copyCols);
        }

        // Alt buffer (no scrollback): same per-row copy.
        final ColorData[] newAltColors = new ColorData[newWidth * terminal.height];
        final ColorData[] newAltColorsBackground = new ColorData[newWidth * terminal.height];
        final int[] newAltBuffer = new int[newWidth * terminal.height];
        final byte[] newAltStyles = new byte[newWidth * terminal.height];
        Arrays.fill(newAltBuffer, ' ');
        Arrays.fill(newAltColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newAltColorsBackground, defaultBackground);
        Arrays.fill(newAltStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < terminal.height; r++) {
            final int src = r * oldWidth;
            final int dst = r * newWidth;
            System.arraycopy(terminal.altBuffer, src, newAltBuffer, dst, copyCols);
            System.arraycopy(terminal.altColors, src, newAltColors, dst, copyCols);
            System.arraycopy(terminal.altColorsBackground, src, newAltColorsBackground, dst, copyCols);
            System.arraycopy(terminal.altStyles, src, newAltStyles, dst, copyCols);
        }

        // Tab stops: preserve existing stops in the surviving columns, default-fill new columns.
        final boolean[] newTabs = new boolean[newWidth];
        final boolean[] newAltTabs = new boolean[newWidth];
        for (int i = 1; i < newWidth; i++) {
            if (i < oldWidth) {
                newTabs[i] = terminal.tabs[i];
                newAltTabs[i] = terminal.altTabs[i];
            } else if (i % TerminalColors.TAB_WIDTH == 0) {
                newTabs[i] = true;
                newAltTabs[i] = true;
            }
        }

        // Commit: all allocations succeeded — swap the fields in one stretch. Any failure
        // above leaves the terminal fully consistent at the old width.
        terminal.buffer = newBuffer;
        terminal.colors = newColors;
        terminal.colorsBackground = newColorsBackground;
        terminal.styles = newStyles;
        terminal.altBuffer = newAltBuffer;
        terminal.altColors = newAltColors;
        terminal.altColorsBackground = newAltColorsBackground;
        terminal.altStyles = newAltStyles;
        terminal.tabs = newTabs;
        terminal.altTabs = newAltTabs;
        terminal.width = newWidth;

        // Scroll margins + scrollback window are row-based (width-independent) — preserved per
        // DECSCPP (does not reset DECSTBM). The active cursor is clamped only if it now sits
        // beyond the new width (xterm CursorSet on cur_col + 1 > value); setCursorPos clamps x
        // and clears the pending wrap + REP last-char, matching any cursor repositioning. The
        // saved cursor is left as-is — restore routes through the clamping setCursorPos (§36 Б6).
        if (terminal.x >= newWidth) {
            terminal.setCursorPos(newWidth - 1, terminal.y);
        }

        // Column margins (DECSLRM): non-destructive like the rest of DECSCPP (see Javadoc).
        adjustColumnMarginsForWidthChange(oldWidth, newWidth);

        // Arm the full refresh atomically with the geometry commit: a consume landing between
        // the field swap and here would otherwise ship a partial diff at the new width with no
        // rows, blanking clients that apply it destructively (same seam class as #38 F1).
        terminal.geometryVersion.incrementAndGet();
        terminal.networkState.markAllDirty();

        // Mark all rows dirty — BOTH sinks. The renderer mask drives local redraw; markAllDirty
        // drives the network diff (it also sets the renderer mask internally). The network mark
        // is load-bearing (Kimi gate, PR-2 review): DECSCPP is the first width path where the
        // server PRESERVES content while the client's TerminalDiff.apply responds to the width
        // change with a destructive setWidth — so the snapshot must re-ship the visible window
        // or the client blanks (screen + scrollback) while the server keeps everything,
        // diverging until the next captureFull. setWidth (DECCOLM) gets away without it only
        // because DECCOLM's escape paths (CH2/CH3) call markAllDirty themselves — the clear
        // ships symmetrically.
        terminal.markAllDirty();
    }

    /**
     * DECSLRM (§44) margin adjustment for a DECSCPP width change: an untouched right margin
     * (still at the OLD full width) tracks growth/shrink like the implicit default it is; an
     * explicit narrower margin is only clamped if it no longer fits.
     */
    private void adjustColumnMarginsForWidthChange(final int oldWidth, final int newWidth) {
        terminal.scrollColLast = terminal.scrollColLast == oldWidth - 1
                ? newWidth - 1
                : Math.min(terminal.scrollColLast, newWidth - 1);
        // Preserve the left < right invariant the DECSLRM handler enforces (CH6 rejects Pl >=
        // Pr): a plain Math.min(scrollColFirst, scrollColLast) can still produce left ==
        // right when shrinking (e.g. margins [10, 79] resized to width 5 -> scrollColLast
        // clamps to 4, so scrollColFirst would land on 4 too). Clamp one column short of
        // scrollColLast instead, floored at 0 for width-1 terminals.
        terminal.scrollColFirst =
                Math.min(terminal.scrollColFirst, Math.max(0, terminal.scrollColLast - 1));
    }
}
