package li.cil.oc2.common.blockentity.monitor;

import java.util.UUID;
import li.cil.oc2.client.model.monitor.MonitorModelTypes;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.monitor.misc.MonitorContraptionHelper;
import li.cil.oc2.common.blockentity.monitor.video.MonitorVideoController;
import li.cil.oc2.common.container.monitor.MonitorDisplayContainer;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class MonitorBlockEntity extends ModBlockEntity
        implements TickableBlockEntity, ICaptureInputStateStorage {
    public final MonitorVideoController video = new MonitorVideoController(this);
    public final MonitorStateManager stateManager;
    private final MonitorTickHandler tickHandler;

    public MonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.MONITOR.get(), pos, state);
        stateManager = new MonitorStateManager(this, this::handleMountedChanged);
        tickHandler = new MonitorTickHandler(this);
        setNeedsLevelUnloadEvent();
    }

    /** @return {@code true} if this block entity is the master (origin) of its multiblock. */
    public boolean isOrigin() {
        return MonitorMultiblock.isOrigin(getBlockState());
    }

    /**
     * Expose the block's multiblock position (width/height/offset/facing) to the fragment-based
     * baked model. Client only; mirror of {@code BusCableBlockEntity#getModelData()}.
     */
    @Override
    public ModelData getModelData() {
        return MonitorModelTypes.fromState(getBlockState());
    }

    public void start() {
        if (!isOrigin()) return;
        stateManager.isPowered = true;
    }

    public void stop() {
        if (!isOrigin()) return;
        stateManager.isPowered = false;
    }

    public void handleInput(final int keycode, final boolean isDown) {
        if (!isOrigin()) return;
        stateManager.keyboardDevice.sendKeyEvent(keycode, isDown);
    }

    private void handleMountedChanged(final boolean value) {
        tickHandler.updateMonitorState(value, stateManager.hasEnergy);
    }

    public void applyMonitorStateClient(final boolean isRendering, final boolean hasEnergy) {
        if (level == null || !level.isClientSide()) return;
        stateManager.isMounted = isRendering;
        stateManager.hasEnergy = hasEnergy;
    }

    public boolean hasPower() {
        return stateManager.hasEnergy;
    }

    public boolean getPowerState() { // NOPMD getter API consumed by renderer/container/screens
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
        if (!isOrigin()) return;
        MonitorDisplayContainer.createServer(this, stateManager.energy, player);
    }

    @Override
    public void clientTick() {
        // Only the origin registers with the contraption helper and tracks framebuffer state.
        // Sub-blocks have no video device and never receive frames.
        if (!isOrigin()) return;
        MonitorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    protected void loadClient() {
        if (!isOrigin()) return;
        MonitorContraptionHelper.registerInClientRegistry(this);
    }

    @Override
    public void serverTick() {
        tickHandler.tick();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return stateManager.createUpdateTag(super.getUpdateTag(registries));
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // Sub-blocks still receive the origin's projecting/has_energy state via the tag, but we
        // don't want them to register with the contraption helper (only the origin should be
        // rendered). The isOrigin() check inside registerInClientRegistry also covers this.
        stateManager.readUpdateTag(tag);
        if (isOrigin()) {
            MonitorContraptionHelper.registerInClientRegistry(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (isOrigin()) {
            MonitorContraptionHelper.unregisterFromClientRegistry(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (isOrigin()) {
            MonitorContraptionHelper.unregisterFromClientRegistry(this);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Sub-blocks don't need to persist their state: when the multiblock is reloaded the
        // origin's BlockState (WIDTH/HEIGHT) plus each sub-block's ORIGIN_OFFSET_X/Y fully
        // describe the structure, and the origin's persisted state fully describes the
        // device/energy. Persisting state on sub-blocks would just waste space and risk
        // diverging from the origin. We still call savePersistent for backward compat with
        // existing saves where this BE might have been an origin before.
        if (isOrigin()) {
            stateManager.savePersistent(tag, registries);
            tag.putBoolean("capture_input", stateManager.captureInputState);
        }
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Always load: a BE that was previously an origin may be reloaded as a sub-block after
        // a multiblock merge, and we want to carry its deviceId/energy across the transition.
        stateManager.loadPersistent(tag, registries);
        if (tag.contains("capture_input")) {
            stateManager.captureInputState = tag.getBoolean("capture_input");
        }
    }

    /**
     * Save the persistent monitor state (energy, power, deviceId, capture input) into a fresh
     * tag, used by {@link MonitorMultiblock} when shifting the origin of a multiblock to a
     * different block position. The video controller / encoder state is intentionally not
     * transferred — it is recomputed from the framebuffer device on the next tick.
     */
    public CompoundTag saveStateForTransfer(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        stateManager.savePersistent(tag, registries);
        tag.putBoolean("capture_input", stateManager.captureInputState);
        return tag;
    }

    /**
     * Load the persistent monitor state previously produced by
     * {@link #saveStateForTransfer(HolderLookup.Provider)} into this block entity. Used by
     * {@link MonitorMultiblock} when this block becomes the new origin of a multiblock whose
     * previous origin is being demoted to a sub-block.
     */
    public void loadStateFromTransfer(final CompoundTag tag, final HolderLookup.Provider registries) {
        stateManager.loadPersistent(tag, registries);
        if (tag.contains("capture_input")) {
            stateManager.captureInputState = tag.getBoolean("capture_input");
        }
        setChanged();
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
