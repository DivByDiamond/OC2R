/* SPDX-License-Identifier: MIT
 * Derived from MarkdownManual, Copyright (c) 2021-2023 Florian "Sangar" Nücke.
 * See LICENSE-MarkdownManual for the original license.
 */

package li.cil.oc2.manual.api.prefab.item;

import javax.annotation.Nullable;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualScreenStyle;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.util.ShowManualScreenEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <26.1 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Base class that may be used for manual items, used as a representation of some manual and
 * to open said manual.
 */
public abstract class AbstractManualItem extends li.cil.oc2.common.item.ModItem {
    protected AbstractManualItem(final Properties properties) {
        super(properties);
    }

    // --------------------------------------------------------------------- //
    // Item

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level world = context.getLevel();
        if (world.isClientSide()) {
            openManualFor(context, world);
        }
        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResult.sidedSuccess(world.isClientSide());
        //?}
    }

    //? if >=26.1 {
    /*    @Override
    public InteractionResult use(final Level world, final Player player, final InteractionHand hand) {
        if (world.isClientSide()) openManual(player);
        return InteractionResult.SUCCESS;
    }

    *///?} else {
    @Override
    public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
        if (world.isClientSide()) {
            openManual(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide());
    }

    //?}

    // --------------------------------------------------------------------- //

    /**
     * The manual instance represented by this item.
     *
     * @return the manual.
     */
    protected abstract ManualModel getManualModel();

    /**
     * The style used when displaying the manual.
     *
     * @return the style.
     */
    @Nullable
    protected ManualStyle getManualStyle() {
        return null;
    }

    /**
     * The style used for the default manual screen.
     *
     * @return the screen style.
     */
    @Nullable
    protected ManualScreenStyle getScreenStyle() {
        return null;
    }

    /**
     * Opens the {@link net.minecraft.client.gui.screens.Screen} used to display the manual represented by this item.
     * <p>
     * By default, this will open the built-in manual screen, which can be customized using the style returned by
     * {@link #getManualStyle()} and {@link #getScreenStyle()}. To use a custom screen implementation, override this
     * method.
     */
    protected void showManualScreen() {
        ShowManualScreenEvent.SHOW_MANUAL_SCREEN_EVENT.fire(listener ->
            listener.accept(new ShowManualScreenEvent(getManualModel(), getManualStyle(), getScreenStyle())));
    }

    // --------------------------------------------------------------------- //

    private void openManualFor(final UseOnContext context, final Level world) {
        final ManualModel model = getManualModel();
        model.reset();
        model.pathFor(world, context.getClickedPos(), context.getClickedFace()).ifPresent(model::push);
        showManualScreen();
    }

    private void openManual(final Player player) {
        final ManualModel manual = getManualModel();
        if (player.isShiftKeyDown()) {
            manual.reset();
        }
        showManualScreen();
    }
}
