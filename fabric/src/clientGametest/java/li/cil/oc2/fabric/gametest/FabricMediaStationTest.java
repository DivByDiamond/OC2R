package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.media.MediaStationBlock;
import li.cil.oc2.common.block.gps.GpsTowerBlock;
import li.cil.oc2.common.blockentity.media.MediaStationBlockEntity;
import li.cil.oc2.common.firmware.FlashWriterService;
import li.cil.oc2.common.item.Items;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Native rendering, live media synchronization, and firmware GUI for the combined station. */
public final class FabricMediaStationTest implements FabricClientGameTest {
    private static final BlockPos STATION = new BlockPos(-2, -59, 0);

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.fov().set(60);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
            hideHud(client);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 2.2 -58.3 -3.0 17 12");
            world.getServer().runOnServer(server -> {
                final var level = server.overworld();
                final var player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                level.setBlockAndUpdate(STATION, Blocks.MEDIA_STATION.get().defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(0, -59, 0), Blocks.MEDIA_STATION.get().defaultBlockState()
                        .setValue(MediaStationBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(new BlockPos(2, -59, 0), Blocks.MEDIA_STATION.get().defaultBlockState()
                        .setValue(MediaStationBlock.FACING, Direction.SOUTH));
                level.setBlockAndUpdate(new BlockPos(4, -60, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int part = 0; part < 5; part++) {
                    level.setBlockAndUpdate(new BlockPos(4, -59 + part, 0), Blocks.GPS_TOWER.get().defaultBlockState()
                            .setValue(GpsTowerBlock.PART, part));
                }
                if (!li.cil.oc2.common.block.gps.GpsTowerStructure.isComplete(level, new BlockPos(4, -59, 0))) {
                    throw new IllegalStateException("Reference tower did not assemble");
                }
                level.setBlockAndUpdate(new BlockPos(6, -59, 0), Blocks.NETWORK_HUB.get().defaultBlockState());
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockEntity(STATION) instanceof MediaStationBlockEntity, 200);
            context.waitTicks(40);
            context.takeScreenshot("oc2r-standard-bus-ports-and-vents");
            world.getServer().runCommand("tp @a 5.2 -58.4 -3.0 0 10");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-gps-standard-port");
            world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(
                    new BlockPos(8, -59, 0), Blocks.MACHINE_INTERFACE.get().defaultBlockState()));
            world.getServer().runCommand("tp @a 10.2 -58.1 -3.0 30 16");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-machine-interface-minimal");
            world.getServer().runOnServer(server -> {
                server.overworld().removeBlock(new BlockPos(8, -59, 0), false);
                server.overworld().setBlockAndUpdate(new BlockPos(8, -56, 0),
                        Blocks.MACHINE_INTERFACE.get().defaultBlockState());
            });
            world.getServer().runCommand("tp @a 10.2 -59.5 -3.0 30 -31");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-machine-interface-bottom-port");
            // Insert after initial tracking: this verifies a real update packet, not just initial chunk NBT.
            world.getServer().runOnServer(server -> {
                final var station = (MediaStationBlockEntity) server.overworld().getBlockEntity(STATION);
                station.insertFloppy(new ItemStack(Items.FLOPPY.get()));
                station.insert(new ItemStack(Items.FLASH_MEMORY_SMALL.get()), null);
            });
            context.waitFor(client -> client.level.getBlockEntity(STATION) instanceof MediaStationBlockEntity station
                    && !station.getRenderedFloppy().isEmpty() && !station.getDiskItemStack().isEmpty(), 100);
            world.getServer().runCommand("tp @a -0.5 -58.4 -3.0 25 10");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-media-station-front-and-floppy");
            world.getServer().runCommand("tp @a -0.5 -58.4 3.0 158 10");
            context.waitTicks(20);
            context.takeScreenshot("oc2r-media-station-rear-port");
            world.getServer().runOnServer(server -> {
                final var station = (MediaStationBlockEntity) server.overworld().getBlockEntity(STATION);
                if (FlashWriterService.open(server.getPlayerList().getPlayers().getFirst(), station) == null) {
                    throw new IllegalStateException("Media station could not open its firmware session");
                }
            });
            context.waitForScreen(li.cil.oc2.client.gui.screen.firmware.FlashWriterScreen.class);
            context.waitTicks(10);
            context.takeScreenshot("oc2r-media-station-firmware-gui");
        }
    }

    private static void hideHud(final Minecraft client) {
        try {
            try {
                final Object hud = client.gui.getClass().getField("hud").get(client.gui);
                if (!(boolean) hud.getClass().getMethod("isHidden").invoke(hud)) {
                    hud.getClass().getMethod("toggle").invoke(hud);
                }
            } catch (final NoSuchFieldException legacy) {
                client.options.getClass().getField("hideGui").setBoolean(client.options, true);
            }
        } catch (final ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot hide HUD for asset inspection", error);
        }
    }
}
