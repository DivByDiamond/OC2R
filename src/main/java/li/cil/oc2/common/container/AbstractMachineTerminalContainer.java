package li.cil.oc2.common.container;

import java.nio.ByteBuffer;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.world.inventory.MenuType;

public abstract class AbstractMachineTerminalContainer extends AbstractMachineContainer {
    protected AbstractMachineTerminalContainer(
            final MenuType<?> type, final int id, final IntPrecisionContainerData energyInfo) {
        super(type, id, energyInfo);
    }

    public abstract void switchToTerminal();

    public abstract Terminal getTerminal();

    public abstract boolean getCaptureInputState();

    public abstract void setCaptureInputState(boolean state);

    public void toggleCaptureInputState() {
        setCaptureInputState(!getCaptureInputState());
    }

    public abstract void sendTerminalInputToServer(final ByteBuffer input);
}