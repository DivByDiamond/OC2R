package li.cil.oc2.common.blockentity.energy;

import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.radio.dock.DockBridge;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.EnergyStorage;
import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The robot dock station: charges what is on top and bridges a wired Ethernet network into the radio. */
public final class ChargerBlockEntity extends ModBlockEntity
        implements NamedDevice, TickableBlockEntity, NetworkInterface {
    private static final Predicate<Entity> ENTITY_PREDICATE =
            EntitySelector.NO_SPECTATORS.and(EntitySelector.ENTITY_STILL_ALIVE);

    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.chargerEnergyStorage);
    private boolean charging;
    private final AABB renderBoundingBox;
    private final DockBridge bridge = new DockBridge(
            () -> level instanceof final ServerLevel server ? server : null, this::getBlockPos, this::isValid, this);

    public ChargerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.CHARGER.get(), pos, state);
        renderBoundingBox = new AABB(pos.above());
    }

    @Override
    public void clientTick() {
        tick();
    }

    @Override
    public void serverTick() {
        tick();
    }

    private void tick() {
        if (level == null) {
            return;
        }

        charging = false;
        chargeBlock();
        chargeEntities();

        if (charging) {
            ChunkUtils.setLazyUnsaved(level, getBlockPos());
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT(registries));
        bridge.save(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        //? if >=26.1 {
        /*energy.deserializeNBT(registries, tag.getCompoundOrEmpty(Constants.ENERGY_TAG_NAME));
        *///?} else {
        energy.deserializeNBT(registries, tag.getCompound(Constants.ENERGY_TAG_NAME));
        //?}
        bridge.load(tag);
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        if (level != null) ServerScheduler.schedule(level, bridge::refresh);
    }

    @Override
    protected void unloadServer(final boolean removed) {
        super.unloadServer(removed);
        bridge.unload();
    }

    @Callback
    public boolean isCharging() {
        return charging;
    }

    @Callback
    public String getRadioEndpointId() {
        return bridge.endpointId();
    }

    /** Enrolls this dock as a radio client with an invitation issued for its endpoint id. */
    @Callback
    public void joinRadioNetwork(
            @Parameter("invite") final String invitation, @Parameter("secret") final String secret) {
        bridge.join(invitation, secret);
        setChanged();
    }

    @Callback
    public String getRadioAddress() {
        return bridge.address();
    }

    @Callback
    public java.util.Map<String, Object> getRadioStatus() {
        return bridge.status();
    }

    @Override
    public byte[] readEthernetFrame() {
        return new byte[0]; // a dock never originates frames itself
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int ttl) {
        bridge.ethernetIn(frame, ttl);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("charger");
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
registrar.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ChargerBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                Blocks.CHARGER.get());
        registrar.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> be instanceof final ChargerBlockEntity self ? self : null,
                Blocks.CHARGER.get());
    }

    private void chargeBlock() {
        assert level != null;

        if (energy.getEnergyStored() == 0) {
            return;
        }

        final var above = getBlockPos().above();
        final BlockEntity blockEntity = level.getBlockEntity(above);
        if (blockEntity != null) {
            final EnergyStorage energy = Platform.energy().getBlockEnergy(level, above, Direction.DOWN);
            if (energy != null) charge(energy);
            final var items =
                    Platform.capabilities()
                            .getBlockCapability(
                                    Capabilities.ItemHandler.BLOCK,
                                    level,
                                    above,
                                    null,
                                    blockEntity,
                                    null);
            if (items != null) chargeItems(items);
        }
    }

    private void chargeEntities() {
        assert level != null;

        if (energy.getEnergyStored() == 0) {
            return;
        }

        final List<Entity> entities =
                level.getEntities((Entity) null, new AABB(getBlockPos().above()), ENTITY_PREDICATE);
        for (final Entity entity : entities) {
            final EnergyStorage energy = Platform.energy().getEntityEnergy(entity, Direction.DOWN);
            if (energy != null) charge(energy);
            final var items = Platform.capabilities()
                    .getEntityCapability(Capabilities.ItemHandler.ENTITY, entity, null);
            if (items != null) chargeItems(items);
        }
    }

    private void chargeItems(final ItemHandler itemHandler) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                final EnergyStorage energy = Platform.energy().getItemEnergy(stack);
                if (energy != null) charge(energy);
            }
        }
    }

    private void charge(final EnergyStorage energyStorage) {
        assert level != null;

        final int amount = Math.min(energy.getEnergyStored(), Config.chargerEnergyPerTick);
        //? if >=26.1 {
        /*final boolean simulate = level.isClientSide();
        *///?} else {
        final boolean simulate = level.isClientSide;
        //?}
        if (energy.extractEnergy(energyStorage.receiveEnergy(amount, simulate), simulate) > 0) {
            charging = true;
        }
    }

    public AABB getRenderBoundingBox() {
        return renderBoundingBox;
    }
}