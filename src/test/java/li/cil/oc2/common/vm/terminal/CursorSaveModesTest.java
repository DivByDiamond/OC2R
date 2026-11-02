package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CursorSaveModesTest {
    private final Terminal terminal = new Terminal();

    private void write(final String value) {
        terminal.io.putOutput(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?47h"})
    void mode1048RestoresRenditionCharsetAndPositionInActiveBuffer(final String buffer) {
        write(buffer + "\033[4;9H\033[1;4;38;2;12;34;56;48;5;123m\033(0\033[?1048h");
        write("\033[0m\033(B\033[1;1H\033[?1048l");
        assertEquals(8, terminal.x);
        assertEquals(3, terminal.y);
        assertEquals(Terminal.STYLE_BOLD_MASK | Terminal.STYLE_UNDERLINE_MASK, terminal.style);
        assertEquals(li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode.TRUE_COLOR,
                terminal.currentForegroundColorMode);
        assertEquals(li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR,
                terminal.currentBackgroundColorMode);
        assertEquals(0x0C2238, terminal.foregroundColor.toInt());
        assertEquals(123, terminal.currentBackgroundColor().g);
        write("q");
        final int[] cells = terminal.currentPrivateModeState.isAltBufferEnabled() ? terminal.altBuffer : terminal.buffer;
        final int row = terminal.currentPrivateModeState.isAltBufferEnabled() ? 3 : terminal.lastRowToDisplayMax - terminal.height + 3;
        assertEquals(0x2500, cells[row * terminal.width + 8]);
    }

    @Test
    void savedModesAndPendingWrapSurvive1048() {
        write("\033[3;10r\033[?6h\033[?7l\033[2;4H\033[?1048h");
        write("\033[?6l\033[?7h\033[?1048l");
        assertTrue(terminal.currentPrivateModeState.DECOM);
        assertFalse(terminal.currentPrivateModeState.DECAWM);
        assertEquals(3, terminal.y);
        write("\033[?6l\033[?7h\033[1;80HX\033[?1048h\033[H\033[?1048l");
        assertTrue(terminal.autowrapPending);
        write("Y");
        assertEquals(1, terminal.y);
        assertEquals(1, terminal.x);
    }

    @Test
    void mainAndAltSaveSlotsAreIndependentForAnsiAndDecAliases() {
        write("\033[4;9H\033[31m\033[?1048h\033[?47h");
        write("\033[7;12H\033[32m\033[s\033[H\033[0m\033[u");
        assertEquals(11, terminal.x);
        assertEquals(6, terminal.y);
        assertEquals(2, terminal.sixteenColor.r);
        write("\033[?47l\033[?1048l");
        assertEquals(8, terminal.x);
        assertEquals(3, terminal.y);
        assertEquals(1, terminal.sixteenColor.r);
    }

    @Test
    void mode1049ReturnsToMainSavedStateAfterAlt1048() {
        write("\033[4;9H\033[31m\033[?1049h\033[7;12H\033[32m\033[?1048h");
        write("\033[H\033[0m\033[?1048l");
        assertEquals(11, terminal.x);
        write("\033[?1049l");
        assertEquals(8, terminal.x);
        assertEquals(3, terminal.y);
        assertEquals(1, terminal.sixteenColor.r);
    }
}
