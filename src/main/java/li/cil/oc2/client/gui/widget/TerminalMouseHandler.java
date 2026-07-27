package li.cil.oc2.client.gui.widget;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.MouseMode;
import li.cil.oc2.common.vm.terminal.modes.PrivateMode;
import org.joml.Vector2i;

final class TerminalMouseHandler {
    private final Terminal terminal;

    TerminalMouseHandler(final Terminal terminal) {
        this.terminal = terminal;
    }

    public boolean mouseClicked(
            final double x,
            final double y,
            final int button,
            final boolean overTerminal,
            final boolean shouldCapture,
            final int leftPos,
            final int topPos) {
        final MouseMode currentMouseMode = terminal.currentPrivateModeState.getMouseMode();
        if (currentMouseMode.isMouseDisabled()) return false;
        final Vector2i position = getMousePosition(x, y, leftPos, topPos);
        if (overTerminal && shouldCapture) {
            if (currentMouseMode.PrimaryMode == PrivateMode.X11MM
                    || currentMouseMode.PrimaryMode == PrivateMode.CELL_MOTION_MOUSE) {
                if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.SGR_MOUSE)) {
                    terminal.putInput(
                            "\033[<" + button + ";" + position.x + ";" + position.y + "M");
                    return true;
                } else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.UTF8_MOUSE)) {
                    byte[] csiMBytes = "\033[M".getBytes(StandardCharsets.UTF_8);
                    byte[] buttonBytes = utf8(button + 32);
                    byte[] colBytes = utf8(position.x + 32);
                    byte[] rowBytes = utf8(position.y + 32);
                    byte[] finalBytes =
                            new byte
                                    [csiMBytes.length
                                            + buttonBytes.length
                                            + colBytes.length
                                            + rowBytes.length];
                    System.arraycopy(csiMBytes, 0, finalBytes, 0, csiMBytes.length);
                    System.arraycopy(
                            buttonBytes, 0, finalBytes, csiMBytes.length, buttonBytes.length);
                    System.arraycopy(
                            colBytes,
                            0,
                            finalBytes,
                            csiMBytes.length + buttonBytes.length,
                            colBytes.length);
                    System.arraycopy(
                            rowBytes,
                            0,
                            finalBytes,
                            csiMBytes.length + buttonBytes.length + colBytes.length,
                            rowBytes.length);
                    terminal.putInput(ByteBuffer.wrap(finalBytes));
                    return true;
                } else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.URXVT_MOUSE)) {
                    terminal.putInput(
                            "\033["
                                    + (button + 32)
                                    + ";"
                                    + position.x
                                    + ";"
                                    + position.y
                                    + "M");
                } else {
                    terminal.putInput('\033');
                    terminal.putInput('[');
                    terminal.putInput('M');
                    terminal.putInput((byte) (button + 32));
                    terminal.putInput((byte) (position.x + 32));
                    terminal.putInput((byte) (position.y + 32));
                    return true;
                }
            } else {
                System.out.println("ERR: Unsupported primary mode");
            }
        }
        return false;
    }

    public boolean mouseReleased(
            final double x,
            final double y,
            final int button,
            final boolean overTerminal,
            final boolean shouldCapture,
            final int leftPos,
            final int topPos) {
        final MouseMode currentMouseMode = terminal.currentPrivateModeState.getMouseMode();
        if (currentMouseMode.isMouseDisabled()) return false;
        final Vector2i position = getMousePosition(x, y, leftPos, topPos);
        if (overTerminal && shouldCapture) {
            if (currentMouseMode.PrimaryMode == PrivateMode.X11MM
                    || currentMouseMode.PrimaryMode == PrivateMode.CELL_MOTION_MOUSE) {
                if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.SGR_MOUSE)) {
                    terminal.putInput(
                            "\033[<" + button + ";" + position.x + ";" + position.y + "m");
                    return true;
                } else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.UTF8_MOUSE)) {
                    byte[] csiMBytes = "\033[M".getBytes(StandardCharsets.UTF_8);
                    byte[] buttonBytes = utf8(35);
                    byte[] colBytes = utf8(position.x + 32);
                    byte[] rowBytes = utf8(position.y + 32);
                    byte[] finalBytes =
                            new byte
                                    [csiMBytes.length
                                            + buttonBytes.length
                                            + colBytes.length
                                            + rowBytes.length];
                    System.arraycopy(csiMBytes, 0, finalBytes, 0, csiMBytes.length);
                    System.arraycopy(
                            buttonBytes, 0, finalBytes, csiMBytes.length, buttonBytes.length);
                    System.arraycopy(
                            colBytes,
                            0,
                            finalBytes,
                            csiMBytes.length + buttonBytes.length,
                            colBytes.length);
                    System.arraycopy(
                            rowBytes,
                            0,
                            finalBytes,
                            csiMBytes.length + buttonBytes.length + colBytes.length,
                            rowBytes.length);
                    terminal.putInput(ByteBuffer.wrap(finalBytes));
                    return true;
                } else if (currentMouseMode.isSecondaryModeEnabled(PrivateMode.URXVT_MOUSE)) {
                    terminal.putInput("\033[" + 35 + ";" + position.x + ";" + position.y + "M");
                } else {
                    terminal.putInput('\033');
                    terminal.putInput('[');
                    terminal.putInput('M');
                    terminal.putInput((byte) 35);
                    terminal.putInput((byte) (position.x + 32));
                    terminal.putInput((byte) (position.y + 32));
                    return true;
                }
            } else {
                System.out.println("ERR: Unsupported primary mode");
            }
        }
        return false;
    }

    private Vector2i getMousePosition(
            final double x, final double y, final int leftPos, final int topPos) {
        int tx = MachineTerminalWidget.TERMINAL_WIDTH / Terminal.WIDTH;
        int ty = MachineTerminalWidget.TERMINAL_HEIGHT / Terminal.HEIGHT;
        int sx = (int) (((x - leftPos) - MachineTerminalWidget.TERMINAL_X) / tx) + 1;
        int sy = (int) (((y - topPos) - MachineTerminalWidget.TERMINAL_Y) / ty) + 1;
        return new Vector2i(sx, sy);
    }

    private static byte[] utf8(int value) {
        return new String(new int[] {value}, 0, 1).getBytes(StandardCharsets.UTF_8);
    }
}