package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.integration.Integrations;
import li.cil.oc2.common.integration.create.CreateControlsDevice;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Compiled only with -PcreateTestJar; every test requires the actual Create mod. */
@GameTestHolder("oc2r_create")
@PrefixGameTestTemplate(false)
public final class CreateControlsAcceptanceTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = "oc2r_create",
            timeoutTicks = 160)
    public static void busInterfaceDiscoversRealSpeedController(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        final var controls = place(helper, "rotation_speed_controller");
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
        helper.runAfterDelay(60, () -> {
            TestSupport.assertTrue(helper, "Create controls visible through ordinary bus interface",
                    computer.devices().stream().anyMatch(d -> d instanceof RPCDevice rpc
                            && rpc.getTypeNames().contains("create_controls")));
            final int id = controls.getSettings().getFirst().id();
            TestSupport.assertTrue(helper, "negative RPM roundtrip", controls.setValue(id, -64) == -64);
            TestSupport.assertTrue(helper, "configured target", controls.getValue(id) == -64);
            final int limit = controls.setValue(id, Integer.MAX_VALUE);
            TestSupport.assertTrue(helper, "Create enforces positive configured limit",
                    limit > 0 && limit < Integer.MAX_VALUE);
            TestSupport.assertTrue(helper, "Create enforces negative configured limit",
                    controls.setValue(id, Integer.MIN_VALUE) == -limit);
            TestSupport.assertTrue(helper, "stop command", controls.setValue(id, 0) == 0);
            helper.succeed();
        });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = "oc2r_create")
    public static void realBearingOptionsUseEnumControls(final GameTestHelper helper) {
        final var controls = place(helper, "mechanical_bearing");
        final var setting = controls.getSettings().stream().filter(s -> s.options().size() > 1)
                .findFirst().orElseThrow(() -> TestSupport.failure(helper, "bearing enum control not exposed"));
        TestSupport.assertTrue(helper, "enum value roundtrip", controls.setValue(setting.id(), 1) == 1);
        TestSupport.assertTrue(helper, "enum clamp",
                controls.setValue(setting.id(), 999) == setting.options().size() - 1);
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = "oc2r_create")
    public static void removedCreateBlockCannotBeChangedByStaleDevice(final GameTestHelper helper) {
        final var controls = place(helper, "rotation_speed_controller");
        helper.getLevel().destroyBlock(helper.absolutePos(TestSupport.DEVICE_POS), false);
        try {
            controls.setValue(0, 64);
            throw TestSupport.failure(helper, "stale control changed removed block");
        } catch (final IllegalStateException expected) {
            TestSupport.assertTrue(helper, "detached block rejected",
                    "Create block is no longer attached".equals(expected.getMessage()));
        }
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = "oc2r_create")
    public static void machineInterfaceControlsRealCreateSettings(final GameTestHelper helper) {
        final var controls = place(helper, "rotation_speed_controller");
        helper.setBlock(TestSupport.CABLE_POS, li.cil.oc2.common.block.common.Blocks.MACHINE_INTERFACE.get()
                .defaultBlockState().setValue(li.cil.oc2.common.block.machine.MachineInterfaceBlock.FACING,
                        Direction.EAST));
        final li.cil.oc2.common.blockentity.machine.MachineInterfaceBlockEntity machine = TestSupport.blockEntity(
                helper, TestSupport.CABLE_POS, li.cil.oc2.common.blockentity.machine.MachineInterfaceBlockEntity.class);
        final int id = machine.getSettings().getFirst().id();
        TestSupport.assertEquals(helper, "machine changes real Create RPM", -32, machine.setSetting(id, -32));
        TestSupport.assertEquals(helper, "real controller reads new target", -32, controls.getValue(id));
        TestSupport.assertTrue(helper, "machine target ID",
                machine.getTargetBlock().equals("create:rotation_speed_controller"));
        helper.succeed();
    }

    private static CreateControlsDevice place(final GameTestHelper helper, final String id) {
        TestSupport.assertTrue(helper, "real Create must be installed for this opt-in suite",
                Integrations.isCreateLoaded());
        final var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
        TestSupport.assertTrue(helper, "Create block is registered: " + id, block != Blocks.AIR);
        helper.setBlock(TestSupport.DEVICE_POS, block.defaultBlockState());
        return CreateControlsDevice.find(helper.getLevel().getBlockEntity(helper.absolutePos(TestSupport.DEVICE_POS)))
                .orElseThrow(() -> TestSupport.failure(helper, "Create adapter not available for " + id));
    }

    private CreateControlsAcceptanceTests() {}
}
