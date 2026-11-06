package li.cil.oc2.gametest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

//?}

/**
 * A PC's wired Ethernet network joins the radio through a robot dock station exactly as it does
 * through a tower: blue cable, network connectors, and an enrolled access point. Each access point
 * has a connector next to it and a second connector three blocks on that stands for the far end of
 * the cable; a Wire there injects and records frames.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RadioDockEthernetTests {
    private static final BlockPos TOWER = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos RELAY = TOWER.offset(5, 0, 0);
    private static final BlockPos DOCK = TOWER.offset(10, 0, 0);

    /** Far end of a cable: frames queued in out enter the cable, received records what left it. */
    static final class Wire implements NetworkInterface {
        final ArrayDeque<byte[]> out = new ArrayDeque<>();
        final List<byte[]> received = new ArrayList<>();

        @Override
        public byte[] readEthernetFrame() {
            return out.poll();
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface from, final byte[] frame, final int ttl) {
            received.add(frame.clone());
        }

        boolean got(final int fill) {
            return received.stream().anyMatch(frame -> frame[0] == (byte) fill);
        }
    }

    static byte[] frame(final int fill) {
        final byte[] bytes = new byte[64];
        Arrays.fill(bytes, (byte) fill);
        return bytes;
    }

    private static ChargerBlockEntity dock(final GameTestHelper helper, final BlockPos pos) {
        TestSupport.placeFloor(helper, pos);
        helper.setBlock(pos, Blocks.CHARGER.get().defaultBlockState());
        return TestSupport.blockEntity(helper, pos, ChargerBlockEntity.class);
    }

    /** A connector beside the host and a far end three blocks on, joined by a cable. */
    private static NetworkConnectorBlockEntity cable(final GameTestHelper helper, final BlockPos host, final Wire far) {
        final NetworkConnectorBlockEntity near = RadioBridgeTests.connector(helper, host.south());
        final NetworkConnectorBlockEntity end = RadioBridgeTests.connector(helper, host.south(3));
        NetworkConnectorBlockEntity.connect(near, end);
        helper.runAfterDelay(2, () -> end.adjacentInterface = far); // after the first tick resolved it to nothing
        return near;
    }

    private static void enroll(final GpsTowerBlockEntity tower, final String admin, final ChargerBlockEntity dock) {
        final var invite = tower.issueRadioInvite(admin, dock.getRadioEndpointId());
        dock.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void dockStationBridgesCableEthernetToTheTowerNetworkBothWays(final GameTestHelper helper) {
        final GpsTowerBlockEntity tower = RadioBridgeTests.tower(helper, TOWER);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final ChargerBlockEntity dock = dock(helper, DOCK);
        enroll(tower, admin, dock);
        final Wire dockFar = new Wire();
        final Wire towerFar = new Wire();
        final NetworkConnectorBlockEntity dockNear = cable(helper, DOCK, dockFar);
        cable(helper, TOWER, towerFar);
        final long[] injected = new long[1];
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the connector on the dock resolves the dock as its network interface",
                            dockNear.adjacentInterface == dock); // NOPMD - identity of the capability
                    TestSupport.assertTrue(helper, "dock is enrolled and attached to the tower",
                            dock.getRadioStatus().get("enrolled").equals(true) && dock.getRadioStatus().get("attached").equals(true));
                    injected[0] = helper.getLevel().getGameTime();
                    dockFar.out.add(frame(0x11));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "a frame from the dock's cable reaches the tower's cable",
                        towerFar.got(0x11)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "radio delivery is delayed by at least one tick",
                            helper.getLevel().getGameTime() > injected[0]);
                    TestSupport.assertEquals(helper, "tower cable copies", 1, towerFar.received.size());
                    towerFar.out.add(frame(0x22));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "a frame from the tower's cable reaches the dock's cable",
                        dockFar.got(0x22)))
                .thenIdle(10)
                .thenExecute(() -> {
                    TestSupport.assertEquals(helper, "dock cable copies", 1, dockFar.received.size());
                    TestSupport.assertTrue(helper, "no frame echoes back to its own cable",
                            !dockFar.got(0x11) && !towerFar.got(0x22) && towerFar.received.size() == 1);
                })
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void unenrolledAndRevokedDocksBridgeNothing(final GameTestHelper helper) {
        final GpsTowerBlockEntity tower = RadioBridgeTests.tower(helper, TOWER);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final ChargerBlockEntity dock = dock(helper, DOCK);
        final Wire dockFar = new Wire();
        final Wire towerFar = new Wire();
        cable(helper, DOCK, dockFar);
        cable(helper, TOWER, towerFar);
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    dockFar.out.add(frame(0x31));
                    towerFar.out.add(frame(0x32));
                })
                .thenIdle(12)
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "a cabled but unenrolled dock sends nothing into the network",
                            towerFar.received.isEmpty());
                    TestSupport.assertTrue(helper, "and receives nothing from it", dockFar.received.isEmpty());
                    enroll(tower, admin, dock);
                    dockFar.out.add(frame(0x33));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "after enrollment the same cable bridges",
                        towerFar.got(0x33)))
                .thenExecute(() -> {
                    tower.revokeRadioEndpoint(admin, dock.getRadioEndpointId());
                    dockFar.out.add(frame(0x34));
                    towerFar.out.add(frame(0x35));
                })
                .thenIdle(12)
                .thenExecute(() -> TestSupport.assertTrue(helper, "a revoked dock bridges nothing in either direction",
                        !towerFar.got(0x34) && !dockFar.got(0x35)))
                .thenSucceed();
    }

    /** A wired loop between a tower and a dock must not multiply a frame inside the radio network. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void wiredLoopBetweenTowerAndDockDeliversOneCopy(final GameTestHelper helper) {
        final GpsTowerBlockEntity tower = RadioBridgeTests.tower(helper, TOWER);
        final GpsTowerBlockEntity relay = RadioBridgeTests.tower(helper, RELAY);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final var relayInvite = tower.issueRadioInvite(admin, relay.getRadioEndpointId());
        relay.joinRadioNetwork(relayInvite.get("invite"), relayInvite.get("secret"));
        final ChargerBlockEntity dock = dock(helper, DOCK);
        enroll(tower, admin, dock);
        final Wire tap = new Wire();
        final Wire relayFar = new Wire();
        final NetworkConnectorBlockEntity dockNear = cable(helper, DOCK, tap);
        cable(helper, RELAY, relayFar);
        final NetworkConnectorBlockEntity towerNear = RadioBridgeTests.connector(helper, TOWER.south());
        NetworkConnectorBlockEntity.connect(towerNear, dockNear);
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> tower.writeEthernetFrame(tap, frame(0x41), 16))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "the frame crosses the radio to the relay's cable",
                        relayFar.got(0x41)))
                .thenIdle(25)
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the frame really travelled around the loop through the dock's cable",
                            tap.got(0x41));
                    TestSupport.assertEquals(helper, "copies at the relay despite the loop", 1, relayFar.received.size());
                })
                .thenSucceed();
    }

    private RadioDockEthernetTests() {}
}
