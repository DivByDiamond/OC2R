package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.misc.EnvironmentSensorBlock;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PeripheralAnimationTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 250)
    public static void sensorIndicatorTracksRunningVmHotPlugAndDisconnect(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        final BlockPos sensor = TestSupport.DEVICE_POS;
        helper.setBlock(sensor.south(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.setBlock(sensor, Blocks.ENVIRONMENT_SENSOR.get().defaultBlockState());
        helper.startSequence().thenIdle(25)
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "unconnected sensor stays off after sampling interval", !lit(helper, sensor));
                    computer.start();
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "computer starts", computer.virtualMachine().isRunning()))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "running nearby computer alone does not light sensor", !lit(helper, sensor));
                    TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "hot-plug lights the mounted sensor", lit(helper, sensor)))
                .thenExecute(computer::stop)
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "stopping VM turns sensor off", !lit(helper, sensor)))
                .thenExecute(computer::start)
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "restarting VM lights sensor again", lit(helper, sensor)))
                .thenExecute(() -> helper.setBlock(TestSupport.CABLE_POS,
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "removing bus cable turns sensor off", !lit(helper, sensor)))
                .thenExecute(computer::stop)
                .thenSucceed();
    }

    private static boolean lit(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockState(pos).getValue(EnvironmentSensorBlock.LIT);
    }

    private PeripheralAnimationTests() {}
}
