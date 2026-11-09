package li.cil.oc2.client.gui.widget.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

final class TerminalKeyboardHandlerTest {
    private static String drain(final Terminal terminal) {
        final ByteBuffer input = terminal.io.getInput();
        assertNotNull(input);
        return StandardCharsets.UTF_8.decode(input).toString();
    }

    @Test
    void controlVPastesTheClipboard() {
        final Terminal terminal = new Terminal();
        final TerminalKeyboardHandler handler = new TerminalKeyboardHandler(terminal);

        assertTrue(handler.keyPressed(
                GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL, true, () -> "echo pasted"));

        assertEquals("echo pasted", drain(terminal));
    }

    @Test
    void bracketedPasteIsWrapped() {
        final Terminal terminal = new Terminal();
        terminal.currentPrivateModeState.SET_BRACKETED_PASTE = true;
        final TerminalKeyboardHandler handler = new TerminalKeyboardHandler(terminal);

        handler.keyPressed(GLFW.GLFW_KEY_V, 0, GLFW.GLFW_MOD_CONTROL, true, () -> "x");

        assertEquals("\033[200~x\033[201~", drain(terminal));
    }
}
