package li.cil.oc2.common.vm.terminal.modes;

public class MouseMode {
    public int PrimaryMode;
    public int[] SecondaryModes;

    public MouseMode(int primaryMode, int[] secondaryModes) {
        PrimaryMode = primaryMode;
        SecondaryModes = secondaryModes;
    }

    public boolean isSecondaryModeEnabled(int mode) {
        for (int secondaryMode : SecondaryModes) {
            if (secondaryMode == mode) return true;
        }

        return false;
    }

    public boolean isMouseDisabled() {
        return PrimaryMode == 0;
    }
}
