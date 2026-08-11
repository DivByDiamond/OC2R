package li.cil.oc2.common.block.cable.interaction;

import static li.cil.oc2.common.block.cable.BusCableStateProperties.*;
import static li.cil.oc2.common.util.text.TranslationUtils.text;

import javax.annotation.Nullable;
import li.cil.oc2.client.gui.screen.monitor.BusInterfaceScreen;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.facade.FacadeType;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.world.level.LevelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class BusCableInteractionHandler {
    @Nullable
    public static ItemInteractionResult handleUseItemOn(
            final ItemStack heldItem,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        if (heldItem.getItem().equals(Items.BUS_CABLE.get())
                || heldItem.getItem().equals(Items.BUS_INTERFACE.get())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final BusCableBlockEntity busCableBlockEntity)) {
            return null;
        }

        if (Wrenches.isWrench(heldItem)) {
            if (player.isShiftKeyDown()) {
                final ItemStack facadeItem = busCableBlockEntity.getFacade();
                if (!facadeItem.isEmpty()) {
                    if (!level.isClientSide()) {
                        busCableBlockEntity.removeFacade();
                        if (!player.isCreative()
                                && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                            ItemStackUtils.spawnAsEntity(
                                            level, pos, facadeItem, hitResult.getDirection())
                                    .ifPresent(
                                            entity -> {
                                                entity.setNoPickUpDelay();
                                                entity.playerTouch(player);
                                            });
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    if (getPartCount(state) > 1
                            && (tryRemoveInterface(state, level, pos, player, hitResult)
                                    || tryRemoveCable(state, level, pos, player))) {
                        return ItemInteractionResult.sidedSuccess(level.isClientSide());
                    }
                }
            } else {
                final Direction side = getHitSide(pos, hitResult);
                if (getConnectionType(state, side) == ConnectionType.INTERFACE) {
                    if (level.isClientSide()) {
                        openBusInterfaceScreen(busCableBlockEntity, side);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        } else if (!player.isShiftKeyDown()
                && !state.getValue(HAS_FACADE)
                && getInterfaceCount(state) == 0) {
            final var facadeType = busCableBlockEntity.getFacadeType(heldItem);
            if (facadeType == FacadeType.INVALID_BLOCK) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            text("message.{mod}.invalid_facade_block"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            } else if (facadeType == FacadeType.VALID_BLOCK) {
                if (!level.isClientSide()) {
                    busCableBlockEntity.setFacade(heldItem);
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return null;
    }

    private static boolean tryRemoveInterface(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hit) {
        final Direction side = getHitSide(pos, hit);
        final EnumProperty<ConnectionType> property = FACING_TO_CONNECTION_MAP.get(side);
        if (state.getValue(property) != ConnectionType.INTERFACE) {
            return false;
        }
        final BlockPos neighborPos = pos.relative(side);
        final boolean isReplacedByCable =
                state.getValue(HAS_CABLE)
                        && canHaveCableTo(level.getBlockState(neighborPos), side.getOpposite());
        if (isReplacedByCable) {
            level.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.CABLE));
        } else {
            level.setBlockAndUpdate(pos, state.setValue(property, ConnectionType.NONE));
        }
        handlePartRemoved(
                state,
                level,
                pos,
                side,
                player,
                new ItemStack(Items.BUS_INTERFACE.get()),
                isReplacedByCable);
        return true;
    }

    private static boolean tryRemoveCable(
            final BlockState state, final Level level, final BlockPos pos, final Player player) {
        if (!state.getValue(HAS_CABLE)) {
            return false;
        }
        level.setBlockAndUpdate(pos, state.setValue(HAS_CABLE, false));
        handlePartRemoved(
                state, level, pos, null, player, new ItemStack(Items.BUS_CABLE.get()), true);
        return true;
    }

    private static void handlePartRemoved(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            @Nullable final Direction side,
            final Player player,
            final ItemStack drop,
            final boolean neighborConnectionChanged) {
        onConnectionTypeChanged(level, pos, side, neighborConnectionChanged);
        if (!player.isCreative() && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStackUtils.spawnAsEntity(level, pos, drop, side)
                    .ifPresent(
                            entity -> {
                                entity.setNoPickUpDelay();
                                entity.playerTouch(player);
                            });
        }
        LevelUtils.playSound(level, pos, state.getSoundType(level, pos, null), SoundType::getBreakSound);
    }

    @OnlyIn(Dist.CLIENT)
    private static void openBusInterfaceScreen(
            final BusCableBlockEntity blockEntity, final Direction side) {
        final BusInterfaceScreen screen = new BusInterfaceScreen(blockEntity, side);
        Minecraft.getInstance().setScreen(screen);
    }

    private BusCableInteractionHandler() {}
}