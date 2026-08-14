package li.cil.oc2.common.vm.terminal.modes.impl;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * DECKPAM (ESC =) / DECKPNM (ESC >) — Application / Numeric keypad mode.
 * These are escape sequences (not CSI), but they flip a private mode (DECNKM, mode 66).
 * Combined into one file following the MouseMode pattern — two sides of the same coin.
 */
public class KeypadMode {
    public static void setApplication(final Terminal terminal) {
        terminal.currentPrivateModeState.DECNKM = true;
    }

    public static void setNumeric(final Terminal terminal) {
        terminal.currentPrivateModeState.DECNKM = false;
    }
}
