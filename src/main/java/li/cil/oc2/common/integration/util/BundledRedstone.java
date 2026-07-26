package li.cil.oc2.common.integration.util;

import li.cil.oc2.common.integration.projectred.BundledCableHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;


public class BundledRedstone {
    private static BundledRedstone INSTANCE = null;

    private BundledCableHandler handler = null;

    private BundledRedstone() { }

    public static synchronized BundledRedstone getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundledRedstone();
        }

        return INSTANCE;
    }

    public void register(BundledCableHandler handler) {
        this.handler = handler;
    }

    public boolean isAvailable() {
        return this.handler != null;
    }

    @Nullable
    public byte[] getBundledInput(Level level, BlockPos blockPos, Direction side) {
        if (handler != null) {
            return handler.getBundledInput(level, blockPos, side);
        } else {
            return null;
        }
    }
}
