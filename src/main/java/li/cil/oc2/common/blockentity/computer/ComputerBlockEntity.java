package li.cil.oc2.common.blockentity.computer;

import java.util.*;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.computer.bus.ComputerBusElement;
import li.cil.oc2.common.blockentity.computer.contraption.ComputerContraptionHandler;
import li.cil.oc2.common.blockentity.computer.handler.ComputerItemStackHandlers;
import li.cil.oc2.common.blockentity.computer.persistence.ComputerBlockEntityPersistence;
import li.cil.oc2.common.blockentity.computer.vm.ComputerVirtualMachine;
import li.cil.oc2.common.bus.controller.AfterDeviceScanEvent;
import li.cil.oc2.common.bus.controller.BlockDeviceBusController;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import li.cil.oc2.common.container.ComputerTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.vm.*;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ComputerBlockEntity extends ModBlockEntity
        implements TerminalUserProvider, TickableBlockEntity, ICaptureInputStateStorage {
    public static final int MEMORY_SLOTS = 4,
            HARD_DRIVE_SLOTS = 4,
            FLASH_MEMORY_SLOTS = 1,
            CARD_SLOTS = 4,
            CPU_SLOTS = 1;
    boolean hasAddedOwnDevices;
    public boolean isNeighborUpdateScheduled;
    public volatile LevelChunk chunk;
    public final Terminal terminal = new Terminal();
    public final ComputerBusElement busElement = new ComputerBusElement(this);
    public final ComputerItemStackHandlers deviceItems =
            new ComputerItemStackHandlers(this, () -> this.getLevel().registryAccess());
    public final FixedEnergyStorage energy = new FixedEnergyStorage(Config.computerEnergyStorage);
    public final ComputerVirtualMachine virtualMachine =
            new ComputerVirtualMachine(
                    this,
                    new BlockDeviceBusController(busElement, Config.computerEnergyPerTick, this),
                    deviceItems::getDeviceAddressBase);
    final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean captureInputState;

    public ComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.COMPUTER.get(), pos, state);
        setNeedsLevelUnloadEvent();
        virtualMachine.busController.afterDeviceScanListeners.add(this::onAfterDeviceScan);
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public VMItemStackHandlers getItemStackHandlers() {
        return deviceItems;
    }

    @Override
    public boolean getCaptureInputState() {
        return captureInputState;
    }

    @Override
    public void setCaptureInputState(boolean value) {
        this.captureInputState = value;
    }

    public void start() {
        if (level != null && !level.isClientSide()) virtualMachine.start();
    }

    public void stop() {
        if (level != null && !level.isClientSide()) virtualMachine.stop();
    }

    public void openTerminalScreen(final ServerPlayer player) {
        ComputerTerminalContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void openInventoryScreen(final ServerPlayer player) {
        ComputerInventoryContainer.createServer(this, energy, virtualMachine.busController, player);
    }

    public void addTerminalUser(final Player player) {
        terminalUsers.add(player);
    }

    public void removeTerminalUser(final Player player) {
        terminalUsers.remove(player);
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        return terminalUsers;
    }

    public void handleNeighborChanged() {
        if (level != null && !level.isClientSide()) virtualMachine.busController.scheduleBusScan();
    }

    public void onAfterDeviceScan(final AfterDeviceScanEvent event) {
        if (event.didDevicesChange()) level.invalidateCapabilities(getBlockPos());
    }

    @SuppressWarnings("unchecked")
    public <T extends Device> @Nullable T getFirstDevice(Class<T> cls) {
        for (final Device device : virtualMachine.busController.getDevices())
            if (cls.isAssignableFrom(device.getClass())) return (T) device;
        return null;
    }

    public UUID getDeviceId() {
        return busElement.deviceId;
    }

    public boolean isContraptionVirtualClone() {
        return ComputerContraptionHandler.isContraptionVirtualClone(this);
    }

    @Nullable
    public ComputerBlockEntity getPrimaryForContraptionRendering() {
        return ComputerContraptionHandler.getPrimaryForContraptionRendering(this);
    }

    @Override
    public void clientTick() {
        terminal.clientTick();
        ComputerContraptionHandler.registerInClientRegistry(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ComputerContraptionHandler.unregisterFromClientRegistry(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ComputerContraptionHandler.unregisterFromClientRegistry(this);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (!hasAddedOwnDevices) {
            hasAddedOwnDevices = true;
            busElement.addOwnDevices();
        }
        if (isNeighborUpdateScheduled) {
            isNeighborUpdateScheduled = false;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
        chunk = level.getChunkAt(getBlockPos());
        virtualMachine.tick();
    }

    @Override
    protected void applyImplicitComponents(final DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        var container = componentInput.get(DataComponents.RESTRICTED_CONTAINER);
        if (container != null) {
            deviceItems.loadItems(getLevel().registryAccess(), container);
        } else {
            var block_entity_data =
                    componentInput.get(
                            net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            if (block_entity_data == null) return;
            block_entity_data.copyTag().size();
        }
    }

    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        var container = new RestrictedContainer();
        deviceItems.saveItems(container);
        components.set(DataComponents.RESTRICTED_CONTAINER, container);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put("terminal", ComputerBlockEntityPersistence.getUpdateTag(this, registries));
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        ComputerBlockEntityPersistence.handleUpdateTagClient(this, tag, registries);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ComputerBlockEntityPersistence.saveAdditional(this, tag, registries);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ComputerBlockEntityPersistence.loadAdditional(this, tag, registries);
    }

    public void exportToItemStack(final ItemStack stack) {
        var container = new RestrictedContainer();
        deviceItems.exportDeviceDataToItemStacks();
        deviceItems.saveItems(container);
        stack.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);
    }

    @Override
    protected void loadClient() {
        super.loadClient();
        terminal.setDisplayOnly(true);
        ComputerContraptionHandler.registerInClientRegistry(this);
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        assert level != null;
        virtualMachine.state.builtinDevices.rtcMinecraft.setLevel(level);
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);
        if (isRemove) virtualMachine.stop();
        else virtualMachine.suspend();
        virtualMachine.dispose();
        busElement.scheduleScan();
    }

    public void sendToClientsTrackingComputer(final CustomPacketPayload message) {
        if (chunk != null) Network.sendToClientsTrackingChunk(message, chunk);
    }
}