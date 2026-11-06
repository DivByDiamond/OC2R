package li.cil.oc2.common.item.tablet;

import java.util.Optional;
import java.util.UUID;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.item.ModItem;
import li.cil.oc2.common.tablet.TabletManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
*///?} else {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A pocket computer. The item only carries an id; the machine lives in world data under it. */
public final class TabletItem extends ModItem {
    public TabletItem() {
        super(createProperties().stacksTo(1));
    }

    /** The id stored in the stack, if it has been opened at least once. */
    public static Optional<UUID> getId(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TabletItem)) return Optional.empty();
        return Optional.ofNullable(stack.get(DataComponents.TABLET_ID.get()));
    }

    /** Returns the id of the stack, assigning a fresh one the first time. */
    public static UUID getOrCreateId(final ItemStack stack) {
        return getId(stack)
                .orElseGet(
                        () -> {
                            final UUID id = UUID.randomUUID();
                            stack.set(DataComponents.TABLET_ID.get(), id);
                            return id;
                        });
    }

    //? if >=26.1 {
    /*@Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
            TabletManager.openTerminal(serverPlayer, getOrCreateId(player.getItemInHand(hand)));
        }
        return InteractionResult.SUCCESS;
    }
    *///?} else {
    @Override
    public InteractionResultHolder<ItemStack> use(
            final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
            TabletManager.openTerminal(serverPlayer, getOrCreateId(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    //?}
}
