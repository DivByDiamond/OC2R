package li.cil.oc2.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/** {@link GameHooks} backed by the NeoForge patches and events. */
public final class NeoForgeGameHooks implements GameHooks {
    @Override
    public SoundType getSoundType(
            final BlockState state, final LevelReader level, final BlockPos pos, @Nullable final Entity entity) {
        return state.getSoundType(level, pos, entity);
    }

    @Override
    public BlockState rotate(
            final BlockState state, final LevelAccessor level, final BlockPos pos, final Rotation rotation) {
        return state.rotate(level, pos, rotation);
    }

    @Override
    public CompoundTag getPersistentData(final Entity entity) {
        return entity.getPersistentData();
    }

    @Override
    public boolean mayBreakBlock(
            final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        return !CommonHooks.fireBlockBreak(level, GameType.DEFAULT_MODE, player, pos, state).isCanceled();
    }

    @Override
    public boolean canHarvest(
            final ServerPlayer player, final BlockState state, final ServerLevel level, final BlockPos pos) {
        return EventHooks.doPlayerHarvestCheck(player, state, level, pos);
    }

    @Override
    public ServerPlayer getFakePlayer(final ServerLevel level, final GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    @Override
    public ItemStack getCraftingRemainder(final ItemStack stack) {
        return stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
    }

    @Override
    public boolean canFitInsideContainerItems(final ItemStack stack) {
        return stack.canFitInsideContainerItems();
    }

    @Override
    public boolean destroyBlockAsPlayer(
            final ServerLevel level,
            final BlockPos pos,
            final BlockState state,
            final ServerPlayer player,
            final boolean willHarvest) {
        return state.onDestroyedByPlayer(level, pos, player, willHarvest, level.getFluidState(pos));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }
}
