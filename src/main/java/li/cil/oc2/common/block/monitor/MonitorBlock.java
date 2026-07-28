package li.cil.oc2.common.block.monitor;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.block.energy.EnergyConsumingBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.MonitorPowerMessageForwarded;
import li.cil.oc2.common.util.block.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class MonitorBlock extends HorizontalDirectionalBlock
        implements EnergyConsumingBlock, EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    // We bake the "screen" indent on the front into the collision shape, to prevent stuff being
    // placeable on that side, such as network connectors, torches, etc.
    private static final VoxelShape NEG_Z_SHAPE =
            Shapes.or(
                    Block.box(0, 0, 1, 16, 16, 16), // main body
                    Block.box(0, 15, 0, 16, 16, 1), // across top
                    Block.box(0, 0, 0, 16, 4, 1), // across bottom
                    Block.box(0, 0, 0, 2, 16, 1), // up left
                    Block.box(14, 0, 0, 16, 16, 1) // up right
                    );
    private static final VoxelShape NEG_X_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    public MonitorBlock() {
        super(
                Properties.of()
                        .mapColor(MapColor.METAL)
                        .sound(SoundType.METAL)
                        .lightLevel(state -> state.getValue(LIT) ? 8 : 0)
                        .strength(1.5f, 6.0f));
        registerDefaultState(
                getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<MonitorBlock> codec() {
        return BlockCodecs.MONITOR.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag advanced) {
        super.appendHoverText(stack, context, tooltip, advanced);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NEG_Z_SHAPE;
            case SOUTH -> POS_Z_SHAPE;
            case WEST -> NEG_X_SHAPE;
            default -> POS_X_SHAPE;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hitResult) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final MonitorBlockEntity monitor)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                monitor.start();
                NetworkMessages.sendToClientsTrackingBlockEntity(
                        new MonitorPowerMessageForwarded(monitor, true), monitor);
            } else if (player instanceof final ServerPlayer serverPlayer) {
                monitor.openTerminalScreen(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.MONITOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createTicker(level, type, BlockEntities.MONITOR.get());
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, LIT);
    }

    @Override
    public int getEnergyConsumption() {
        return Config.monitorEnergyPerTick;
    }
}