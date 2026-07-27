package li.cil.oc2.common.block.cable;

import static li.cil.oc2.common.block.BusCableStateProperties.*;

import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import li.cil.oc2.common.block.types.ConnectionType;

final class BusCableItemHelper {
    static void addExtraDrops(
            final List<ItemStack> drops, final BlockState state, final LootParams.Builder builder) {
        if (state.getValue(HAS_FACADE)) {
            final BlockEntity blockEntity =
                    builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
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
            final ConnectionType connectionType =
                    state.getValue(FACING_TO_CONNECTION_MAP.get(side));
            if (connectionType == ConnectionType.INTERFACE) {
                interfaceCount++;
            }
        }
        if (interfaceCount > 0) {
            drops.add(new ItemStack(Items.BUS_INTERFACE.get(), interfaceCount));
        }
    }

    @Nullable
    static ItemStack getPickBlock(
            final BlockState state,
            final HitResult hit,
            final LevelReader level,
            final BlockPos pos,
            final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCable)) {
            return null;
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
        return null;
    }

    private BusCableItemHelper() {}
}
