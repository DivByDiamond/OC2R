package li.cil.oc2.common.blockentity.monitor;

import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.loadbalancer.MonitorLoadBalancer;
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
        final boolean hasPowered;
        if (Config.monitorsUseEnergy()) {
            hasPowered =
                    blockEntity.stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, true)
                            >= Config.monitorEnergyPerTick;
            if (hasPowered) {
                blockEntity.stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, false);
            }
        } else hasPowered = true;
        updateMonitorState(blockEntity.stateManager.isMounted, hasPowered);
        if (!blockEntity.stateManager.hasEnergy
                || !blockEntity.stateManager.isPowered
                || (!blockEntity.stateManager.monitorDevice.hasChanges()
                        && !blockEntity.video.isKeyframeRequired())) {
            return;
        }
        MonitorLoadBalancer.offerFrame(
                blockEntity, () -> blockEntity.video.encodeFrame(blockEntity.stateManager.monitorDevice));
    }

    void updateMonitorState(final boolean newIsMounted, final boolean newHasEnergy) {
        if (!blockEntity.isOrigin()) return;
        final MonitorStateManager state = blockEntity.stateManager;
        if ((newIsMounted == state.isMounted && newHasEnergy == state.hasEnergy)
                || !blockEntity.isValid()) {
            return;
        }
        if (blockEntity.getLevel() != null
                && !blockEntity.getLevel().isClientSide()
                && blockEntity.getLevel().isLoaded(blockEntity.getBlockPos())) {
            if (state.isMounted && !newIsMounted) {
                blockEntity.video.clearPicture();
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
    }
}
