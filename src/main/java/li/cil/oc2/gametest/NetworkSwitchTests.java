/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.util.Arrays;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.switches.NetworkSwitchBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A real switch block between three cabled hosts: flooding, address learning, no stray frames, and the bus. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkSwitchTests {
    private static final BlockPos SWITCH = new BlockPos(5, TestSupport.WORK_Y, 5);
    private static final byte[] BROADCAST = {-1, -1, -1, -1, -1, -1};
    private static final byte[] HOST_A = {2, 0, 0, 0, 0, 0x0A};
    private static final byte[] HOST_B = {2, 0, 0, 0, 0, 0x0B};

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void loneSwitchTicksAndReportsNoLinksOrHosts(final GameTestHelper helper) {
        helper.setBlock(SWITCH, Blocks.NETWORK_SWITCH.get().defaultBlockState());
        final var device = TestSupport.blockEntity(helper, SWITCH, NetworkSwitchBlockEntity.class);
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            for (final boolean linked : device.getLinkState()) {
                TestSupport.assertTrue(helper, "a lone switch has no links", !linked);
            }
            TestSupport.assertTrue(helper, "a lone switch knows no hosts", device.getHostTable().isEmpty());
        }).thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void switchFloodsUnknownAddressesThenForwardsOnlyToTheLearnedPort(final GameTestHelper helper) {
        helper.setBlock(SWITCH, Blocks.NETWORK_SWITCH.get().defaultBlockState());
        final var device = TestSupport.blockEntity(helper, SWITCH, NetworkSwitchBlockEntity.class);
        final var a = new RadioDockEthernetTests.Wire();
        final var b = new RadioDockEthernetTests.Wire();
        final var c = new RadioDockEthernetTests.Wire();
        host(helper, Direction.SOUTH, a);
        host(helper, Direction.EAST, b);
        host(helper, Direction.WEST, c);

        helper.startSequence().thenIdle(6)
                .thenExecute(() -> {
                    int links = 0;
                    for (final boolean linked : device.getLinkState()) if (linked) links++;
                    TestSupport.assertEquals(helper, "cabled faces of the switch", 3, links);
                    a.out.add(frame(BROADCAST, HOST_A, 0x11));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "a broadcast reaches both other hosts",
                        got(b, 0x11) == 1 && got(c, 0x11) == 1))
                .thenExecute(() -> {
                    TestSupport.assertEquals(helper, "the sender does not hear its own broadcast", 0, got(a, 0x11));
                    b.out.add(frame(HOST_A, HOST_B, 0x22));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "a reply reaches the learned host", got(a, 0x22) == 1))
                .thenIdle(10)
                .thenExecute(() -> {
                    TestSupport.assertEquals(helper, "the third host does not see the reply", 0, got(c, 0x22));
                    TestSupport.assertEquals(helper, "no empty or repeated frames at the third host", 1, c.received.size());
                    TestSupport.assertEquals(helper, "both senders are in the address table", 2,
                            device.getHostTable().size());
                })
                .thenSucceed();
    }

    /** The switch has a Lua API (host table, port settings), so a computer on the bus must see it. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void switchIsADeviceOnTheComputerBus(final GameTestHelper helper) {
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        final int[] base = new int[1];
        helper.startSequence()
                .thenExecuteAfter(40, () -> base[0] = computer.deviceCount())
                .thenExecute(() -> {
                    TestSupport.place(helper, player, new ItemStack(Items.NETWORK_SWITCH.get()), TestSupport.DEVICE_POS);
                    TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.EAST);
                })
                .thenExecuteAfter(40, () -> TestSupport.assertTrue(helper,
                        "the switch added a device to the bus: " + computer.describe(),
                        computer.deviceCount() > base[0]))
                .thenSucceed();
    }

    /** A connector on one face of the switch, cabled to a far connector whose device is the given wire. */
    private static void host(final GameTestHelper helper, final Direction face, final RadioDockEthernetTests.Wire wire) {
        final NetworkConnectorBlockEntity near = connector(helper, SWITCH.relative(face), face);
        final NetworkConnectorBlockEntity far = connector(helper, SWITCH.relative(face, 3), face);
        NetworkConnectorBlockEntity.connect(near, far);
        helper.runAfterDelay(2, () -> far.adjacentInterface = wire); // after the first tick resolved it to nothing
    }

    private static NetworkConnectorBlockEntity connector(
            final GameTestHelper helper, final BlockPos pos, final Direction facing) {
        helper.setBlock(pos, Blocks.NETWORK_CONNECTOR.get().defaultBlockState()
                .setValue(NetworkConnectorBlock.FACING, facing));
        return TestSupport.blockEntity(helper, pos, NetworkConnectorBlockEntity.class);
    }

    private static byte[] frame(final byte[] destination, final byte[] source, final int marker) {
        final byte[] bytes = new byte[64];
        System.arraycopy(destination, 0, bytes, 0, 6);
        System.arraycopy(source, 0, bytes, 6, 6);
        bytes[12] = 0x08; // IPv4, no VLAN tag
        Arrays.fill(bytes, 14, bytes.length, (byte) marker);
        return bytes;
    }

    private static long got(final RadioDockEthernetTests.Wire wire, final int marker) {
        return wire.received.stream().filter(f -> f.length > 20 && f[20] == (byte) marker).count();
    }

    private NetworkSwitchTests() {
    }
}
