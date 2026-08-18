package li.cil.oc2.common.vm.terminal.modes.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImplementedPrivateModes {
    private static final Logger LOGGER = LogManager.getLogger();

    public static Map<Integer, Boolean> modeStatus = new ConcurrentHashMap<>();

    public static ImplementedPrivateModes instance = new ImplementedPrivateModes();

    public ImplementedPrivateModes() {
        for (final ModeTable mode : ModeTable.values()) {
            if (mode.getKind() == ModeTable.Kind.PRIVATE) {
                modeStatus.put(mode.getNumber(), mode.isImplemented());
            }
        }
    }

    public void modeUsed(int mode, boolean state) {
        if (Boolean.FALSE.equals(modeStatus.get(mode))) {
            LOGGER.warn("Unimplemented Mode: {} was {}.", mode, state ? "set" : "reset");
        }
    }
}
