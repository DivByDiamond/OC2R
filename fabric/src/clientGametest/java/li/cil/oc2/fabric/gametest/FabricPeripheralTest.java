package li.cil.oc2.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.misc.EnvironmentSensorBlock;
import li.cil.oc2.common.block.network.WirelessAccessPointBlock;
import li.cil.oc2.common.item.Items;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real client model, texture and connection smoke test for the two supplied peripherals. */
public final class FabricPeripheralTest implements FabricClientGameTest {
    private static final BlockPos POINT = new BlockPos(0, -57, 0);
    private static final BlockPos SENSOR = new BlockPos(2, -57, 0);

    @Override
    public void runTest(final ClientGameTestContext context) {
        context.runOnClient(client -> {
            client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.framerateLimit().set(30);
            client.options.enableVsync().set(false);
        });
        try (var world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 3.8 -56.2 -4.0 32 12");
            world.getServer().runOnServer(server -> {
                final var level = server.overworld();
                final var player = server.getPlayerList().getPlayers().getFirst();
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                level.setBlockAndUpdate(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
                final ItemStack connector = new ItemStack(Items.NETWORK_CONNECTOR.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, connector);
                connector.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atBottomCenterOf(POINT), Direction.DOWN, POINT, false)));
                if (!level.getBlockState(POINT.below()).is(Blocks.NETWORK_CONNECTOR.get()))
                    throw new AssertionError("Access point underside connector could not be placed");
                level.setBlockAndUpdate(SENSOR.south(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                final ItemStack sensor = new ItemStack(Items.ENVIRONMENT_SENSOR.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, sensor);
                sensor.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(SENSOR.south()).add(0, 0, -0.5),
                                Direction.NORTH, SENSOR.south(), false)));
                if (!level.getBlockState(SENSOR).is(Blocks.ENVIRONMENT_SENSOR.get()))
                    throw new AssertionError("Sensor could not attach to its wall");
            });
            context.waitFor(client -> client.level != null
                    && client.level.getBlockState(POINT.below()).is(Blocks.NETWORK_CONNECTOR.get())
                    && client.level.getBlockState(SENSOR).is(Blocks.ENVIRONMENT_SENSOR.get()), 200);
            context.waitTicks(30);
            context.runOnClient(client -> {
                for (final Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                    for (final boolean lit : List.of(false, true)) {
                        assertModel(client, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState()
                                .setValue(WirelessAccessPointBlock.FACING, facing).setValue(WirelessAccessPointBlock.LIT, lit),
                                "wireless_access_point/" + (lit ? "on" : "off"));
                        assertModel(client, Blocks.ENVIRONMENT_SENSOR.get().defaultBlockState()
                                .setValue(EnvironmentSensorBlock.FACING, facing).setValue(EnvironmentSensorBlock.LIT, lit),
                                "environment_sensor/" + (lit ? "on" : "off"));
                    }
                }
            });
            context.takeScreenshot("oc2r-wireless-access-point-and-environment-sensor");
            world.getServer().runOnServer(FabricPeripheralTest::activateDevices);
            context.waitFor(client -> client.level.getBlockState(POINT).getValue(WirelessAccessPointBlock.LIT)
                    && client.level.getBlockState(SENSOR).getValue(EnvironmentSensorBlock.LIT), 240);
            context.waitTicks(12);
            context.takeScreenshot("oc2r-peripherals-active-radio-pulse-and-sensor");
            context.waitTicks(6);
            context.takeScreenshot("oc2r-peripherals-active-radio-pulse-next-frame");
            world.getServer().runCommand("tp @a 4.2 -56.2 4.0 145 12");
            context.waitTicks(15);
            context.takeScreenshot("oc2r-environment-sensor-rear-connection");
        }
    }

    private static void activateDevices(final net.minecraft.server.MinecraftServer server) {
        final var level = server.overworld();
        final BlockPos towerPos = new BlockPos(8, -59, 0);
        level.setBlockAndUpdate(towerPos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for (int part = 0; part < 5; part++)
            level.setBlockAndUpdate(towerPos.above(part), Blocks.GPS_TOWER.get().defaultBlockState()
                    .setValue(li.cil.oc2.common.block.gps.GpsTowerBlock.PART, part));
        final var tower = (li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity) level.getBlockEntity(towerPos);
        final var point = (li.cil.oc2.common.blockentity.network.WirelessAccessPointBlockEntity) level.getBlockEntity(POINT);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final var invitation = tower.issueRadioInvite(admin, point.getRadioEndpointId());
        point.joinRadioNetwork(invitation.get("invite"), invitation.get("secret"));
        final BlockPos computerPos = SENSOR.south(3);
        final BlockPos cablePos = SENSOR.south(2);
        final BlockPos rearCable = SENSOR.south();
        level.setBlockAndUpdate(computerPos.below(), Blocks.CREATIVE_ENERGY.get().defaultBlockState());
        // The front face intentionally has no device-bus socket; keep it away from the rear cable.
        level.setBlockAndUpdate(computerPos, Blocks.COMPUTER.get().defaultBlockState()
                .setValue(li.cil.oc2.common.block.computer.ComputerBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(rearCable, Blocks.BUS_CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(cablePos, Blocks.BUS_CABLE.get().defaultBlockState());
        li.cil.oc2.common.block.cable.BusCableStateProperties.addInterface(level, rearCable,
                level.getBlockState(rearCable), Direction.NORTH);
        li.cil.oc2.common.block.cable.BusCableStateProperties.addInterface(level, cablePos,
                level.getBlockState(cablePos), Direction.SOUTH);
        for (final BlockPos pos : List.of(rearCable, cablePos))
            level.setBlockAndUpdate(pos, li.cil.oc2.common.block.cable.BusCableStateProperties.recomputeConnections(
                    level, pos, level.getBlockState(pos)));
        final var computer = (li.cil.oc2.common.blockentity.computer.ComputerBlockEntity) level.getBlockEntity(computerPos);
        final var devices = computer.getItemStackHandlers();
        devices.getItemHandler(li.cil.oc2.api.bus.device.DeviceTypes.CPU).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.CPU_TIER_1.get()));
        devices.getItemHandler(li.cil.oc2.api.bus.device.DeviceTypes.MEMORY).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        devices.getItemHandler(li.cil.oc2.api.bus.device.DeviceTypes.FLASH_MEMORY).orElseThrow()
                .setStackInSlot(0, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.energy.receiveEnergy(Integer.MAX_VALUE, false);
        computer.virtualMachine.start();
    }

    private static void assertModel(final Minecraft client, final BlockState state, final String texture) {
        final var model = client.getModelManager().getBlockStateModelSet().get(state);
        final var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(0), parts);
        int quads = 0;
        boolean expectedTexture = false;
        final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        final List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (final var part : parts) {
            for (final Direction side : sides) {
                for (final var quad : part.getQuads(side)) {
                    quads++;
                    for (int vertex = 0; vertex < 4; vertex++) {
                        final var position = quad.position(vertex);
                        final double[] xyz = {position.x(), position.y(), position.z()};
                        for (int axis = 0; axis < 3; axis++) {
                            min[axis] = Math.min(min[axis], xyz[axis]);
                            max[axis] = Math.max(max[axis], xyz[axis]);
                        }
                    }
                    final String name = quad.materialInfo().sprite().contents().name().toString();
                    if (name.contains("missingno")) throw new AssertionError("Missing texture for " + state);
                    if (name.equals("oc2r:block/" + texture)) {
                        expectedTexture = true;
                        final boolean animated = quad.materialInfo().sprite().contents().isAnimated();
                        if (animated != texture.equals("wireless_access_point/on"))
                            throw new AssertionError("Unexpected texture animation state for " + texture);
                    }
                }
            }
        }
        final var shape = state.getShape(client.level, SENSOR).bounds();
        final double[] expectedMin = {shape.minX, shape.minY, shape.minZ};
        final double[] expectedMax = {shape.maxX, shape.maxY, shape.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(min[axis] - expectedMin[axis]) > 0.00001
                    || Math.abs(max[axis] - expectedMax[axis]) > 0.00001)
                throw new AssertionError("Model and selection box disagree for " + state + " on axis " + axis);
        }
        if (quads < 20 || !expectedTexture)
            throw new AssertionError("Incomplete model for " + state + ": " + quads + " quads, texture=" + expectedTexture);
    }
}
