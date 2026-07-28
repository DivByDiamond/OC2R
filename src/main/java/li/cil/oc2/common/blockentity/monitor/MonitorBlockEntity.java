package li.cil.oc2.common.blockentity.monitor;

import java.util.UUID;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.MonitorDisplayContainer;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.loadbalancer.MonitorLoadBalancer;
import li.cil.oc2.common.network.message.monitor.MonitorStateMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MonitorBlockEntity extends ModBlockEntity
        implements TickableBlockEntity, ICaptureInputStateStorage {
    public final MonitorVideoController video = new MonitorVideoController(this);
    final MonitorStateManager stateManager;

    public MonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.MONITOR.get(), pos, state);
        stateManager = new MonitorStateManager(this, this::handleMountedChanged);
        setNeedsLevelUnloadEvent();
    }

    public void start() {
        stateManager.isPowered = true;
    }

    public void stop() {
        stateManager.isPowered = false;
    }

    public void handleInput(final int keycode, final boolean isDown) {
        stateManager.keyboardDevice.sendKeyEvent(keycode, isDown);
    }

    private void handleMountedChanged(final boolean value) {
        updateMonitorState(value, stateManager.hasEnergy);
    }

    private void updateMonitorState(final boolean newIsMounted, final boolean newHasEnergy) {
        if ((newIsMounted == stateManager.isMounted && newHasEnergy == stateManager.hasEnergy)
                || !isValid()) return;
        if (level != null && !level.isClientSide() && level.isLoaded(getBlockPos())) {
            if (stateManager.isMounted && !newIsMounted)
                video.clearPicture();
            stateManager.isMounted = newIsMounted;
            stateManager.hasEnergy = newHasEnergy;
            level.setBlock(
                    getBlockPos(),
                    getBlockState().setValue(MonitorBlock.LIT, newIsMounted),
                    Block.UPDATE_CLIENTS);
            NetworkMessages.sendToClientsTrackingBlockEntity(
                    new MonitorStateMessage(this, newIsMounted, newHasEnergy), this);
        }
    }

    public void applyMonitorStateClient(final boolean isRendering, final boolean hasEnergy) {
        if (level == null || !level.isClientSide()) return;
        stateManager.isMounted = isRendering;
        stateManager.hasEnergy = hasEnergy;
    }

    public boolean hasPower() {
        return stateManager.hasEnergy;
    }

    public boolean getPowerState() {
        return stateManager.isPowered;
    }

    public boolean isMounted() {
        return stateManager.isMounted;
    }

    public MonitorGUIRenderer getMonitor() {
        return stateManager.monitor;
    }

    public UUID getDeviceId() {
        return stateManager.deviceId;
    }

    public void openTerminalScreen(final ServerPlayer player) {
        MonitorDisplayContainer.createServer(this, stateManager.energy, player);
    }

    @Override
    public void clientTick() {
        MonitorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    protected void loadClient() {
        MonitorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    public void serverTick() {
        if (level == null || !isValid()) return;
        final boolean hasPowered;
        if (Config.monitorsUseEnergy()) {
            hasPowered =
                    stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, true)
                            >= Config.monitorEnergyPerTick;
            if (hasPowered) stateManager.energy.extractEnergy(Config.monitorEnergyPerTick, false);
        } else hasPowered = true;
        updateMonitorState(stateManager.isMounted, hasPowered);
        if (!stateManager.hasEnergy
                || !stateManager.isPowered
                || (!stateManager.monitorDevice.hasChanges() && !video.isKeyframeRequired()))
            return;
        MonitorLoadBalancer.offerFrame(
                this, () -> video.encodeFrame(stateManager.monitorDevice));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return stateManager.createUpdateTag(super.getUpdateTag(registries));
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        stateManager.readUpdateTag(tag);
        MonitorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        MonitorContraptionHelper.unregisterFromClientRegistry(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        MonitorContraptionHelper.unregisterFromClientRegistry(this);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        stateManager.savePersistent(tag, registries);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stateManager.loadPersistent(tag, registries);
    }

    @Override
    public boolean getCaptureInputState() {
        return stateManager.captureInputState;
    }

    @Override
    public void setCaptureInputState(final boolean value) {
        stateManager.captureInputState = value;
    }
}
