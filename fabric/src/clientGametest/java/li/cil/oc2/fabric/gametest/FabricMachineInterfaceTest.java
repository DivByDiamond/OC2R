package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.machine.MachineInterfaceBlock;
import li.cil.oc2.common.blockentity.machine.MachineInterfaceBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reproducible world model and vanilla buffer GUI test, without guest boot or audio. */
public final class FabricMachineInterfaceTest implements FabricClientGameTest {
    private static final BlockPos BASE = new BlockPos(0, -59, 0);

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.fov().set(60);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 2.8 -58.6 -3.0 35 10");
            world.getServer().runOnServer(server -> {
                final var level = server.overworld();
                level.setBlockAndUpdate(BASE, Blocks.MACHINE_INTERFACE.get().defaultBlockState()
                        .setValue(MachineInterfaceBlock.FACING, Direction.NORTH));
                level.setBlockAndUpdate(BASE.north(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                final var player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                final var machine = (MachineInterfaceBlockEntity) level.getBlockEntity(BASE);
                machine.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 32));
                if (machine.pushItems(0, 0, 16) != 16) throw new IllegalStateException("Chest transfer failed");
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(BASE).is(Blocks.MACHINE_INTERFACE.get()), 200);
            context.waitTicks(40);
            context.takeScreenshot("oc2r-machine-interface-world");
            world.getServer().runOnServer(server -> {
                final var machine = (MachineInterfaceBlockEntity) server.overworld().getBlockEntity(BASE);
                server.getPlayerList().getPlayers().getFirst().openMenu(machine);
            });
            context.waitFor(client -> client.player != null
                    && client.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu
                    && client.player.containerMenu.getType() == MenuType.GENERIC_9x2, 100);
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (client.player.containerMenu.getSlot(0).getItem().getCount() != 16) {
                    throw new IllegalStateException("Buffer GUI does not show remaining items");
                }
            });
            context.takeScreenshot("oc2r-machine-interface-buffer");
        }
    }
}
