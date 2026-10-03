package li.cil.oc2.common.integration.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class BundledRedstone {
    /** Source of bundled redstone input, provided by a loader specific mod integration. */
    public interface Handler {
        byte[] getBundledInput(Level level, BlockPos blockPos, Direction side);
    }

    private static BundledRedstone INSTANCE = null;

    private Handler handler = null;

    private BundledRedstone() {}

    public static synchronized BundledRedstone getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BundledRedstone();
        }

        return INSTANCE;
    }

    public void register(Handler handler) {
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
            return new byte[0];
        }
    }
}