package li.cil.oc2.common.vm.terminal.modes;

import li.cil.ceres.api.Serialized;

@Serialized
public class ModeState {
    public boolean KAM = false;
    public boolean IRM = false;
    public boolean SRM = false;
    public boolean LNM = false;

    public int getModeForRequest(int mode) {
        Boolean modeState = getMode(mode);
        if (modeState == null) return 0;
        if (modeState) return 1;
        return 2;
    }

    public Boolean getMode(int mode) {
        return switch (mode) {
            case 2 -> KAM;
            case 4 -> IRM;
            case 12 -> SRM;
            case 20 -> LNM;
            default -> null;
        };
    }
}
