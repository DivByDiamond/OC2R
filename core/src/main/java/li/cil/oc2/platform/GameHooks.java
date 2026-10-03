package li.cil.oc2.platform;

import com.mojang.authlib.GameProfile;
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

/**
 * Loader-independent access to game behaviour that NeoForge patches into vanilla (or adds around it)
 * and that Fabric has to provide differently: context-aware sound types, wrench rotation, persistent
 * entity data, block break permission checks, fake players and creative tab builders. A stateless
 * singleton discovered through {@link java.util.ServiceLoader} via {@link Platform#hooks()}.
 */
public interface GameHooks {
    /** The sound type of {@code state}, letting blocks adapt it to the position and acting entity. */
    SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity);

    /** Rotates {@code state} in the world, the way a wrench does (blocks may refuse). */
    BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation rotation);

    /** NBT data attached to {@code entity} that survives saving and loading it (and respawning, for players). */
    CompoundTag getPersistentData(Entity entity);

    /** Whether the loader and other mods allow {@code player} to break the block at {@code pos}. */
    boolean mayBreakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);

    /** Whether {@code player} counts as able to harvest {@code state} (correct tool and mod overrides). */
    boolean canHarvest(ServerPlayer player, BlockState state, ServerLevel level, BlockPos pos);

    /** The shared fake player for {@code profile} in {@code level}. */
    ServerPlayer getFakePlayer(ServerLevel level, GameProfile profile);

    /** The item left behind when {@code stack} is used as a crafting ingredient, or empty if there is none. */
    ItemStack getCraftingRemainder(ItemStack stack);

    /** Whether {@code stack} may be put into container items such as shulker boxes. */
    boolean canFitInsideContainerItems(ItemStack stack);

    /**
     * Removes the block at {@code pos} on behalf of {@code player}, giving the block and other mods the chance
     * to react. Returns whether the block was removed.
     */
    boolean destroyBlockAsPlayer(
            ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, boolean willHarvest);

    /** A creative tab builder that is not bound to a fixed row and column. */
    CreativeModeTab.Builder creativeTabBuilder();
}
