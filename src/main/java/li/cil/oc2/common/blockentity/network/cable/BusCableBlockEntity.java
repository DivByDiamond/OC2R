package li.cil.oc2.common.blockentity.network.cable;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.facade.FacadeManager;
import li.cil.oc2.common.blockentity.network.cable.facade.FacadeType;
import li.cil.oc2.common.blockentity.network.cable.facade.InterfaceNameManager;
import li.cil.oc2.common.blockentity.network.cable.facade.NeighborListener;
import li.cil.oc2.common.blockentity.network.cable.faceoverride.FaceOverride;
import li.cil.oc2.common.blockentity.network.cable.faceoverride.FaceOverrides;
import li.cil.oc2.common.energy.CableEnergyStorage;
import li.cil.oc2.common.energy.EnergyNetworkCache;
import li.cil.oc2.common.energy.EnergyTransferManager;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;

public final class BusCableBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";
    private static final String FACADE_TAG_NAME = "facade";
    private static final String FACE_OVERRIDES_TAG_NAME = "faceOverrides";

    public final BusCableBusElement busElement = new BusCableBusElement(this);
    public final CableEnergyStorage energy = new CableEnergyStorage();
    public long energyDistributionTick = -1;
    public long energyRedistributeTick = -1;
    private final FaceOverrides faceOverrides = new FaceOverrides();
    final FacadeManager facadeManager = new FacadeManager(this);
    final InterfaceNameManager interfaceNameManager = new InterfaceNameManager(this);
    private final BusCableModelData modelData = new BusCableModelData(this);
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private final ICapabilityInvalidationListener[] neighborListeners =
            new NeighborListener[Constants.BLOCK_FACE_COUNT];

    public BusCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.BUS_CABLE.get(), pos, state);
        requestModelDataUpdate();
    }

    public FaceOverride getFaceOverride(@Nullable final Direction side) {
        return faceOverrides.get(side);
    }

    /** Sets the override for {@code side} and re-resolves the bus if it changed. */
    public void setFaceOverride(final Direction side, final FaceOverride override) {
        if (!faceOverrides.set(side, override)) {
            return;
        }
        setChanged();
        handleConfigurationChanged(side, true);
    }

    public String getInterfaceName(final Direction side) {
        return interfaceNameManager.getInterfaceName(side);
    }

    @Override
    public void serverTick() {
        assert level != null;
        EnergyTransferManager.distribute(this);
    }

    public void setInterfaceName(final Direction side, final String name) {
        interfaceNameManager.setInterfaceName(side, name);
    }

    public ItemStack getFacade() {
        return facadeManager.getFacade();
    }

    public FacadeType getFacadeType(final ItemStack stack) {
        return facadeManager.getFacadeType(stack);
    }

    public FacadeType getFacadeType(@Nullable final BlockState state) {
        return facadeManager.getFacadeType(state);
    }

    public void setFacade(final ItemStack stack) {
        facadeManager.setFacade(stack);
    }

    public void removeFacade() {
        facadeManager.removeFacade();
    }

    public void handleConfigurationChanged(
            @Nullable final Direction side, final boolean neighborConnectivityChanged) {
        if (side != null) {
            // Any change of this side's configuration (connection type toggled by the block state
            // property, face override set by setFaceOverride) also discards that side's interface
            // name: the name is a synthetic device advertised behind this side, and it must not
            // outlive the configuration it was defined for.
            setInterfaceName(side, "");
            if (level != null) level.invalidateCapabilities(getBlockPos());
            // scheduleScan() below only re-walks bus TOPOLOGY (cable-to-cable/computer BFS); it
            // never re-evaluates which devices sit behind this specific side, which is cached
            // separately per side and otherwise only refreshed by the load-time scan or a
            // neighbor's own capability invalidation. Without this, a side that just went
            // NONE -> INTERFACE keeps reporting zero devices until something unrelated forces
            // a rescan.
            busElement.updateDevicesForNeighbor(side);
        }
        if (neighborConnectivityChanged) {
            busElement.scheduleScan();
            if (level instanceof ServerLevel) {
                EnergyNetworkCache.invalidate();
            }
        }
    }

    @Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return modelData.getModelData();
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(INTERFACE_NAMES_TAG_NAME, (ListTag) interfaceNameManager.serialize());
        tag.put(FACADE_TAG_NAME, facadeManager.serialize());
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        interfaceNameManager.deserialize(
                tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        facadeManager.deserialize(tag.getCompound(FACADE_TAG_NAME));
        // Model data is built from the facade; a client that starts tracking must
        // rebuild it, otherwise the cable renders without the facade.
        requestModelDataUpdate();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save(registries));
        faceOverrides.save(tag, FACE_OVERRIDES_TAG_NAME);
        tag.put(INTERFACE_NAMES_TAG_NAME, (ListTag) interfaceNameManager.serialize());
        tag.put(FACADE_TAG_NAME, facadeManager.serialize());
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        faceOverrides.load(tag, FACE_OVERRIDES_TAG_NAME);
        interfaceNameManager.deserialize(
                tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        facadeManager.deserialize(tag.getCompound(FACADE_TAG_NAME));
        requestModelDataUpdate();
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        assert level != null;
        EnergyNetworkCache.invalidate();
        final ServerLevel serverLevel = (ServerLevel) level;
        for (final var side : Direction.values()) {
            // NOPMD: listener is tied to the loop's side and registered per-neighbor position
            final var listener = new NeighborListener(serverLevel, busElement, side); // NOPMD allocation depends on loop iteration / per-item state
            neighborListeners[side.get3DDataValue()] = listener;
            serverLevel.registerCapabilityListener(getBlockPos().relative(side), listener);
        }
        busElement.scheduleLateLoad();
        requestModelDataUpdate();
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);
        EnergyNetworkCache.invalidate();
        if (isRemove) busElement.setRemoved();
    }
}