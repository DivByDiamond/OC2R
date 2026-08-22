package li.cil.oc2.common.item.network.cable;

import java.util.Objects;
import java.util.Optional;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.item.ModItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NetworkCableItem extends ModItem {
    private static final String LINK_START_TAG_NAME = API.MOD_ID + ":" + "network_cable_link_start";

    @Override
    public InteractionResultHolder<ItemStack> use(
            final Level level, final Player player, final InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (player instanceof final ServerPlayer serverPlayer) {
                final CompoundTag persistentData = serverPlayer.getPersistentData();
                persistentData.remove(LINK_START_TAG_NAME);
            }

            return InteractionResultHolder.success(player.getItemInHand(hand));
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

        if (!level.isClientSide()
                && player instanceof final ServerPlayer serverPlayer
                && handleServerUse(serverPlayer, level, currentPos, currentConnector, stack)) {
            return super.useOn(context);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private boolean handleServerUse(
            final ServerPlayer serverPlayer,
            final Level level,
            final BlockPos currentPos,
            final NetworkConnectorBlockEntity currentConnector,
            final ItemStack stack) {
        final CompoundTag persistentData = serverPlayer.getPersistentData();
        final Optional<BlockPos> startPos =
                NbtUtils.readBlockPos(persistentData, LINK_START_TAG_NAME);
        persistentData.remove(LINK_START_TAG_NAME);
        if (startPos.isEmpty() || Objects.equals(startPos.get(), currentPos)) {
            beginLink(persistentData, currentPos, currentConnector, serverPlayer);
        } else {
            return completeLink(
                    level, startPos.get(), currentConnector, serverPlayer, stack, persistentData);
        }
        return false;
    }

    private void beginLink(
            final CompoundTag persistentData,
            final BlockPos currentPos,
            final NetworkConnectorBlockEntity currentConnector,
            final Player player) {
        if (currentConnector.canConnectMore()) {
            persistentData.put(LINK_START_TAG_NAME, NbtUtils.writeBlockPos(currentPos));
        } else {
            player.displayClientMessage(
                    Component.translatable(Constants.CONNECTOR_ERROR_FULL), true);
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
                break;

            case FAILURE:
                keepLinkStart(persistentData, startPos);
                break;
            case ALREADY_CONNECTED:
                keepLinkStart(persistentData, startPos);
                player.displayClientMessage(
                        Component.translatable(Constants.CONNECTOR_ERROR_ALREADY_CONNECTED),
                        true);
                break;
            case FAILURE_FULL:
                keepLinkStart(persistentData, startPos);
                player.displayClientMessage(
                        Component.translatable(Constants.CONNECTOR_ERROR_FULL), true);
                break;
            case FAILURE_TOO_FAR:
                keepLinkStart(persistentData, startPos);
                player.displayClientMessage(
                        Component.translatable(Constants.CONNECTOR_ERROR_TOO_FAR), true);
                break;
            case FAILURE_OBSTRUCTED:
                keepLinkStart(persistentData, startPos);
                player.displayClientMessage(
                        Component.translatable(Constants.CONNECTOR_ERROR_OBSTRUCTED), true);
                break;
            default:
                throw new AssertionError(connectionResult);
        }
        return false;
    }

    private static void keepLinkStart(
            final CompoundTag persistentData, final BlockPos startPos) {
        persistentData.put(LINK_START_TAG_NAME, NbtUtils.writeBlockPos(startPos));
    }
}