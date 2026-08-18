package li.cil.oc2.client.gui.widget.terminal;

import java.util.function.Supplier;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.vm.terminal.Terminal;
import org.lwjgl.glfw.GLFW;

final class TerminalKeyboardHandler {
    private final Terminal terminal;

    TerminalKeyboardHandler(final Terminal terminal) {
        this.terminal = terminal;
    }

    public boolean charTyped(final char ch, final int modifier) {
        if (modifier == 0 || modifier == GLFW.GLFW_MOD_SHIFT) {
            terminal.io.putInput(String.valueOf(ch));
        }
        return true;
    }

    @SuppressWarnings("unused")
    public boolean keyPressed(
            final int keyCode,
            final int scanCode,
            final int modifiers,
            final boolean shouldCapture,
            final Supplier<String> clipboardSupplier) {
        if (!shouldCapture && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && terminal.currentPrivateModeState.APPLICATION_ESC_MODE) {
            terminal.io.putInput("\033[0[");
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = clipboardSupplier.get();
            boolean bracketed = terminal.currentPrivateModeState.SET_BRACKETED_PASTE;
            if (bracketed) terminal.io.putInput("\033[200~");
            terminal.io.putInput(value);
            if (bracketed) terminal.io.putInput("\033[201~");
        } else {
            byte[] sequence;
            if (terminal.currentPrivateModeState.DECCKM
                    && (keyCode == GLFW.GLFW_KEY_UP
                            || keyCode == GLFW.GLFW_KEY_DOWN
                            || keyCode == GLFW.GLFW_KEY_LEFT
                            || keyCode == GLFW.GLFW_KEY_RIGHT))
                sequence = TerminalInput.getDECCKMSequence(keyCode, modifiers);
            else sequence = TerminalInput.getSequence(keyCode, modifiers);
            if (sequence != null) {
                for (final byte b : sequence) {
                    terminal.io.putInput(b);
                }
            }
        }
        return true;
    }
}