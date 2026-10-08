package li.cil.oc2.common.item.network.cable;

import java.util.Objects;
import java.util.Optional;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.item.ModItem;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <26.1 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NetworkCableItem extends ModItem {

    private static final String LINK_START_TAG_NAME = API.MOD_ID + ":" + "network_cable_link_start";

    @Override
    //? if >=26.1 {
    /*public InteractionResult use(
    *///?} else {
    public InteractionResultHolder<ItemStack> use(
    //?}
            final Level level, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                final CompoundTag persistentData = Platform.hooks().getPersistentData(player);
                persistentData.remove(LINK_START_TAG_NAME);
            }

            //? if >=26.1 {
            /*return InteractionResult.SUCCESS;
            *///?} else {
            return InteractionResultHolder.success(player.getItemInHand(hand));
            //?}
        }

        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null) {
            return super.useOn(context);
        }

        final ItemStack stack = player.getItemInHand(context.getHand());
        if (stack.isEmpty() || !stack.getItem().equals(this)) {
            return super.useOn(context);
        }

        final Level level = context.getLevel();
        final BlockPos currentPos = context.getClickedPos();

        final BlockEntity currentBlockEntity = level.getBlockEntity(currentPos);
        if (!(currentBlockEntity instanceof final NetworkConnectorBlockEntity currentConnector)) {
            return super.useOn(context);
        }

        if (!level.isClientSide() && handleServerUse(player, level, currentPos, currentConnector, stack)) {
            return super.useOn(context);
        }

        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide());
        //?}
    }

    private boolean handleServerUse(
            final Player player,
            final Level level,
            final BlockPos currentPos,
            final NetworkConnectorBlockEntity currentConnector,
            final ItemStack stack) {
        final CompoundTag persistentData = Platform.hooks().getPersistentData(player);
        final Optional<BlockPos> startPos =
                //? if >=26.1 {
                /*persistentData.read(LINK_START_TAG_NAME, BlockPos.CODEC);
                *///?} else {
                NbtUtils.readBlockPos(persistentData, LINK_START_TAG_NAME);
                //?}
        persistentData.remove(LINK_START_TAG_NAME);
        if (startPos.isEmpty() || Objects.equals(startPos.get(), currentPos)) {
            beginLink(persistentData, currentPos, currentConnector, player);
        } else {
            return completeLink(
                    level, startPos.get(), currentConnector, player, stack, persistentData);
        }
        return false;
    }

    private void beginLink(
            final CompoundTag persistentData,
            final BlockPos currentPos,
            final NetworkConnectorBlockEntity currentConnector,
            final Player player) {
        // A full connector must still be able to start a link so an existing one can be removed;
        // completeLink reports FULL if the second click would add a new link.
        if (currentConnector.canConnectMore() || !currentConnector.getConnectedPositions().isEmpty()) {
            //? if >=26.1 {
            /*persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, currentPos);
            notifyPlayer(player, Constants.CONNECTOR_LINK_STARTED, SoundEvents.LEAD_TIED, 0.6f);
            *///?} else {
            persistentData.put(LINK_START_TAG_NAME, NbtUtils.writeBlockPos(currentPos));
            notifyPlayer(player, Constants.CONNECTOR_LINK_STARTED, SoundEvents.LEASH_KNOT_PLACE, 0.6f);
            //?}
        } else {
            notifyPlayer(player, Constants.CONNECTOR_ERROR_FULL, SoundEvents.DISPENSER_FAIL, 1f);
        }
    }

    private boolean completeLink(
            final Level level,
            final BlockPos startPos,
            final NetworkConnectorBlockEntity currentConnector,
            final Player player,
            final ItemStack stack,
            final CompoundTag persistentData) {
        final BlockEntity startBlockEntity = level.getBlockEntity(startPos);
        if (!(startBlockEntity instanceof final NetworkConnectorBlockEntity startConnector)) {
            // Starting connector was removed in the meantime.
            return true;
        }

        if (startConnector.getConnectedPositions().contains(currentConnector.getBlockPos())) {
            // Using the cable on two connectors that are already linked removes that link. The
            // cable drops only in survival: a creative player never paid one for this link.
            NetworkConnectorBlockEntity.disconnect(
                    startConnector, currentConnector, !player.isCreative());
            return handleConnectionResult(
                    ConnectionResult.DISCONNECTED, startPos, player, stack, persistentData);
        }

        final ConnectionResult connectionResult =
                NetworkConnectorBlockEntity.connect(startConnector, currentConnector);
        return handleConnectionResult(connectionResult, startPos, player, stack, persistentData);
    }

    private boolean handleConnectionResult(
            final ConnectionResult connectionResult,
            final BlockPos startPos,
            final Player player,
            final ItemStack stack,
            final CompoundTag persistentData) {
        switch (connectionResult) {
            case SUCCESS:
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                //? if >=26.1 {
                /*notifyPlayer(player, Constants.CONNECTOR_CONNECTED, SoundEvents.LEAD_TIED, 1f);
                *///?} else {
                notifyPlayer(player, Constants.CONNECTOR_CONNECTED, SoundEvents.LEASH_KNOT_PLACE, 1f);
                //?}
                break;
            case DISCONNECTED:
                notifyPlayer(
                        //? if >=26.1 {
                        /*player, Constants.CONNECTOR_DISCONNECTED, SoundEvents.LEAD_BREAK, 1f);
                        *///?} else {
                        player, Constants.CONNECTOR_DISCONNECTED, SoundEvents.LEASH_KNOT_BREAK, 1f);
                        //?}
                break;

            case FAILURE:
                keepLinkStart(persistentData, startPos);
                break;
            case ALREADY_CONNECTED:
                keepLinkStart(persistentData, startPos);
                notifyPlayer(
                        player, Constants.CONNECTOR_ERROR_ALREADY_CONNECTED, SoundEvents.DISPENSER_FAIL, 1f);
                break;
            case FAILURE_FULL:
                keepLinkStart(persistentData, startPos);
                notifyPlayer(
                        player, Constants.CONNECTOR_ERROR_FULL, SoundEvents.DISPENSER_FAIL, 1f);
                break;
            case FAILURE_TOO_FAR:
                keepLinkStart(persistentData, startPos);
                notifyPlayer(
                        player, Constants.CONNECTOR_ERROR_TOO_FAR, SoundEvents.DISPENSER_FAIL, 1f);
                break;
            case FAILURE_OBSTRUCTED:
                keepLinkStart(persistentData, startPos);
                notifyPlayer(
                        player, Constants.CONNECTOR_ERROR_OBSTRUCTED, SoundEvents.DISPENSER_FAIL, 1f);
                break;
            default:
                throw new AssertionError(connectionResult);
        }
        return false;
    }

    private static void notifyPlayer(
            final Player player, final String messageKey, final SoundEvent sound, final float pitch) {
        //? if >=26.1 {
        /*player.sendOverlayMessage(Component.translatable(messageKey));
        *///?} else {
        player.displayClientMessage(Component.translatable(messageKey), true);
        //?}
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.BLOCKS, 0.8f, pitch);
    }

    private static void keepLinkStart(
            final CompoundTag persistentData, final BlockPos startPos) {
        //? if >=26.1 {
        /*persistentData.store(LINK_START_TAG_NAME, BlockPos.CODEC, startPos);
        *///?} else {
        persistentData.put(LINK_START_TAG_NAME, NbtUtils.writeBlockPos(startPos));
        //?}
    }
}