package li.cil.oc2.common.bus.device.rpc.item.util;

import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.misc.FakePlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;

public class BlockHarvestHelper {
    public static boolean tryHarvestBlock(
            final ServerLevel level,
            final BlockPos blockPos,
            final Entity entity,
            final ItemStack identity) {
        final BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        }

        final ServerPlayer player = FakePlayerUtils.getFakePlayer(level, entity);
        if (!isHarvestAllowed(level, player, blockPos, blockState)) {
            return false;
        }

        var damage = identity.getDamageValue();
        if (damage >= identity.getMaxDamage()) {
            return false;
        }
        damage += 1;
        identity.setDamageValue(damage);

        if (!blockState.onDestroyedByPlayer(
                level, blockPos, player, true, level.getFluidState(blockPos))) {
            return false;
        }

        final BlockEntity blockEntity = level.getBlockEntity(blockPos);
        final Block block = blockState.getBlock();
        block.destroy(level, blockPos, blockState);
        block.playerDestroy(level, player, blockPos, blockState, blockEntity, ItemStack.EMPTY);

        return true;
    }

    private static boolean isHarvestAllowed(
            final ServerLevel level,
            final ServerPlayer player,
            final BlockPos blockPos,
            final BlockState blockState) {
        final var breakEvent =
                CommonHooks.fireBlockBreak(
                        level, GameType.DEFAULT_MODE, player, blockPos, blockState);
        if (breakEvent.isCanceled()) {
            return false;
        }

        final Block block = blockState.getBlock();
        final boolean isCommandBlock =
                block instanceof CommandBlock
                        || block instanceof StructureBlock
                        || block instanceof JigsawBlock;
        if (isCommandBlock && !player.canUseGameMasterBlocks()) {
            return false;
        }

        if (player.blockActionRestricted(level, blockPos, GameType.DEFAULT_MODE)) {
            return false;
        }

        final Tier toolTier = getToolTier();
        if (toolTier == null || blockState.is(toolTier.getIncorrectBlocksForDrops())) {
            return false;
        }

        return EventHooks.doPlayerHarvestCheck(player, blockState, level, blockPos);
    }

    private static Tier getToolTier() {
        try {
            return Tiers.valueOf(Config.blockOperationsModuleToolTier);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}