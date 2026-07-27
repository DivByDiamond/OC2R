package li.cil.oc2.common.blockentity.network;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.world.LevelUtils;
import li.cil.oc2.common.util.nbt.NBTTagIds;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class BusCableBlockEntity extends ModBlockEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";
    private static final String FACADE_TAG_NAME = "facade";

    final AbstractBlockDeviceBusElement busElement = new BusCableBusElement(this);
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

    public String getInterfaceName(final Direction side) {
        return interfaceNameManager.getInterfaceName(side);
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
            setInterfaceName(side, "");
            if (level != null) level.invalidateCapabilities(getBlockPos());
        }
        if (neighborConnectivityChanged) busElement.scheduleScan();
    }

    @Override
    public ModelData getModelData() {
        return modelData.getModelData();
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.put(INTERFACE_NAMES_TAG_NAME, (ListTag) interfaceNameManager.serialize());
        if (facadeManager.getFacade().equals(ItemStack.EMPTY)) {
            tag.put(FACADE_TAG_NAME, new CompoundTag());
        } else {
            tag.put(
                    FACADE_TAG_NAME,
                    ItemStack.CODEC
                            .encodeStart(NbtOps.INSTANCE, facadeManager.getFacade())
                            .getOrThrow());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        interfaceNameManager.deserialize(
                tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        final var facadeNbt = tag.getCompound(FACADE_TAG_NAME);
        if (!facadeNbt.isEmpty()) {
            facadeManager.setFacadeDirectly(
                    ItemStack.CODEC.parse(NbtOps.INSTANCE, facadeNbt).getOrThrow());
        } else {
            facadeManager.setFacadeDirectly(ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save(registries));
        tag.put(INTERFACE_NAMES_TAG_NAME, (ListTag) interfaceNameManager.serialize());
        tag.put(
                FACADE_TAG_NAME,
                ItemStack.OPTIONAL_CODEC
                        .encodeStart(NbtOps.INSTANCE, facadeManager.getFacade())
                        .getOrThrow());
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        interfaceNameManager.deserialize(
                tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        final var facadeNbt = tag.getCompound(FACADE_TAG_NAME);
        try {
            facadeManager.setFacadeDirectly(
                    ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, facadeNbt).getOrThrow());
        } catch (final IllegalStateException e) {
            facadeManager.setFacadeDirectly(ItemStack.EMPTY);
        }
        requestModelDataUpdate();
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        assert level != null;
        final ServerLevel serverLevel = (ServerLevel) level;
        for (final var side : Direction.values()) {
            final var listener = new NeighborListener(serverLevel, busElement, side);
            neighborListeners[side.ordinal()] = listener;
            serverLevel.registerCapabilityListener(getBlockPos().relative(side), listener);
        }
        scheduleLateLoad();
        requestModelDataUpdate();
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);
        if (isRemove) busElement.setRemoved();
    }

    private void scheduleLateLoad() {
        assert level != null;
        ServerScheduler.schedule(
                level,
                () -> {
                    if (!isValid()) return;
                    final var world = requireNonNull(getLevel());
                    final var pos = getBlockPos();
                    for (final var direction : Constants.DIRECTIONS) {
                        busElement.updateDevicesForNeighbor(direction);
                        final var neighborPos = pos.relative(direction);
                        final var blockEntity =
                                LevelUtils.getBlockEntityIfChunkExists(world, neighborPos);
                        if (blockEntity == null) continue;
                        final var capability =
                                world.getCapability(
                                        Capabilities.DeviceBusElement.BLOCK,
                                        neighborPos,
                                        null,
                                        blockEntity,
                                        direction.getOpposite());
                        if (capability != null) capability.scheduleScan();
                    }
                });
    }
}