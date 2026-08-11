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
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorPowerMessageForwarded;
import li.cil.oc2.common.util.block.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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

    // ----- Multiblock state properties ------------------------------------------
    /** Total width of this multiblock, in blocks. Same value on every block of the multiblock. */
    public static final IntegerProperty WIDTH =
            IntegerProperty.create("mb_width", 1, MonitorMultiblock.MAX_WIDTH);
    /** Total height of this multiblock, in blocks. Same value on every block of the multiblock. */
    public static final IntegerProperty HEIGHT =
            IntegerProperty.create("mb_height", 1, MonitorMultiblock.MAX_HEIGHT);
    /** This block's column offset from the origin (0 = origin = top-right from viewer POV). */
    public static final IntegerProperty ORIGIN_OFFSET_X =
            IntegerProperty.create("mb_ox", 0, MonitorMultiblock.MAX_WIDTH - 1);
    /** This block's row offset from the origin (0 = origin = top row). */
    public static final IntegerProperty ORIGIN_OFFSET_Y =
            IntegerProperty.create("mb_oy", 0, MonitorMultiblock.MAX_HEIGHT - 1);

    /**
     * Re-entrancy guard for {@link #playerWillDestroy}. When we proactively remove the other
     * blocks of a multiblock we don't want those removals to recursively drop items or trigger
     * multiblock teardown again.
     */
    static final ThreadLocal<Boolean> IS_BREAKING_MULTIBLOCK = ThreadLocal.withInitial(() -> false);

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
                getStateDefinition()
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false)
                        .setValue(WIDTH, 1)
                        .setValue(HEIGHT, 1)
                        .setValue(ORIGIN_OFFSET_X, 0)
                        .setValue(ORIGIN_OFFSET_Y, 0));
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
        // Sub-blocks of a multiblock redirect interaction to the origin (master) block entity.
        final MonitorBlockEntity monitor;
        if (MonitorMultiblock.isOrigin(state)) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof final MonitorBlockEntity origin)) {
                return super.useWithoutItem(state, level, pos, player, hitResult);
            }
            monitor = origin;
        } else {
            monitor = MonitorMultiblock.getOriginEntity(level, pos, state);
            if (monitor == null) {
                return super.useWithoutItem(state, level, pos, player, hitResult);
            }
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

    @Override
    public void setPlacedBy(
            final Level level, final BlockPos pos, final BlockState state,
            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            // Attempt to merge the freshly placed 1x1 monitor into an adjacent multiblock.
            // tryMergeIntoMultiblock will re-stamp our BlockState (and possibly our neighbors')
            // with the new dimensions / offsets if a merge happens.
            MonitorMultiblock.tryMergeIntoMultiblock(level, pos, state.getValue(FACING));
        }
    }

    @Override
    public BlockState playerWillDestroy(
            final Level level, final BlockPos pos, final BlockState state, final Player player) {
        if (!IS_BREAKING_MULTIBLOCK.get() && !level.isClientSide()) {
            IS_BREAKING_MULTIBLOCK.set(true);
            try {
                // Drop W*H items at the broken position (the player's pick-up spot). We drop
                // W*H-1 here because the vanilla loot table will drop 1 more item for the
                // original block when destroyBlock() runs right after playerWillDestroy(). In
                // creative mode the vanilla drop is skipped, so we also skip our drop.
                final BlockPos originPos = MonitorMultiblock.isOrigin(state)
                        ? pos : MonitorMultiblock.getOriginPos(pos, state);
                final BlockState originState = level.getBlockState(originPos);
                if (originState.getBlock() instanceof MonitorBlock) {
                    final int W = originState.getValue(WIDTH);
                    final int H = originState.getValue(HEIGHT);
                    if (!player.isCreative() && (W > 1 || H > 1)) {
                        Block.popResource(
                                level, pos,
                                new ItemStack(li.cil.oc2.common.item.Items.MONITOR.get(), W * H - 1));
                    }
                    // Remove every other block of the multiblock silently. removeBlock does
                    // not call getDrops, so no items are dropped for them.
                    final Direction facing = originState.getValue(FACING);
                    for (int ox = 0; ox < W; ox++) {
                        for (int oy = 0; oy < H; oy++) {
                            final BlockPos blockPos = MonitorMultiblock.getBlockPos(originPos, facing, ox, oy);
                            if (blockPos.equals(pos)) continue;
                            final BlockEntity be = level.getBlockEntity(blockPos);
                            if (be != null) be.setRemoved();
                            level.removeBlock(blockPos, false);
                        }
                    }
                }
            } finally {
                IS_BREAKING_MULTIBLOCK.set(false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<ItemStack> getDrops(final BlockState state, final net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        // During multiblock teardown we already popped W*H-1 items in playerWillDestroy; the
        // vanilla loot table drop for the originally broken block supplies the final item, so
        // we just defer to the standard loot table here. The IS_BREAKING_MULTIBLOCK flag is
        // only used to prevent recursive teardown if removeBlock() somehow re-enters this
        // path (it shouldn't, but we keep the guard for safety).
        return super.getDrops(state, builder);
    }

    @Override
    public BlockState rotate(final BlockState state, final net.minecraft.world.level.LevelAccessor level, final BlockPos pos, final net.minecraft.world.level.block.Rotation rotation) {
        // Rotating a single block of a multiblock with a wrench would change its FACING and
        // break the multiblock structure. Refuse to rotate when this block is part of a
        // multiblock larger than 1x1.
        if (state.getValue(WIDTH) > 1 || state.getValue(HEIGHT) > 1) {
            return state;
        }
        return super.rotate(state, level, pos, rotation);
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
        builder.add(FACING, LIT, WIDTH, HEIGHT, ORIGIN_OFFSET_X, ORIGIN_OFFSET_Y);
    }

    @Override
    public int getEnergyConsumption() {
        return Config.monitorEnergyPerTick;
    }
}
