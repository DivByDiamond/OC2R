/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity.network;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;

import li.cil.oc2.api.API;
import li.cil.oc2.client.model.BusCableBakedModel;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.ConnectionType;
import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusCableFacadeMessage;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.client.model.BusCableBakedModel.*;

@EventBusSubscriber(modid = API.MOD_ID)
public final class BusCableBlockEntity extends ModBlockEntity {
    public enum FacadeType {
        NOT_A_BLOCK,
        INVALID_BLOCK,
        VALID_BLOCK,
    }

    private ModelData currentModelData = ModelData.EMPTY;

    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";
    private static final String FACADE_TAG_NAME = "facade";

    ///////////////////////////////////////////////////////////////////

    private final AbstractBlockDeviceBusElement busElement = new BusCableBusElement();
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private final ICapabilityInvalidationListener[] neighborListeners = new NeighborListener[Constants.BLOCK_FACE_COUNT];
    private ItemStack facade = ItemStack.EMPTY;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.BUS_CABLE.get(), pos, state);

        requestModelDataUpdate();
    }

    ///////////////////////////////////////////////////////////////////

    public String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    public void setInterfaceName(final Direction side, final String name) {
        if (level == null) {
            return;
        }

        final String validatedName = validateName(name);
        if (Objects.equals(validatedName, interfaceNames[side.get3DDataValue()])) {
            return;
        }

        interfaceNames[side.get3DDataValue()] = validatedName;
        setChanged();

        if (!level.isClientSide()) {
            final BusInterfaceNameMessage message = BusInterfaceNameMessage.ToClient(this, side, interfaceNames[side.get3DDataValue()]);
            Network.sendToClientsTrackingBlockEntity(message, this);
            busElement.updateDevicesForNeighbor(side);
        }
    }

    public FacadeType getFacadeType(final ItemStack stack) {
        return getFacadeType(ItemStackUtils.getBlockState(stack));
    }

    public FacadeType getFacadeType(@Nullable final BlockState state) {
        if (state == null) {
            return FacadeType.NOT_A_BLOCK;
        }

        if (level == null ||
            state.getRenderShape() != RenderShape.MODEL ||
            !state.isSolidRender(level, getBlockPos()) ||
            state.getBlock() instanceof EntityBlock) {
            return FacadeType.INVALID_BLOCK;
        }

        return FacadeType.VALID_BLOCK;
    }

    public ItemStack getFacade() {
        return facade;
    }

    public void setFacade(ItemStack stack) {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(stack);
        if (getFacadeType(facadeState) != FacadeType.VALID_BLOCK) {
            stack = ItemStack.EMPTY;
        }

        if (ItemStack.isSameItem(stack, facade)) {
            return;
        }

        facade = stack.copy();
        facade.setCount(1);
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, true);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }

        requestModelDataUpdate();
    }

    public void removeFacade() {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(facade);
        facade = ItemStack.EMPTY;
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, false);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }

        requestModelDataUpdate();
    }

    public void handleConfigurationChanged(@Nullable final Direction side, final boolean neighborConnectivityChanged) {
        if (side != null) {
            // Whenever the type changes we can clear it. Technically only needed
            // for the interface->none transition, but all others are no-ops, so
            // we can just do this.
            setInterfaceName(side, "");

            if (level != null)
                level.invalidateCapabilities(getBlockPos());
        }

        if (neighborConnectivityChanged) {
            busElement.scheduleScan();
        }
    }

    @Override
    public ModelData getModelData()
    {
        if (level == null) return ModelData.EMPTY;
        BlockState state = getBlockState();
        BlockPos pos = getBlockPos();
        if (state.hasProperty(BusCableBlock.HAS_FACADE) && state.getValue(BusCableBlock.HAS_FACADE)) {
            BlockState facadeState;
            final ItemStack facadeItem = getFacade();

            facadeState = ItemStackUtils.getBlockState(facadeItem);
            if (facadeState == null) {
                facadeState = Blocks.IRON_BLOCK.defaultBlockState();
            }

            final BlockModelShaper shapes = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
            final BakedModel model = shapes.getBlockModel(facadeState);
            ModelData data = model.getModelData(level, pos, facadeState, currentModelData);

            currentModelData = ModelData.builder()
                .with(BUS_CABLE_FACADE_PROPERTY, new BusCableBakedModel.BusCableFacade(facadeState, model, data))
                .build();

            return currentModelData;
        }

        Direction supportSide = null;
        for (final Direction direction : Constants.DIRECTIONS) {
            if (isNeighborInDirectionSolid(level, pos, direction)) {
                final EnumProperty<ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
                if (state.hasProperty(property) && state.getValue(property) == ConnectionType.INTERFACE) {
                    return currentModelData; // Plug is already supporting us, bail.
                }

                if (supportSide == null) { // Prefer vertical supports.
                    supportSide = direction;
                }
            }
        }

        if (supportSide != null) {
            currentModelData = ModelData.builder()
                .with(BUS_CABLE_SUPPORT_PROPERTY, new BusCableBakedModel.BusCableSupportSide(supportSide))
                .build();
            return currentModelData;
        }

        return currentModelData;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        if (facade == ItemStack.EMPTY) {
            tag.put(FACADE_TAG_NAME, new CompoundTag());
        } else {
            var facade_nbt = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, facade);
            tag.put(FACADE_TAG_NAME, facade_nbt.getOrThrow());
        }

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));

        var facade_nbt = tag.getCompound(FACADE_TAG_NAME);
        if (!facade_nbt.isEmpty()) {
            var facade_parsed = ItemStack.CODEC.parse(NbtOps.INSTANCE, facade_nbt);
            facade = facade_parsed.getOrThrow();
        } else {
            facade = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save(registries));
        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        var facade_nbt = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, facade);
        tag.put(FACADE_TAG_NAME, facade_nbt.getOrThrow());
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        busElement.loadAdditional(tag.getCompound(BUS_ELEMENT_TAG_NAME), registries);
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        var facade_nbt = tag.getCompound(FACADE_TAG_NAME);
        try {
            facade = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, facade_nbt).getOrThrow();
        } catch (IllegalStateException e) {
            // It was ok for older minecraft versions to serialize ItemStack.EMPTY literally
            // Newer versions throw an error if they see a minecraft:air serialized
            facade = ItemStack.EMPTY;
        }

        requestModelDataUpdate();
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
            Capabilities.DeviceBusElement.BLOCK,
            (level, pos, state, be, side) -> {
                if (be instanceof final BusCableBlockEntity self) {
                    if (BusCableBlock.getConnectionType(be.getBlockState(), side) != ConnectionType.NONE) {
                        return self.busElement;
                    }
                }
                return null;
            },
            li.cil.oc2.common.block.Blocks.BUS_CABLE.get()
        );
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        assert level != null;
        ServerLevel level = (ServerLevel) this.level;

        for (var side : Direction.values()) {
            var listener = new NeighborListener(level, busElement, side);
            // We need to hold a reference to these listeners, as Neoforge will only maintain a weak reference
            neighborListeners[side.ordinal()] = listener;
            level.registerCapabilityListener(
                getBlockPos().relative(side),
                listener
            );
        }

        scheduleLateLoad();

        requestModelDataUpdate();
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            busElement.setRemoved();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private ListTag serializeInterfaceNames() {
        final ListTag tag = new ListTag();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            tag.add(StringTag.valueOf(getInterfaceName(Direction.from3DDataValue(i))));
        }
        return tag;
    }

    private void deserializeInterfaceNames(final ListTag tag) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final String name = tag.getString(i).trim();
            interfaceNames[i] = name.substring(0, Math.min(32, name.length()));
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private void scheduleLateLoad() {
        // This is called from onLoad, so we cannot access neighbors yet.
        assert level != null;
        ServerScheduler.schedule(level, () -> {
            if (!isValid()) {
                return;
            }

            final Level level = requireNonNull(getLevel());
            final BlockPos pos = getBlockPos();
            for (final Direction direction : Constants.DIRECTIONS) {
                busElement.updateDevicesForNeighbor(direction);

                final BlockPos neighborPos = pos.relative(direction);
                final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, neighborPos);
                if (blockEntity == null) {
                    continue;
                }

                final var capability = level.getCapability(Capabilities.DeviceBusElement.BLOCK, neighborPos, null, blockEntity, direction.getOpposite());
                if (capability != null) {
                    capability.scheduleScan();
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusCableBusElement extends AbstractBlockDeviceBusElement {
        @Nullable
        @Override
        public Level getLevel() {
            return BusCableBlockEntity.this.getLevel();
        }

        @Override
        public BlockPos getPosition() {
            return getBlockPos();
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            final ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == ConnectionType.CABLE ||
                connectionType == ConnectionType.INTERFACE;
        }

        @Override
        public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
            final ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == ConnectionType.INTERFACE;
        }

        @Override
        protected void collectSyntheticDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
            super.collectSyntheticDevices(level, pos, side, entries);

            if (side == null || entries.isEmpty()) {
                return;
            }

            final String interfaceName = interfaceNames[side.get3DDataValue()];
            if (!StringUtil.isNullOrEmpty(interfaceName)) {
                entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)), side));
            }
        }

        @Override
        public double getEnergyConsumption() {
            return super.getEnergyConsumption()
                + Config.busCableEnergyPerTick
                + BusCableBlock.getInterfaceCount(getBlockState()) * Config.busInterfaceEnergyPerTick;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class NeighborListener implements ICapabilityInvalidationListener {
        ServerLevel level;
        AbstractBlockDeviceBusElement busElement;
        Direction side;

        public NeighborListener(ServerLevel level, AbstractBlockDeviceBusElement busElement, Direction side) {
            this.level = level;
            this.busElement = busElement;
            this.side = side;
        }

        @Override
        public boolean onInvalidate() {
            // We can't touch the level during an invalidate, so schedule it
            ServerScheduler.schedule(level, () -> {
                busElement.updateDevicesForNeighbor(side);
            });
            return true;
        }
    }
}
