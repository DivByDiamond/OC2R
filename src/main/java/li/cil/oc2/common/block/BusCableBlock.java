package li.cil.oc2.common.block;

import static li.cil.oc2.common.block.BusCableStateProperties.*;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BusCableBlock extends BaseEntityBlock {
    private final VoxelShape[] shapes;

    public BusCableBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        BlockState defaultState = getStateDefinition().any();
        for (final EnumProperty<ConnectionType> property : FACING_TO_CONNECTION_MAP.values()) {
            defaultState = defaultState.setValue(property, ConnectionType.NONE);
        }
        registerDefaultState(defaultState.setValue(HAS_CABLE, true).setValue(HAS_FACADE, false));
        shapes = BusCableShapeBuilder.makeShapes();
    }

    @Override
    protected MapCodec<BusCableBlock> codec() {
        return BlockCodecs.BUS_CABLE.get();
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final LootParams.Builder builder) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        BusCableInteractionHandler.addExtraDrops(drops, state, builder);
        return drops;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        final Level level = context.getLevel();
        final BlockPos position = context.getClickedPos();
        for (final Map.Entry<Direction, EnumProperty<ConnectionType>> entry :
                FACING_TO_CONNECTION_MAP.entrySet()) {
            final Direction facing = entry.getKey();
            final BlockPos facingPos = position.relative(facing);
            if (context.getItemInHand().getItem().equals(Items.BUS_CABLE.get())
                    && canHaveCableTo(level.getBlockState(facingPos), facing.getOpposite())) {
                state = state.setValue(entry.getValue(), ConnectionType.CABLE);
            }
        }
        return state;
    }

    @Override
    public BlockState updateShape(
            final BlockState state,
            final Direction facing,
            final BlockState facingState,
            final LevelAccessor level,
            final BlockPos currentPos,
            final BlockPos facingPos) {
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(facing);
        if (state.getValue(property) == ConnectionType.INTERFACE) {
            return state;
        }
        final boolean neighborChanged;
        BlockState result = state;
        if (result.getValue(HAS_CABLE) && canHaveCableTo(facingState, facing.getOpposite())) {
            neighborChanged = result.getValue(property) != ConnectionType.CABLE;
            result = result.setValue(property, ConnectionType.CABLE);
        } else {
            neighborChanged = result.getValue(property) != ConnectionType.NONE;
            result = result.setValue(property, ConnectionType.NONE);
        }
        onConnectionTypeChanged(level, currentPos, facing, neighborChanged);
        return result;
    }

    @Override
    public VoxelShape getShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        if (state.getValue(HAS_FACADE)) {
            return Shapes.block();
        }
        return shapes[BusCableShapeBuilder.getShapeIndex(state)];
    }

    @Override
    protected ItemInteractionResult useItemOn(
            final ItemStack heldItem,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        final ItemInteractionResult result =
                BusCableInteractionHandler.handleUseItemOn(
                        heldItem, state, level, pos, player, hand, hitResult);
        return result != null
                ? result
                : super.useItemOn(heldItem, state, level, pos, player, hand, hitResult);
    }

    @Override
    public ItemStack getCloneItemStack(
            final BlockState state,
            final HitResult hit,
            final LevelReader level,
            final BlockPos pos,
            final Player player) {
        final ItemStack result =
                BusCableInteractionHandler.getPickBlock(state, hit, level, pos, player);
        return result != null ? result : super.getCloneItemStack(state, hit, level, pos, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.BUS_CABLE.get().create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        FACING_TO_CONNECTION_MAP.values().forEach(builder::add);
        builder.add(HAS_CABLE, HAS_FACADE);
    }
}