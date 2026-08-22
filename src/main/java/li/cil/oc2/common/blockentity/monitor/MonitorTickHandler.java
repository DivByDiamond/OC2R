package li.cil.oc2.common.blockentity.monitor;

import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.MonitorStateMessage;
import net.minecraft.world.level.block.Block;

final class MonitorTickHandler {
    private final MonitorBlockEntity blockEntity;

    MonitorTickHandler(final MonitorBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    void tick() {
        if (blockEntity.getLevel() == null || !blockEntity.isValid()) return;
        // Only the origin runs the live monitor logic (energy, framebuffer, network sync).
        // Sub-blocks are inert: their BlockEntity exists only so Minecraft can persist their
        // multiblock offset BlockState.
        if (!blockEntity.isOrigin()) return;
        final boolean hasPowered = updateEnergy();
        updateMonitorState(blockEntity.stateManager.isMounted, hasPowered);
        if (shouldOfferFrame()) {
            blockEntity.video.sendFrame(blockEntity.stateManager.monitorDevice);
        }
    }

    private boolean updateEnergy() {
        if (!Config.monitorsUseEnergy()) {
            return true;
        }
        final boolean hasPowered =
                blockEntity.stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, true)
                        >= Config.monitorEnergyPerTick;
        if (hasPowered) {
            blockEntity.stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, false);
        }
        return hasPowered;
    }

    private boolean shouldOfferFrame() {
        return blockEntity.stateManager.hasEnergy
                && blockEntity.stateManager.isPowered
                && blockEntity.stateManager.monitorDevice.hasChanges();
    }

    void updateMonitorState(final boolean newIsMounted, final boolean newHasEnergy) {
        if (!blockEntity.isOrigin()) return;
        final MonitorStateManager state = blockEntity.stateManager;
        if ((newIsMounted == state.isMounted && newHasEnergy == state.hasEnergy)
                || !blockEntity.isValid()) {
            return;
        }
        if (!shouldApplyState()) {
            return;
        }
        state.isMounted = newIsMounted;
        state.hasEnergy = newHasEnergy;
        blockEntity.getLevel().setBlock(
                blockEntity.getBlockPos(),
                blockEntity.getBlockState().setValue(MonitorBlock.LIT, newIsMounted),
                Block.UPDATE_CLIENTS);
        NetworkMessages.sendToClientsTrackingBlockEntity(
                new MonitorStateMessage(blockEntity, newIsMounted, newHasEnergy), blockEntity);
    }

    private boolean shouldApplyState() {
        return blockEntity.getLevel() != null
                && !blockEntity.getLevel().isClientSide()
                && blockEntity.getLevel().isLoaded(blockEntity.getBlockPos());
    }
}
