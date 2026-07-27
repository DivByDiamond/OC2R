package li.cil.oc2.common.block;

import static li.cil.oc2.common.block.BusCableStateProperties.*;
import static li.cil.oc2.common.util.TranslationUtils.text;

import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.client.gui.screen.BusInterfaceScreen;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.FacadeType;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

final class BusCableInteractionHandler {
    @Nullable
    static ItemInteractionResult handleUseItemOn(
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