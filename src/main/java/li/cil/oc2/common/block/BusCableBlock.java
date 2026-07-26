/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import li.cil.oc2.client.gui.screen.BusInterfaceScreen;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusCableBlock extends BaseEntityBlock {
    ///////////////////////////////////////////////////////////////////

    public static final BooleanProperty HAS_CABLE = BooleanProperty.create("has_cable");
    public static final BooleanProperty HAS_FACADE = BooleanProperty.create("has_facade");
    public static final EnumProperty<ConnectionType> CONNECTION_NORTH = EnumProperty.create("connection_north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_EAST = EnumProperty.create("connection_east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_SOUTH = EnumProperty.create("connection_south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_WEST = EnumProperty.create("connection_west", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_UP = EnumProperty.create("connection_up", ConnectionType.class);
    public static final EnumProperty<ConnectionType> CONNECTION_DOWN = EnumProperty.create("connection_down", ConnectionType.class);

    public static final Map<Direction, EnumProperty<ConnectionType>> FACING_TO_CONNECTION_MAP = Util.make(Maps.newEnumMap(Direction.class), directions -> {
        directions.put(Direction.NORTH, CONNECTION_NORTH);
        directions.put(Direction.EAST, CONNECTION_EAST);
        directions.put(Direction.SOUTH, CONNECTION_SOUTH);
        directions.put(Direction.WEST, CONNECTION_WEST);
        directions.put(Direction.UP, CONNECTION_UP);
        directions.put(Direction.DOWN, CONNECTION_DOWN);
    });

    public static ConnectionType getConnectionType(final BlockState state, @Nullable final Direction direction) {
        if (direction != null) {
            return state.getValue(BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction));
        } else {
            return ConnectionType.NONE;
        }
    }

    public static int getInterfaceCount(final BlockState state) {
        int partCount = 0;
        for (final EnumProperty<ConnectionType> connectionType : FACING_TO_CONNECTION_MAP.values()) {
            if (state.getValue(connectionType) == ConnectionType.INTERFACE) {
                partCount++;
            }
        }
        return partCount;
    }

    public static Direction getHitSide(final BlockPos pos, final BlockHitResult hit) {
        final Vec3 localHitPos = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        return Direction.getNearest(localHitPos.x, localHitPos.y, localHitPos.z);
    }

    ///////////////////////////////////////////////////////////////////

    private final VoxelShape[] shapes;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));

        BlockState defaultState = getStateDefinition().any();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.setValue(property, ConnectionType.NONE);
        }
        defaultState = defaultState.setValue(HAS_CABLE, true);
        defaultState = defaultState.setValue(HAS_FACADE, false);
        registerDefaultState(defaultState);

        shapes = BusCableShapeBuilder.makeShapes();
    }

    @Override
    protected MapCodec<BusCableBlock> codec() {
        return BlockCodecs.BUS_CABLE.get();
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean addInterface(final Level level, final BlockPos pos, final BlockState state, final Direction side) {
        if (state.getBlock() != Blocks.BUS_CABLE.get()) {
            return false;
        }

        if (state.getValue(HAS_FACADE)) {
            return false;
        }

        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);
        if (state.getValue(property) != ConnectionType.NONE) {
            return false;
        }

        level.setBlock(pos, state.setValue(property, ConnectionType.INTERFACE), Block.UPDATE_ALL_IMMEDIATE);

        onConnectionTypeChanged(level, pos, side, false);

        return true;
    }

    public static boolean addCable(final Level level, final BlockPos pos, final BlockState state) {
        if (state.getBlock() != Blocks.BUS_CABLE.get()) {
            return false;
        }

        if (state.getValue(HAS_CABLE)) {
            return false;
        }

        level.setBlock(pos, state.setValue(HAS_CABLE, true), Block.UPDATE_ALL_IMMEDIATE);

        onConnectionTypeChanged(level, pos, null, false);

        return true;
    }

    public static void setHasFacade(final Level level, final BlockPos pos, final BlockState state, @Nullable final BlockState facadeState, final boolean value) {
        if (state.getValue(HAS_FACADE) == value) {
            return;
        }

        level.setBlock(pos, state.setValue(HAS_FACADE, value), Block.UPDATE_ALL_IMMEDIATE);

        final BlockState soundsSource = facadeState != null ? facadeState : state;
        LevelUtils.playSound(level, pos, soundsSource.getSoundType(), value ? SoundType::getPlaceSound : SoundType::getBreakSound);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldItem, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult) {
        if (heldItem.getItem() == Items.BUS_CABLE.get() ||
            heldItem.getItem() == Items.BUS_INTERFACE.get()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCableBlockEntity)) {
            return super.useItemOn(heldItem, state, level, pos, player, hand, hitResult);
        }

        if (Wrenches.isWrench(heldItem)) {
            if (player.isShiftKeyDown()) {
                final ItemStack facadeItem = busCableBlockEntity.getFacade();
                if (!facadeItem.isEmpty()) {
                    if (!level.isClientSide()) {
                        busCableBlockEntity.removeFacade();
                        if (!player.isCreative() && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                            ItemStackUtils.spawnAsEntity(level, pos, facadeItem, hitResult.getDirection()).ifPresent(entity -> {
                                entity.setNoPickUpDelay();
                                entity.playerTouch(player);
                            });
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    // NB: leave wrenching logic up to wrench when the to-be-removed interface is the last
                    //     part of this bus. This ensures we properly remove the block itself without having
                    //     to duplicate the logic needed for that.
                    if (getPartCount(state) > 1 && (tryRemoveInterface(state, level, pos, player, hitResult) || tryRemoveCable(state, level, pos, player))) {
                        return ItemInteractionResult.sidedSuccess(level.isClientSide());
                    }
                }
            } else {
                final Direction side = getHitSide(pos, hitResult);
                if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                    if (level.isClientSide()) {
                        openBusInterfaceScreen(busCableBlockEntity, side);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        } else if (!player.isShiftKeyDown() && !state.getValue(HAS_FACADE) && getInterfaceCount(state) == 0) {
            switch (busCableBlockEntity.getFacadeType(heldItem)) {
                case INVALID_BLOCK -> {
                    if (!level.isClientSide()) {
                        player.displayClientMessage(text("message.{mod}.invalid_facade_block"), true);
                    }

                    // Always return success (even on failure) to avoid accidentally placing blocks.
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
                case VALID_BLOCK -> {
                    if (!level.isClientSide()) {
                        busCableBlockEntity.setFacade(heldItem);
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        return super.useItemOn(heldItem, state, level, pos, player, hand, hitResult);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));

        if (state.getValue(HAS_FACADE)) {
            final BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (blockEntity instanceof final BusCableBlockEntity busCable) {
                final ItemStack stack = busCable.getFacade();
                if (!stack.isEmpty()) {
                    drops.add(stack);
                }
            }
        }

        if (state.getValue(HAS_CABLE)) {
            drops.add(new ItemStack(Items.BUS_CABLE.get()));
        }

        int interfaceCount = 0;
        for (final Direction side : Constants.DIRECTIONS) {
            final ConnectionType connectionType = state.getValue(FACING_TO_CONNECTION_MAP.get(side));
            if (connectionType == ConnectionType.INTERFACE) {
                interfaceCount++;
            }
        }

        if (interfaceCount > 0) {
            drops.add(new ItemStack(Items.BUS_INTERFACE.get(), interfaceCount));
        }

        return drops;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = defaultBlockState();

        final Level level = context.getLevel();
        final BlockPos position = context.getClickedPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry : FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.relative(facing);
            if (context.getItemInHand().getItem() == Items.BUS_CABLE.get() &&
                canHaveCableTo(level.getBlockState(facingPos), facing.getOpposite())) {
                state = state.setValue(entry.getValue(), ConnectionType.CABLE);
            }
        }

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, final Direction facing, final BlockState facingState, final LevelAccessor level, final BlockPos currentPos, final BlockPos facingPos) {
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(facing);
        if (state.getValue(property) == ConnectionType.INTERFACE) {
            return state;
        }

        final boolean neighborConnectionChanged;
        if (state.getValue(HAS_CABLE) && canHaveCableTo(facingState, facing.getOpposite())) {
            neighborConnectionChanged = state.getValue(property) != ConnectionType.CABLE;
            state = state.setValue(property, ConnectionType.CABLE);
        } else {
            neighborConnectionChanged = state.getValue(property) != ConnectionType.NONE;
            state = state.setValue(property, ConnectionType.NONE);
        }

        onConnectionTypeChanged(level, currentPos, facing, neighborConnectionChanged);

        return state;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        if (state.getValue(HAS_FACADE)) {
            return Shapes.block();
        }

        return shapes[BusCableShapeBuilder.getShapeIndex(state)];
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult hit, final LevelReader level, final BlockPos pos, final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) {
            return super.getCloneItemStack(state, hit, level, pos, player);
        }

        final ItemStack facadeItem = busCable.getFacade();
        if (!facadeItem.isEmpty()) {
            return facadeItem;
        }

        if (hit instanceof final BlockHitResult blockHit) {
            final Direction side = getHitSide(pos, blockHit);
            if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                return new ItemStack(Items.BUS_INTERFACE.get());
            }
        }

        return super.getCloneItemStack(state, hit, level, pos, player);
    }

    ///////////////////////////////////////////////////////////////////
    // BaseEntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.BUS_CABLE.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
        builder.add(HAS_CABLE);
        builder.add(HAS_FACADE);
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean canHaveCableTo(final BlockState state, final Direction side) {
        return state.getBlock() == Blocks.BUS_CABLE.get() &&
            state.getValue(HAS_CABLE) &&
            state.getValue(FACING_TO_CONNECTION_MAP.get(side)) != ConnectionType.INTERFACE;
    }

    private static int getPartCount(final BlockState state) {
        int partCount = getInterfaceCount(state);
        if (state.getValue(HAS_CABLE)) {
            partCount++;
        }
        return partCount;
    }

    private static boolean tryRemoveInterface(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final Direction side = getHitSide(pos, hit);
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);

        if (state.getValue(property) != ConnectionType.INTERFACE) {
            return false;
        }

        final BlockPos neighborPos = pos.relative(side);
        final boolean isReplacedByCable = state.getValue(HAS_CABLE) && canHaveCableTo(level.getBlockState(neighborPos), side.getOpposite());
        if (isReplacedByCable) {
            level.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.CABLE));
        } else {
            level.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.NONE));
        }

        handlePartRemoved(state, level, pos, side, player, new ItemStack(Items.BUS_INTERFACE.get()), isReplacedByCable);

        return true;
    }

    private static boolean tryRemoveCable(final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (!state.getValue(HAS_CABLE)) {
            return false;
        }

        level.setBlockAndUpdate(pos, state.setValue(HAS_CABLE, false));

        handlePartRemoved(state, level, pos, null, player, new ItemStack(Items.BUS_CABLE.get()), true);

        return true;
    }

    private static void handlePartRemoved(final BlockState state, final Level level, final BlockPos pos, @Nullable final Direction side, final Player player, final ItemStack drop, final boolean neighborConnectionChanged) {
        onConnectionTypeChanged(level, pos, side, neighborConnectionChanged);

        if (!player.isCreative() && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStackUtils.spawnAsEntity(level, pos, drop, side).ifPresent(entity -> {
                entity.setNoPickUpDelay();
                entity.playerTouch(player);
            });
        }

        LevelUtils.playSound(level, pos, state.getSoundType(), SoundType::getBreakSound);
    }

    private static void onConnectionTypeChanged(final LevelAccessor level, final BlockPos pos, @Nullable final Direction face, final boolean neighborConnectionChanged) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final BusCableBlockEntity busCable) {
            busCable.handleConfigurationChanged(face, neighborConnectionChanged);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void openBusInterfaceScreen(final BusCableBlockEntity blockEntity, final Direction side) {
        final BusInterfaceScreen screen = new BusInterfaceScreen(blockEntity, side);
        Minecraft.getInstance().setScreen(screen);
    }

}
