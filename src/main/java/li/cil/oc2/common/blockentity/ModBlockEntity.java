package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.util.scheduler.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ModBlockEntity extends BlockEntity {
    private final Runnable unloadHandler = this::onWorldUnloaded;
    private boolean needsWorldUnloadEvent;
    private boolean isUnloaded;

    protected ModBlockEntity(
            final BlockEntityType<?> blockEntityType, final BlockPos pos, final BlockState state) {
        super(blockEntityType, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (level == null) {
            return;
        }

        if (level.isClientSide()) {
            loadClient();
        } else {
            loadServer();

            if (needsWorldUnloadEvent) {
                ServerScheduler.scheduleOnUnload(level, unloadHandler);
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded(); // -> invalidateCaps()
        onUnload(false);
        isUnloaded = true;
    }

    public void onWorldUnloaded() {
        onUnload(false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved(); // -> invalidateCaps()
        if (!isUnloaded) {
            onUnload(true);
        }
    }

    public boolean isValid() {
        return !isRemoved() && !isUnloaded;
    }

    protected void onUnload(final boolean isRemove) {
        if (level != null && !level.isClientSide()) {
            unloadServer(isRemove);
            ServerScheduler.cancelOnUnload(level, unloadHandler);
        }
    }

    protected final void setNeedsLevelUnloadEvent() {
        needsWorldUnloadEvent = true;
    }

    protected void loadClient() {}

    protected void loadServer() {}

    protected void unloadServer(final boolean isRemove) {}
}