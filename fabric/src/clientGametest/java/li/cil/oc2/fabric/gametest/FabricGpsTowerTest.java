package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.gps.GpsTowerBlock;
import li.cil.oc2.common.block.gps.GpsTowerStructure;
import li.cil.oc2.common.item.Items;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real client render/selection smoke test, independent of booting a guest OS. */
public final class FabricGpsTowerTest implements FabricClientGameTest {
    private static final BlockPos BASE = new BlockPos(0, -59, 0);

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.fov().set(75);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 3.0 -58.1 -3.0 35.5 0");
            world.getServer().runOnServer(server -> {
                final var level = server.overworld();
                level.setBlockAndUpdate(BASE.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                final var player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                final ItemStack stack = new ItemStack(Items.GPS_TOWER.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atBottomCenterOf(BASE), Direction.UP, BASE.below(), false)));
                if (!GpsTowerStructure.isComplete(level, BASE)) throw new IllegalStateException("Tower not placed");
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(BASE.above(4)).is(Blocks.GPS_TOWER.get()), 200);
            context.waitTicks(40);
            context.runOnClient(client -> {
                final var state = client.level.getBlockState(BASE.above(2));
                if (state.getValue(GpsTowerBlock.PART) != 2
                        || state.getShape(client.level, BASE.above(2)).bounds().getYsize() != 5) {
                    throw new IllegalStateException("Incomplete tower selection shape");
                }
            });
            context.waitFor(client -> client.level != null && client.hitResult instanceof BlockHitResult hit
                    && client.level.getBlockState(hit.getBlockPos()).is(Blocks.GPS_TOWER.get()), 100);
            context.takeScreenshot("oc2r-gps-tower-assembled");
            context.runOnClient(client -> {
                final BlockHitResult hit = (BlockHitResult) client.hitResult;
                if (!client.gameMode.startDestroyBlock(hit.getBlockPos(), hit.getDirection())) {
                    throw new IllegalStateException("Client could not break selected tower part");
                }
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(BASE).isAir()
                    && client.level.getBlockState(BASE.above(4)).isAir(), 100);
            context.takeScreenshot("oc2r-gps-tower-disassembled");
        }
    }
}
