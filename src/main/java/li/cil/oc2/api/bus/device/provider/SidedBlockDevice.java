package li.cil.oc2.api.bus.device.provider;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional physical socket policy for a block exposed through a device-bus interface.
 * Invalidate block capabilities when a retained block entity's socket layout changes.
 */
public interface SidedBlockDevice {
    /** Whether a device-bus query may enter this block through the given world-space face. */
    boolean canConnectDevice(BlockState state, @Nullable Direction side);

    /** Blocks without a socket policy retain their normal access from every face. */
    static boolean permits(final BlockState state, @Nullable final Direction side) {
        return !(state.getBlock() instanceof final SidedBlockDevice sided)
                || sided.canConnectDevice(state, side);
    }
}
