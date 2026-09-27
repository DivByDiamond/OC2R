package li.cil.oc2.common.vm;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.controller.BusState;
import net.minecraft.network.chat.Component;

public final class VMErrorCalculator {

    public static Component getError(
            final BusState busState,
            final int busOverflow,
            final VMRunState runState,
            final Component bootError) {
        switch (busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_INCOMPLETE);
            case TOO_COMPLEX:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX);
            case MULTIPLE_CONTROLLERS:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS);
            case READY:
                if (runState == VMRunState.STOPPED || runState == VMRunState.LOADING_DEVICES) {
                    // A concrete boot failure outranks the generic overflow hint: the overflow is
                    // still communicated by the blinking overlay, while this text names what
                    // actually went wrong for the player.
                    if (bootError != null) {
                        return bootError;
                    }
                    if (busOverflow > 0) {
                        return Component.translatable(Constants.COMPUTER_BUS_OVERFLOW);
                    }
                }
                break;
            default:
                throw new AssertionError(busState);
        }
        return null;
    }
}
