package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.render.RendererModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SGR (Select Graphic Rendition) integration tests, plus DECSC/DECRC color+style save/restore.
 *
 * <p>Feeds real escape sequences through the {@link Terminal} parser (the same path the VM firmware
 * uses) via {@code putOutput}, then asserts on terminal state. Covers the color-parser refactor
 * ({@code SGRColorParser} / {@code SGRStyleDispatch}), the malformed-extended-color skip, the
 * 256/RGB bounds clamping, and the DECSC/DECRC saved-color lifecycle. Isolated parser unit tests
 * live in {@code SGRColorParserTest}; the cases here exercise the full dispatch path.
 *
 * <p>Split out of {@code TerminalBufferTest} so the SGR feature has a dedicated home.
 */
public class SGRTest {
    private Terminal terminal;
    private DummyRenderer renderer;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
        renderer = new DummyRenderer();
        terminal.renderers.add(renderer);
    }

    @Test
    void sgr256ColorForeground() {
        write(terminal, "\u001b[38;5;196mX");
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.currentForegroundColorMode);
        assertEquals(196, terminal.twoFiftySixColor.R);
        assertEquals('X', charAt(0, 0));
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.colors[0].Mode);
        assertEquals(196, terminal.colors[0].R);
    }

    @Test
    void sgrTrueColorForegroundAndBackground() {
        write(terminal, "\u001b[38;2;100;150;200;48;2;10;20;30mY");
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentForegroundColorMode);
        assertEquals(100, terminal.foregroundColor.R);
        assertEquals(150, terminal.foregroundColor.G);
        assertEquals(200, terminal.foregroundColor.B);
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentBackgroundColorMode);
        assertEquals(10, terminal.backgroundColor.R);
        assertEquals(20, terminal.backgroundColor.G);
        assertEquals(30, terminal.backgroundColor.B);
        assertEquals('Y', charAt(0, 0));
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.colors[0].Mode);
        assertEquals(100, terminal.colors[0].R);
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.colorsBackground[0].Mode);
        assertEquals(10, terminal.colorsBackground[0].R);
    }

    @Test
    void sgrTrueColorKeepsFollowingAttributes() {
        write(terminal, "\u001b[38;2;100;150;200;1mZ");
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentForegroundColorMode);
        assertEquals(100, terminal.foregroundColor.R);
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.style & Terminal.STYLE_BOLD_MASK);
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.styles[0] & Terminal.STYLE_BOLD_MASK);
    }

    @Test
    void sgrResetRestoresDefaults() {
        write(terminal, "\u001b[38;2;100;150;200;48;5;52;1mX");
        write(terminal, "\u001b[0mY");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        assertEquals(TerminalColors.ColorMode.DEFAULT_BACKGROUND, terminal.currentBackgroundColorMode);
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.style);
    }

    // --- SGR extended color: malformed sequences skip the mode byte ---
    // A malformed 38/48 (mode byte 5/2 with missing sub-args, or an unrecognized mode byte) must
    // skip BOTH the selector and the mode byte, so the mode byte is not re-read as a top-level
    // SGR code (5 → blink, 2 → dim, 7 → invert). This matches the pre-refactor base behavior,
    // which the monolithic SGR did with an explicit "still skip v2 so it isn't re-read" comment.

    @Test
    void sgrMalformedExtendedColorSkipsModeByte() {
        // 38;5 with no color index: must NOT fall through to SGR 5 (blink).
        write(terminal, "\u001b[38;5m");
        assertEquals(0, terminal.style & Terminal.STYLE_BLINK_MASK, "malformed 38;5 must not enable blink");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode,
            "malformed 38;5 must not change the foreground color mode");

        // 38;2 with no RGB triple: must NOT fall through to SGR 2 (dim).
        write(terminal, "\u001b[38;2m");
        assertEquals(0, terminal.style & Terminal.STYLE_DIM_MASK, "malformed 38;2 must not enable dim");

        // 38;7 — 7 is not a color mode, so the whole 38;7 pair is skipped and 7 is NOT applied as
        // SGR 7 (invert).
        write(terminal, "\u001b[38;7m");
        assertEquals(0, terminal.style & Terminal.STYLE_INVERT_MASK,
            "38;7 must skip 7 as the mode byte, not apply it as invert");
    }

    @Test
    void sgrMalformedExtendedColorBareSelectorDoesNotCrash() {
        // 38 as the very last arg (no mode byte at all): skip just the selector, no crash.
        write(terminal, "\u001b[38m");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.style);
    }

    @Test
    void sgrMalformedExtendedColorDoesNotSwallowFollowingAttribute() {
        // 38;7 is malformed (7 is not a color mode) → skip both 38 and 7; the trailing 1 (bold) is
        // a separate top-level SGR and must still apply.
        write(terminal, "\u001b[38;7;1m");
        assertEquals(0, terminal.style & Terminal.STYLE_INVERT_MASK,
            "7 must be skipped as the mode byte, not applied as invert");
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.style & Terminal.STYLE_BOLD_MASK,
            "trailing 1 must still apply bold");
    }

    // --- SGR extended color: out-of-range components are clamped, not wrapped/OOB ---

    @Test
    void sgr256ColorIndexClampedToBounds() {
        // Out-of-range 256-color index must clamp to 255, not throw on palette access.
        write(terminal, "\u001b[38;5;300m");
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.currentForegroundColorMode);
        assertEquals(255, terminal.twoFiftySixColor.R);
    }

    @Test
    void sgrTrueColorComponentsClampedToBounds() {
        // Out-of-range true-color components must clamp to 255, not wrap via & 0xFF.
        write(terminal, "\u001b[38;2;300;0;0m");
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentForegroundColorMode);
        assertEquals(255, terminal.foregroundColor.R);
        assertEquals(0, terminal.foregroundColor.G);
        assertEquals(0, terminal.foregroundColor.B);
    }

    // --- DECSC (ESC 7) / DECRC (ESC 8) save and restore SGR color + style state ---

    @Test
    void decscDecrcRestoresForegroundColor() {
        write(terminal, "\u001b[38;5;196m");   // 256-color fg, index 196
        write(terminal, "\u001b7");             // DECSC: save
        write(terminal, "\u001b[38;5;21m");     // change fg to index 21
        assertEquals(21, terminal.twoFiftySixColor.R);
        write(terminal, "\u001b8");             // DECRC: restore
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.currentForegroundColorMode);
        assertEquals(196, terminal.twoFiftySixColor.R, "DECRC must restore the saved foreground color");
    }

    @Test
    void decscDecrcRestoresStyle() {
        write(terminal, "\u001b[1m");           // bold
        write(terminal, "\u001b7");             // DECSC
        write(terminal, "\u001b[22m");          // bold off
        assertEquals(0, terminal.style & Terminal.STYLE_BOLD_MASK);
        write(terminal, "\u001b8");             // DECRC
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.style & Terminal.STYLE_BOLD_MASK,
            "DECRC must restore the saved style");
    }

    @Test
    void decscDecrcUsesDefensiveCopy() {
        // Restoring a saved color must copy, so later mutating current state cannot corrupt saved.
        write(terminal, "\u001b[38;5;100m");
        write(terminal, "\u001b7");             // save (savedTwoFiftySixColor.R = 100)
        write(terminal, "\u001b[38;5;200m");    // current = 200
        write(terminal, "\u001b8");             // restore → current = 100
        assertEquals(100, terminal.twoFiftySixColor.R);
        assertEquals(100, terminal.savedTwoFiftySixColor.R);
        write(terminal, "\u001b[38;5;50m");     // mutate current again
        assertEquals(50, terminal.twoFiftySixColor.R);
        assertEquals(100, terminal.savedTwoFiftySixColor.R,
            "saved color must not alias current color after restore");
    }

    @Test
    void decrcAfterRisRestoresDefaultsNotStaleColor() {
        // RIS resets saved state, so a DECRC after RIS restores the branch defaults (a no-op for
        // current state, since RIS already set it there), not the pre-RIS color. The saved
        // foreground mode default must match the operative current-state default (SIXTEEN_COLOR),
        // not DEFAULT_FOREGROUND — whose rendering semantics arrive with the screen-features work.
        write(terminal, "\u001b[38;5;196m");
        write(terminal, "\u001b7");             // save 196
        write(terminal, "\u001bc");             // RIS: reset current + saved state to defaults
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        write(terminal, "\u001b8");             // DECRC: restore saved (now defaults) — no-op
        assertNotEquals(196, terminal.twoFiftySixColor.R,
            "RIS must have reset saved state so DECRC does not restore the stale color");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode,
            "DECRC after RIS is a no-op: current stays at the SIXTEEN_COLOR default");
    }

    @Test
    void decscDecrcRestoresColorInAltBuffer() {
        // The alt-buffer path uses the separate altSaved* fields; exercise it explicitly.
        write(terminal, "\u001b[?1049h");       // enter alt buffer
        write(terminal, "\u001b[38;5;160m");    // alt fg 160
        write(terminal, "\u001b7");             // DECSC (alt path)
        write(terminal, "\u001b[38;5;200m");    // change
        write(terminal, "\u001b8");             // DECRC (alt path)
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.currentForegroundColorMode);
        assertEquals(160, terminal.twoFiftySixColor.R,
            "DECRC in alt buffer must restore the saved color via altSaved*");
    }

    private void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private char charAt(final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return (char) terminal.buffer[x + row * Terminal.WIDTH];
    }

    private static final class DummyRenderer implements RendererModel {
        private final AtomicInteger dirtyMask = new AtomicInteger();

        @Override
        public AtomicInteger getDirtyMask() {
            return dirtyMask;
        }

        @Override
        public void close() {
            dirtyMask.set(0);
        }
    }
}
