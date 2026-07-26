/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity.computer;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.controller.BlockDeviceBusController;
import li.cil.oc2.common.blockentity.computer.bus.ComputerBusElement;
import li.cil.oc2.common.blockentity.computer.handler.ComputerItemStackHandlers;
import li.cil.oc2.common.blockentity.computer.vm.ComputerVirtualMachine;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.bus.controller.CommonDeviceBusController;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import li.cil.oc2.common.container.ComputerTerminalContainer;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.ext.ICaptureInputStateStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.ComputerTerminalOutputMessage;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.*;
import li.cil.oc2.common.vm.*;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ComputerBlockEntity extends ModBlockEntity implements TerminalUserProvider, TickableBlockEntity, ICaptureInputStateStorage {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String DEVICES_TAG_NAME = "devices";
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String STATE_TAG_NAME = "state";
    private static final String ENERGY_TAG_NAME = "energy";

    public static final int MEMORY_SLOTS = 4;
    public static final int HARD_DRIVE_SLOTS = 4;
    public static final int FLASH_MEMORY_SLOTS = 1;
    public static final int CARD_SLOTS = 4;
    public static final int CPU_SLOTS = 1;

    /**
     * Client-side registry of "primary" (non-contraption) ComputerBlockEntities
     * keyed by their persistent device id.
     *
     * When Create: Aeronautics / Sable assembles a contraption, it creates a
     * "virtual clone" of each BlockEntity at a far-away virtual position
     * (typically ~20 million blocks from origin) for rendering on the ship.
     * The virtual clone's terminal never receives ComputerTerminalOutputMessage
     * updates because the server sends those with the original BlockPos, and
     * the client only applies them to the BlockEntity at that exact pos.
     *
     * To make the contraption-rendered PC actually display its terminal, we
     * register every "primary" (non-virtual) ComputerBlockEntity here on tick.
     * The renderer and the message handler can then look up the primary by
     * device id and use / forward its terminal state to the virtual clone.
     *
     * A BlockEntity is considered "primary" (and gets registered) when its
     * BlockPos is within a reasonable range of the origin. Sable's virtual
     * positions are at ~20M blocks, so the 1M threshold comfortably separates
     * the two.
     */
    private static final ConcurrentHashMap<UUID, ComputerBlockEntity> PRIMARY_BY_DEVICE_ID = new ConcurrentHashMap<>();
    private static final long VIRTUAL_POSITION_THRESHOLD = 1_000_000L;

    ///////////////////////////////////////////////////////////////////

    boolean hasAddedOwnDevices;
    public boolean isNeighborUpdateScheduled;
    public volatile LevelChunk chunk;

    ///////////////////////////////////////////////////////////////////

    public final Terminal terminal = new Terminal();
    public final ComputerBusElement busElement = new ComputerBusElement(this);
    public final ComputerItemStackHandlers deviceItems = new ComputerItemStackHandlers(this, () -> this.getLevel().registryAccess());
    public final FixedEnergyStorage energy = new FixedEnergyStorage(Config.computerEnergyStorage);
    public final ComputerVirtualMachine virtualMachine = new ComputerVirtualMachine(this, new BlockDeviceBusController(busElement, Config.computerEnergyPerTick, this), deviceItems::getDeviceAddressBase);
    final Set<Player> terminalUsers = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean captureInputState;

    ///////////////////////////////////////////////////////////////////

    public ComputerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.COMPUTER.get(), pos, state);

        // We want to unload devices even on level unload to free global resources.
        setNeedsLevelUnloadEvent();

        virtualMachine.busController.onAfterDeviceScan.add(this::onAfterDeviceScan);
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
        if (level != null && !level.isClientSide()) {
            virtualMachine.start();
        }
    }

    public void stop() {
        if (level != null && !level.isClientSide()) {
            virtualMachine.stop();
        }
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
        if (level != null && !level.isClientSide()) {
            virtualMachine.busController.scheduleBusScan();
        }
    }

    public void onAfterDeviceScan(final CommonDeviceBusController.AfterDeviceScanEvent event) {
        if (event.didDevicesChange()) {
            level.invalidateCapabilities(getBlockPos());
        }
    }

    public <T extends Device> @Nullable T getFirstDevice(Class<T> cls) {
        for (final Device device : virtualMachine.busController.getDevices())
        {
            if (cls.isAssignableFrom(device.getClass())) {
                //noinspection unchecked
                return (T) device;
            }
        }

        return null;
    }

    /**
     * Persistent UUID of this computer's bus element.
     *
     * Preserved across block-entity save/load, so it is identical on the
     * original BlockEntity and on any "virtual clone" that Create: Aeronautics
     * / Sable creates at a far-away virtual position for contraption
     * rendering. The client uses it to route terminal output to the right
     * Terminal instance even when the contraption-rendered clone lives at a
     * different BlockPos than the server-side BlockEntity.
     */
    public UUID getDeviceId() {
        return busElement.deviceId;
    }

    /**
     * Returns true if this BlockEntity appears to be a Sable/Create:Aeronautics
     * "virtual clone" — i.e. Sable has teleported its BlockPos to a far-away
     * virtual position for contraption rendering. The original BlockEntity
     * (still at its real position) stays at small coordinates.
     */
    public boolean isContraptionVirtualClone() {
        final BlockPos pos = getBlockPos();
        return Math.abs(pos.getX()) > VIRTUAL_POSITION_THRESHOLD
            || Math.abs(pos.getZ()) > VIRTUAL_POSITION_THRESHOLD;
    }

    /**
     * Look up the "primary" (non-virtual) ComputerBlockEntity that shares the
     * same persistent device id as this one. Returns this when called on the
     * primary itself; returns the primary when called on a virtual clone.
     *
     * Used by the BER to render the primary's terminal (which receives the
     * ComputerTerminalOutputMessage updates) on the virtual clone's face.
     */
    @Nullable
    public ComputerBlockEntity getPrimaryForContraptionRendering() {
        if (!isContraptionVirtualClone()) {
            return this;
        }
        final ComputerBlockEntity primary = PRIMARY_BY_DEVICE_ID.get(getDeviceId());
        if (primary != null && !primary.isRemoved()) {
            return primary;
        }
        return this;
    }

    private void registerInClientRegistry() {
        if (level == null || !level.isClientSide()) {
            return;
        }
        if (isContraptionVirtualClone()) {
            return; // Only register the primary, not the virtual clone.
        }
        PRIMARY_BY_DEVICE_ID.put(getDeviceId(), this);
    }

    private void unregisterFromClientRegistry() {
        if (level == null || !level.isClientSide()) {
            return;
        }
        // remove() with a value comparison to avoid removing a different BE
        // that happened to register over us.
        PRIMARY_BY_DEVICE_ID.remove(getDeviceId(), this);
    }

    @Override
    public void clientTick() {
        terminal.clientTick();
        registerInClientRegistry();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unregisterFromClientRegistry();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterFromClientRegistry();
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        // Always add devices provided for the computer itself, even if there's no
        // adjacent cable. Because that would just be weird.
        if (!hasAddedOwnDevices) {
            hasAddedOwnDevices = true;
            busElement.addOwnDevices();
        }

        if (isNeighborUpdateScheduled) {
            isNeighborUpdateScheduled = false;
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }

        // Just grab it again every tick, to avoid this becoming invalid if something tries to
        // mess with this BlockEntity in unexpected ways.
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
            var block_entity_data = componentInput.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            if (block_entity_data == null) return;
            var tag = block_entity_data.copyTag();
            tag.size();
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

        tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        tag.putInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME, virtualMachine.getBusState().ordinal());
        tag.putInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME, virtualMachine.getRunState().ordinal());
        tag.putString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME, Component.Serializer.toJson(virtualMachine.getBootError(), registries));

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);

        // Only update client-side state on the client
        if (level != null && level.isClientSide()) {
            virtualMachine.setBusStateClient(CommonDeviceBusController.BusState.values()[tag.getInt(AbstractVirtualMachine.BUS_STATE_TAG_NAME)]);
            virtualMachine.setRunStateClient(VMRunState.values()[tag.getInt(AbstractVirtualMachine.RUN_STATE_TAG_NAME)]);
            virtualMachine.setBootErrorClient(Component.Serializer.fromJson(tag.getString(AbstractVirtualMachine.BOOT_ERROR_TAG_NAME), registries));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (virtualMachine.getRunState() != VMRunState.STOPPED) {
            tag.put(STATE_TAG_NAME, virtualMachine.serialize());
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }

        tag.put(ENERGY_TAG_NAME, energy.serializeNBT(registries));
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save(registries));
        tag.put(ITEMS_TAG_NAME, deviceItems.saveItems(registries));
        tag.put(DEVICES_TAG_NAME, deviceItems.saveDevices(registries));
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.deserializeNBT(registries, tag.getCompound(ENERGY_TAG_NAME));
        busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        deviceItems.loadItems(registries, tag.getCompound(ITEMS_TAG_NAME));
        deviceItems.loadDevices(registries, tag.getCompound(DEVICES_TAG_NAME));
        virtualMachine.deserialize(tag.getCompound(STATE_TAG_NAME));
        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
    }

    public void exportToItemStack(final ItemStack stack) {
        var container = new RestrictedContainer();
        deviceItems.exportDeviceDataToItemStacks();
        deviceItems.saveItems(container);
        stack.set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER, container);
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final ComputerBlockEntity self) {
                    return self.deviceItems.combinedItemHandlers;
                }
                return null;
            },
            Blocks.COMPUTER.get()
        );
        event.registerBlock(
            Capabilities.DeviceBusElement.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final ComputerBlockEntity self) {
                    return self.busElement;
                }
                return null;
            },
            Blocks.COMPUTER.get()
        );
        // TerminalUserProvider used to be offered via the old collectCapabilities
        // hook in 1.20.1; the 1.21.1 port dropped it. External mods (e.g. soft
        // integrations with Create / Aeronautics / Sable peripherals) that look
        // up this capability on the computer block to find a terminal host need
        // it to be present — restore it.
        event.registerBlock(
            Capabilities.TerminalUserProvider.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final ComputerBlockEntity self) {
                    return self;
                }
                return null;
            },
            Blocks.COMPUTER.get()
        );

        if (Config.computersUseEnergy()) {
            event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                Blocks.COMPUTER.get()
            );
        }
    }

    @Override
    protected void loadClient() {
        super.loadClient();

        terminal.setDisplayOnly(true);

        // Register as soon as we're added to a client level, so the BER can
        // find us even before the first clientTick() fires. The lookup is
        // by persistent device id — used by getPrimaryForContraptionRendering()
        // when the BER is asked to render a Sable / Create:Aeronautics virtual
        // clone of this computer.
        registerInClientRegistry();
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

        if (isRemove) {
            virtualMachine.stop();
        } else {
            virtualMachine.suspend();
        }

        virtualMachine.dispose();

        // This is necessary in case some other controller found us before our controller
        // did its scan, which can happen because the scan can happen with a delay. In
        // that case we don't know that controller and disposing our controller won't
        // notify it, so we also send out a notification through our bus element, which
        // would be registered with other controllers in that case.
        busElement.scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    public void sendToClientsTrackingComputer(final CustomPacketPayload message) {
        if (chunk != null) {
            Network.sendToClientsTrackingChunk(message, chunk);
        }
    }

}
