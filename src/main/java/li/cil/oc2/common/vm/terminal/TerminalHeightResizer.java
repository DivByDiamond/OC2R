package li.cil.oc2.common.vm.terminal;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;

/**
 * DECSLPP/DECSNLS/XTWINOPS-case-8 height reallocation for a {@link Terminal}. Split out of
 * {@link TerminalResizer} (А1) purely for file size — the two are otherwise the same kind of
 * object (operates directly on the terminal's buffer/geometry fields) and share no state.
 */
final class TerminalHeightResizer {
    private final Terminal terminal;

    TerminalHeightResizer(final Terminal terminal) {
        this.terminal = terminal;
    }

    /**
     * Non-destructive height change (DECSLPP/DECSNLS, {@code CSI Pn * |}; also XTWINOPS case-8,
     * {@code CSI 8;rows;cols t}): reallocates the height-dependent buffers at the new row count
     * while preserving contents, instead of clearing them. New rows are default-initialized
     * (blank, default fg/bg, no style) — a resize, not a clear.
     *
     * <p>Content anchoring follows xterm-410 {@code Reallocate} with the default SouthWest
     * resizeGravity (screen.c:447-570), NOT a prefix copy: newest content lives at high buffer
     * rows (the buffer shifts up at capacity), so anchoring at row 0 would display ancient
     * scrollback as the screen and destroy the live window.
     * <ul>
     * <li>Shrink: the excess drops off the BOTTOM of the screen first (the rows below the
     * cursor, xterm's {@code max_row - cur_row}); only the remainder scrolls off the top of
     * the buffer. If the surviving span still exceeds the new capacity, the oldest rows are
     * trimmed until it fits. The visible window keeps the cursor's region — e.g. with the
     * cursor on the bottom row, a 48→12 shrink shows the last 12 screen rows, cursor included.
     * <li>Grow: up to {@code delta} scrollback rows are pulled back onto the top of the screen
     * (xterm's {@code move_down}), keeping content glued to the bottom; any delta beyond the
     * available history yields new blank rows at the bottom. This is a pure window shift —
     * buffer rows do not move.
     * </ul>
     *
     * <p>Scroll margins and origin mode are RESET to full-page, also per xterm
     * ({@code ScreenResize} calls {@code resetMargins} and clears ORIGIN unconditionally,
     * screen.c:2427). DEC VT510-RM says DECSLPP preserves DECSTBM — we deliberately follow
     * xterm instead (verified against a physical VT420: the hardware keeps the margins and
     * silently stops using the rest of the page, because this engine's scroll-window machinery
     * gates on full-page margins). Convention: VT first, but when DEC did the big dumb, xterm
     * wins.
     *
     * <p>The caller (CH13) sets any mode flags — this method is flag-agnostic so it can be
     * reused by XTWINOPS case-8 which sets both dimensions at once. Out-of-range heights are
     * refused at this boundary (the handlers police their own params; TerminalDiff.apply on
     * the client does not), and all fields are assigned only after every allocation succeeds
     * (commit-after-alloc), so a failure leaves the terminal fully consistent.
     */
    void resizeHeight(final int newHeight) { // NOPMD: NPath — relayout math mirrors resizeWidth's shape
        if (newHeight != Math.clamp(newHeight, 1, Terminal.MAX_HEIGHT) || newHeight == terminal.height) {
            return;
        }
        terminal.geometryVersion.incrementAndGet();
        final int oldHeight = terminal.height;
        final int oldMainRows = oldHeight * Terminal.SCROLL_BACK_COUNT;
        final int newMainRows = newHeight * Terminal.SCROLL_BACK_COUNT;

        // Relayout plan, computed in OLD buffer coordinates before allocating: source span
        // [srcStart, srcLen) of the old main buffer lands at new rows [0, srcLen).
        final int srcStart;
        final int srcLen;
        final int altSrcStart;
        final int newLrd;
        final int newLrdMax;
        final int newY;
        final int delta = newHeight - oldHeight;
        if (delta > 0) { // grow: keep every row; pull history down into the new top rows
            srcStart = 0;
            srcLen = oldMainRows;
            altSrcStart = 0;
            // The pull comes from the WRITE window's scrollback (lrdMax-based), not the
            // transient view. Both window fields gain +delta (the window is delta taller)
            // and lose -take (the top reclaim): a bottom-anchored view stays glued to the
            // content; a scrolled-back view keeps its rows. newLrdMax >= newHeight holds
            // because take <= lrdMax - oldHeight; the lrd >= height invariant every
            // renderer's (row + lrd - height) indexing relies on is enforced by the floor.
            final int take = Math.min(delta, Math.max(0, terminal.lastRowToDisplayMax - oldHeight));
            newLrdMax = terminal.lastRowToDisplayMax - take + delta;
            newLrd = terminal.lastRowToDisplay == terminal.lastRowToDisplayMax
                    ? newLrdMax
                    : Math.clamp(terminal.lastRowToDisplay, newHeight, newLrdMax);
            // The cursor rides the pull on the main buffer; the alt buffer has no scrollback
            // and its copy is top-anchored, so an alt-active grow leaves the cursor alone.
            newY = terminal.currentPrivateModeState.isAltBufferEnabled() ? terminal.y : terminal.y + take;
        } else { // shrink: drop below-cursor rows first, then off the buffer top (xterm move_up)
            final int excess = -delta;
            final int rowsBelowCursor = oldHeight - 1 - terminal.y;
            final int fromTop = Math.max(0, excess - rowsBelowCursor);
            final int fromBottom = excess - fromTop;
            int start = fromTop;
            int len = terminal.lastRowToDisplayMax - fromBottom - start;
            if (len > newMainRows) { // surviving span overflows the new capacity: trim oldest
                start += len - newMainRows;
                len = newMainRows;
            }
            srcStart = start;
            srcLen = len;
            // The alt buffer has no scrollback, but the same gravity applies to it (xterm's
            // Reallocate treats every ScrnBuf alike): the copy must anchor at the same
            // fromTop the cursor math uses, or the cursor would ride up while its content
            // stays top-anchored — parked on an unrelated row.
            altSrcStart = fromTop;
            newLrdMax = len;
            // lrd - srcStart may go negative when the capacity trim ate rows the view was
            // parked on (deep scrollback + aggressive shrink) — the floor at newHeight then
            // parks the view on the full new screen, which is the only sane answer.
            newLrd = Math.clamp(terminal.lastRowToDisplay - srcStart, newHeight, len);
            newY = Math.clamp(terminal.y - fromTop, 0, newHeight - 1);
        }

        final ColorData defaultBackground = TerminalColors.DEFAULT_BACKGROUND_COLOR.copy();

        // Main buffer (incl. scrollback): reallocate at the new height, fill with defaults,
        // then copy the planned span. Each row is width cells, stride unchanged.
        final int width = terminal.width;
        final int[] newBuffer = new int[width * newMainRows];
        final ColorData[] newColors = new ColorData[width * newMainRows];
        final ColorData[] newColorsBackground = new ColorData[width * newMainRows];
        final byte[] newStyles = new byte[width * newMainRows];
        Arrays.fill(newBuffer, ' ');
        Arrays.fill(newColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newColorsBackground, defaultBackground);
        Arrays.fill(newStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < srcLen; r++) {
            final int src = (srcStart + r) * width;
            final int dst = r * width;
            System.arraycopy(terminal.buffer, src, newBuffer, dst, width);
            System.arraycopy(terminal.colors, src, newColors, dst, width);
            System.arraycopy(terminal.colorsBackground, src, newColorsBackground, dst, width);
            System.arraycopy(terminal.styles, src, newStyles, dst, width);
        }

        // Alt buffer (no scrollback): top-anchored on grow; on shrink anchored at the same
        // fromTop as the cursor relayout (xterm Reallocate gravity, see the plan comment).
        final int copyAltRows = Math.min(oldHeight, newHeight);
        final ColorData[] newAltColors = new ColorData[width * newHeight];
        final ColorData[] newAltColorsBackground = new ColorData[width * newHeight];
        final int[] newAltBuffer = new int[width * newHeight];
        final byte[] newAltStyles = new byte[width * newHeight];
        Arrays.fill(newAltBuffer, ' ');
        Arrays.fill(newAltColors, TerminalColors.DEFAULT_FOREGROUND_COLOR.copy());
        Arrays.fill(newAltColorsBackground, defaultBackground);
        Arrays.fill(newAltStyles, TerminalColors.DEFAULT_STYLE);
        for (int r = 0; r < copyAltRows; r++) {
            final int src = (altSrcStart + r) * width;
            final int dst = r * width;
            System.arraycopy(terminal.altBuffer, src, newAltBuffer, dst, width);
            System.arraycopy(terminal.altColors, src, newAltColors, dst, width);
            System.arraycopy(terminal.altColorsBackground, src, newAltColorsBackground, dst, width);
            System.arraycopy(terminal.altStyles, src, newAltStyles, dst, width);
        }

        // Line attributes (per-row, ESC #3/#4/#5/#6) — same relayout as rows, but one byte per row.
        final byte[] newLineAttrs = new byte[newMainRows];
        final byte[] newAltLineAttrs = new byte[newHeight];
        Arrays.fill(newLineAttrs, Terminal.LINE_ATTR_SINGLE);
        Arrays.fill(newAltLineAttrs, Terminal.LINE_ATTR_SINGLE);
        for (int r = 0; r < srcLen; r++) {
            if (terminal.lineAttrs != null && srcStart + r < terminal.lineAttrs.length) {
                newLineAttrs[r] = terminal.lineAttrs[srcStart + r];
            }
        }
        for (int r = 0; r < copyAltRows; r++) {
            if (terminal.altLineAttrs != null && altSrcStart + r < terminal.altLineAttrs.length) {
                newAltLineAttrs[r] = terminal.altLineAttrs[altSrcStart + r];
            }
        }

        // Commit: all allocations succeeded — swap every field in one stretch. Any failure
        // above leaves the terminal fully consistent at the old height.
        terminal.buffer = newBuffer;
        terminal.colors = newColors;
        terminal.colorsBackground = newColorsBackground;
        terminal.styles = newStyles;
        terminal.altBuffer = newAltBuffer;
        terminal.altColors = newAltColors;
        terminal.altColorsBackground = newAltColorsBackground;
        terminal.altStyles = newAltStyles;
        terminal.lineAttrs = newLineAttrs;
        terminal.altLineAttrs = newAltLineAttrs;
        terminal.height = newHeight;
        terminal.lastRowToDisplay = newLrd;
        terminal.lastRowToDisplayMax = newLrdMax;

        // Margins + origin reset per xterm ScreenResize (see Javadoc) — left/right margins too,
        // xterm resetMargins() clears both axes unconditionally.
        terminal.scrollFirst = 0;
        terminal.scrollLast = newHeight - 1;
        terminal.scrollColFirst = 0;
        terminal.scrollColLast = width - 1;
        terminal.currentPrivateModeState.DECOM = false;

        // Move the cursor with its content (grow pull-down / shrink top-drop), clamped into
        // the new page; routes through setCursorPos for the pending-wrap/REP clearing that
        // matches any repositioning. Unmoved cursor (plain grow) is left alone.
        if (newY != terminal.y) {
            terminal.setCursorPos(terminal.x, newY);
        }

        // Reallocate the network dirty sink for the new capacity and arm the full refresh
        terminal.geometryVersion.incrementAndGet();
        terminal.networkState.reallocate(newHeight);

        // Mark all rows dirty — BOTH sinks (same rationale as resizeWidth).
        terminal.markAllDirty();
    }
}
