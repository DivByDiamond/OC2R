package li.cil.oc2.common.entity.robot.misc;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.world.level.LevelUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;

public final class RobotInteractionHandler {
    public static InteractionResult interact(
            final Robot robot, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!robot.level().isClientSide()) {
            if (Wrenches.isWrench(stack)) {
                if (player.isShiftKeyDown()) {
                    dropSelf(robot);
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    robot.openInventoryScreen(serverPlayer);
                }
            } else {
                if (player.isShiftKeyDown()) {
                    robot.start();
                } else if (player instanceof final ServerPlayer serverPlayer) {
                    robot.openTerminalScreen(serverPlayer);
                }
            }
        }
        return InteractionResult.sidedSuccess(robot.level().isClientSide());
    }

    public static boolean skipAttackInteraction(final Robot robot, final Entity entity) {
        if (entity instanceof Player player && player.isCreative()) {
            dropSelf(robot);
        }
        return true;
    }

    public static void dropSelf(final Robot robot) {
        if (!robot.isAlive()) {
            return;
        }
        final ItemStack stack = new ItemStack(Items.ROBOT.get());
        robot.exportToItemStack(stack);
        robot.spawnAtLocation(stack);
        robot.discard();
        LevelUtils.playSound(
                robot.level(), robot.blockPosition(), SoundType.METAL, SoundType::getBreakSound);
    }
}