package li.cil.oc2.platform;

import com.mojang.authlib.GameProfile;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.fabric.PersistentDataHolder;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** {@link GameHooks} for Fabric, built on Fabric API events and the mixins of this module. */
public final class FabricGameHooks implements GameHooks {
    @Override
    public SoundType getSoundType(
            final BlockState state, final LevelReader level, final BlockPos pos, @Nullable final Entity entity) {
        // Vanilla sound types do not depend on the position or the acting entity.
        return state.getSoundType();
    }

    @Override
    public BlockState rotate(
            final BlockState state, final LevelAccessor level, final BlockPos pos, final Rotation rotation) {
        if (state.getBlock() instanceof MonitorBlock) {
            return MonitorBlock.rotateWithWrench(state, rotation);
        }
        return state.rotate(rotation);
    }

    @Override
    public CompoundTag getPersistentData(final Entity entity) {
        return ((PersistentDataHolder) entity).oc2r$getPersistentData();
    }

    @Override
    public boolean mayBreakBlock(
            final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        return PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }

    @Override
    public boolean canHarvest(
            final ServerPlayer player, final BlockState state, final ServerLevel level, final BlockPos pos) {
        return player.hasCorrectToolForDrops(state);
    }

    @Override
    public ServerPlayer getFakePlayer(final ServerLevel level, final GameProfile profile) {
        return FakePlayer.get(level, profile);
    }

    @Override
    public ItemStack getCraftingRemainder(final ItemStack stack) {
        return stack.getRecipeRemainder();
    }

    @Override
    public boolean canFitInsideContainerItems(final ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public boolean destroyBlockAsPlayer(
            final ServerLevel level,
            final BlockPos pos,
            final BlockState state,
            final ServerPlayer player,
            final boolean willHarvest) {
        // Same as the NeoForge default: on the server vanilla removes the block without drops or
        // particles, the caller handles those.
        return level.removeBlock(pos, false);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }
}
