package li.cil.oc2.common.block.media;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.media.MediaStationBlockEntity;
import li.cil.oc2.common.firmware.FlashWriterService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <26.1 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

/** Upper bay accepts floppies; lower bay programs flash memory or HDDs. */
public final class MediaStationBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public MediaStationBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
                .strength(1.5f, 6.0f).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<MediaStationBlock> codec() { return BlockCodecs.MEDIA_STATION.get(); }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.MEDIA_STATION.get().create(pos, state);
    }

    @Override
    //? if >=26.1 {
    /*protected InteractionResult useItemOn(
    *///?} else {
    protected ItemInteractionResult useItemOn(
    //?}
            final ItemStack stack, final BlockState state, final Level level, final BlockPos pos,
            final Player player, final InteractionHand hand, final BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof MediaStationBlockEntity station)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        final boolean floppy = station.canInsertFloppy(stack);
        if (!floppy && !station.canInsert(stack)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!level.isClientSide()) player.setItemInHand(hand,
                floppy ? station.insertFloppy(stack) : station.insert(stack, player));
        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
        //?}
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                                final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MediaStationBlockEntity station) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                if (!player.isShiftKeyDown()) FlashWriterService.open(serverPlayer, station);
                else if (hit.getLocation().y - pos.getY() >= .5) station.ejectFloppy(player);
                else station.eject(player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
