package li.cil.oc2.common.blockentity.monitor;

import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.WIDTH;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.MonitorBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.MonitorDisplayContainer;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.network.MonitorLoadBalancer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.MonitorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.MonitorStateMessage;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class MonitorBlockEntity extends ModBlockEntity
        implements TickableBlockEntity, ICaptureInputStateStorage {
    final Picture picture = Picture.create(WIDTH, HEIGHT, ColorSpace.YUV420J);
    @Nullable FrameConsumer frameConsumer;
    final MonitorVideoEncoder encoder = new MonitorVideoEncoder();
    final MonitorVideoDecoder decoder = new MonitorVideoDecoder();
    final MonitorStateManager stateManager;
    private long lastKeepAliveSentAt;

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

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        if (consumer == frameConsumer) return;
        synchronized (picture) {
            this.frameConsumer = consumer;
            if (frameConsumer != null) frameConsumer.processFrame(picture);
        }
    }

    private void handleMountedChanged(final boolean value) {
        updateMonitorState(value, stateManager.hasEnergy);
    }

    private void updateMonitorState(final boolean newIsMounted, final boolean newHasEnergy) {
        if ((newIsMounted == stateManager.isMounted && newHasEnergy == stateManager.hasEnergy)
                || !isValid()) return;
        if (level != null && !level.isClientSide() && level.isLoaded(getBlockPos())) {
            if (stateManager.isMounted && !newIsMounted)
                Arrays.fill(picture.getPlaneData(0), (byte) -128);
            stateManager.isMounted = newIsMounted;
            stateManager.hasEnergy = newHasEnergy;
            level.setBlock(
                    getBlockPos(),
                    getBlockState().setValue(MonitorBlock.LIT, newIsMounted),
                    Block.UPDATE_CLIENTS);
            Network.sendToClientsTrackingBlockEntity(
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

    public void setRequiresKeyframe() {
        encoder.setRequiresKeyframe();
    }

    public void applyNextFrameClient(final ByteBuffer frameData) {
        decoder.applyNextFrameClient(frameData, picture, frameConsumer);
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > 1000) {
            lastKeepAliveSentAt = now;
            Network.sendToServer(new MonitorRequestFramebufferMessage(this));
        }
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
                || (!stateManager.monitorDevice.hasChanges() && !encoder.isKeyframeRequired()))
            return;
        MonitorLoadBalancer.offerFrame(
                this, () -> encoder.encodeFrame(picture, stateManager.monitorDevice));
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

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final MonitorBlockEntity self) {
                        if (side != self.getBlockState().getValue(MonitorBlock.FACING))
                            return self.stateManager.deviceGroup;
                    }
                    return null;
                },
                Blocks.MONITOR.get());
        if (Config.monitorsUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) -> {
                        if (be instanceof final MonitorBlockEntity self) {
                            if (side != self.getBlockState().getValue(MonitorBlock.FACING))
                                return self.stateManager.energy;
                        }
                        return null;
                    },
                    Blocks.MONITOR.get());
        }
    }
}