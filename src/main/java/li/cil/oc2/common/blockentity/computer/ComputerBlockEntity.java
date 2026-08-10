package li.cil.oc2.common.blockentity.computer;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.computer.bus.ComputerBusElement;
import li.cil.oc2.common.blockentity.computer.contraption.ComputerContraptionHandler;
import li.cil.oc2.common.blockentity.computer.handler.ComputerItemStackHandlers;
import li.cil.oc2.common.blockentity.computer.persistence.ComputerBlockEntityPersistence;
import li.cil.oc2.common.blockentity.computer.terminal.ComputerTerminalManager;
import li.cil.oc2.common.blockentity.computer.vm.ComputerVirtualMachine;
import li.cil.oc2.common.bus.controller.BlockDeviceBusController;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.vm.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ComputerBlockEntity extends ModBlockEntity
        implements TickableBlockEntity {
    public static final int MEMORY_SLOTS = 4,
            HARD_DRIVE_SLOTS = 4,
            FLASH_MEMORY_SLOTS = 1,
            CARD_SLOTS = 4,
            CPU_SLOTS = 1,
            GPU_SLOTS = 1;
    boolean hasAddedOwnDevices;
    public boolean isNeighborUpdateScheduled;
    public final ComputerBusElement busElement = new ComputerBusElement(this);
    public final ComputerItemStackHandlers deviceItems =
            new ComputerItemStackHandlers(this, () -> this.getLevel().registryAccess());
    public final FixedEnergyStorage energy = new FixedEnergyStorage(Config.computerEnergyStorage);
    public final ComputerVirtualMachine virtualMachine =
            new ComputerVirtualMachine(
                    this,
                    new BlockDeviceBusController(busElement, Config.computerEnergyPerTick, this),
                    deviceItems::getDeviceAddressBase);
    public final ComputerTerminalManager terminalManager = new ComputerTerminalManager(this);

    public ComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.COMPUTER.get(), pos, state);
        setNeedsLevelUnloadEvent();
        virtualMachine.busController.afterDeviceScanListeners.add(terminalManager::onAfterDeviceScan);
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public VMItemStackHandlers getItemStackHandlers() {
        return deviceItems;
    }

    @Override
    public void clientTick() {
        terminalManager.terminal.clientTick();
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
        terminalManager.chunk = level.getChunkAt(getBlockPos());
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
        terminalManager.terminal.setDisplayOnly(true);
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
}
