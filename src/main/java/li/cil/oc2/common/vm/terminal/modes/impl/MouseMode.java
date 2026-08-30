package li.cil.oc2.common.vm.terminal.modes.impl;

public class MouseMode {
    public int primaryMode;
    public int[] secondaryModes;

    public MouseMode(int primaryMode, int... secondaryModes) {
        this.primaryMode = primaryMode;
        this.secondaryModes = secondaryModes.clone();
    }

    public boolean isSecondaryModeEnabled(int mode) {
        for (int secondaryMode : secondaryModes) {
            if (secondaryMode == mode) return true;
        }

        return false;
    }

    public boolean isMouseDisabled() {
        return primaryMode == 0;
    }
}
