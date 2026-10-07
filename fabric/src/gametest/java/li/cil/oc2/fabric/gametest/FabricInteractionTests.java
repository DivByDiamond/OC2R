package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.fabric.FabricInteractionHooks;
import li.cil.oc2.fabric.PersistentDataHolder;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Proves the Fabric side of the NeoForge-only interaction hooks on a real server. */
public final class FabricInteractionTests implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    private static BlockHitResult hit(final GameTestHelper helper, final BlockPos pos, final Direction face) {
        final BlockPos absolute = helper.absolutePos(pos);
        final Vec3 location = Vec3.atCenterOf(absolute)
                .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(location, face, absolute, false);
    }

    private static boolean leverPowered(final GameTestHelper helper) {
        return helper.getBlockState(POS).getValue(LeverBlock.POWERED);
    }

    private static void sneakUse(final GameTestHelper helper, final ItemStack stack) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                hit(helper, POS, Direction.NORTH));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void wrenchSneakStillUsesBlock(final GameTestHelper helper) {
        helper.setBlock(POS, net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        sneakUse(helper, new ItemStack(Items.WRENCH.get()));
        if (!leverPowered(helper)) {
            helper.fail("sneaking with the wrench did not reach the lever");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void otherItemSneakDoesNotUseBlock(final GameTestHelper helper) {
        helper.setBlock(POS, net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        sneakUse(helper, new ItemStack(net.minecraft.world.item.Items.STICK));
        if (leverPowered(helper)) {
            helper.fail("sneaking with a stick must not use the lever (vanilla behaviour changed)");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void nonSneakingUsesBlock(final GameTestHelper helper) {
        helper.setBlock(POS, net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ItemStack stack = new ItemStack(net.minecraft.world.item.Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                hit(helper, POS, Direction.NORTH));
        if (!leverPowered(helper)) {
            helper.fail("plain use must reach the lever");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pickBlockOfBusCable(final GameTestHelper helper) {
        final BlockState state = Blocks.BUS_CABLE.get().defaultBlockState()
                .setValue(BusCableStateProperties.HAS_CABLE, true)
                .setValue(BusCableStateProperties.FACING_TO_CONNECTION_MAP.get(Direction.NORTH),
                        ConnectionType.INTERFACE);
        helper.setBlock(POS, state);
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final BlockPos absolute = helper.absolutePos(POS);

        final ItemStack onInterface = FabricInteractionHooks.pickBlock(
                helper.getLevel().getBlockState(absolute), hit(helper, POS, Direction.NORTH),
                helper.getLevel(), absolute, player);
        if (!onInterface.is(Items.BUS_INTERFACE.get())) {
            helper.fail("picking the interface side gave " + onInterface);
        }

        final ItemStack onCable = FabricInteractionHooks.pickBlock(
                helper.getLevel().getBlockState(absolute), hit(helper, POS, Direction.SOUTH),
                helper.getLevel(), absolute, player);
        if (!onCable.is(Items.BUS_CABLE.get())) {
            helper.fail("picking the cable gave " + onCable);
        }

        // Facade: the pick result is the facade item.
        final BusCableBlockEntity cable = (BusCableBlockEntity) helper.getBlockEntity(POS);
        cable.setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));
        final ItemStack onFacade = FabricInteractionHooks.pickBlock(
                helper.getLevel().getBlockState(absolute), hit(helper, POS, Direction.NORTH),
                helper.getLevel(), absolute, player);
        if (!onFacade.is(net.minecraft.world.item.Items.STONE)) {
            helper.fail("picking the facade gave " + onFacade);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void persistentDataSurvivesRespawn(final GameTestHelper helper) {
        final ServerPlayer oldPlayer = helper.makeMockServerPlayerInLevel();
        final ServerPlayer newPlayer = helper.makeMockServerPlayerInLevel();
        final CompoundTag marker = new CompoundTag();
        marker.putInt("answer", 42);
        ((PersistentDataHolder) oldPlayer).oc2r$getPersistentData().put("oc2r_gametest", marker);

        ServerPlayerEvents.COPY_FROM.invoker().copyFromPlayer(oldPlayer, newPlayer, false);

        final CompoundTag copied = ((PersistentDataHolder) newPlayer).oc2r$getPersistentData()
                .getCompound("oc2r_gametest");
        if (copied.getInt("answer") != 42) {
            helper.fail("persistent data was not copied: " + copied);
        }
        if (copied == marker) {
            helper.fail("persistent data must be copied, not shared");
        }
        helper.succeed();
    }
}
