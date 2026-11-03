package li.cil.oc2.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
import net.minecraft.world.phys.Vec3;
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
public final class RadioBridgeTests {
    private static final BlockPos SOURCE = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos TARGET = SOURCE.offset(5, 0, 0);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void authenticatedEthernetFramesCrossRadioAndCableAfterTickDelay(
            final GameTestHelper helper) {
        final GpsTowerBlockEntity source = tower(helper, SOURCE);
        final GpsTowerBlockEntity target = tower(helper, TARGET);
        final var created = source.createRadioNetwork();
        final var invitation =
                source.issueRadioInvite(created.get("adminSecret"), target.getRadioEndpointId());
        target.joinRadioNetwork(invitation.get("invite"), invitation.get("secret"));
        final NetworkConnectorBlockEntity output = connector(helper, TARGET.south());
        final NetworkConnectorBlockEntity cableEnd = connector(helper, TARGET.south(3));
        NetworkConnectorBlockEntity.connect(output, cableEnd);
        final List<byte[]> received = new ArrayList<>();
        final NetworkInterface capture =
                new NetworkInterface() {
                    public byte[] readEthernetFrame() {
                        return null;
                    }

                    public void writeEthernetFrame(
                            final NetworkInterface from, final byte[] frame, final int ttl) {
                        received.add(frame.clone());
                    }
                };
        final byte[] ethernet = new byte[64];
        Arrays.fill(ethernet, (byte) 0x5a);
        helper.runAfterDelay(
                2,
                () -> {
                    cableEnd.adjacentInterface = capture;
                    source.writeEthernetFrame(capture, ethernet, 16);
                    TestSupport.assertTrue(helper, "radio ingress is delayed", received.isEmpty());
                    ethernet[0] = 0; // The authoritative queue must own a copy.
                });
        helper.runAfterDelay(
                8,
                () -> {
                    TestSupport.assertTrue(
                            helper, "one Ethernet frame reached cable", received.size() == 1);
                    TestSupport.assertTrue(
                            helper,
                            "Ethernet payload is unchanged",
                            received.get(0)[0] == 0x5a && received.get(0)[63] == 0x5a);
                    TestSupport.assertTrue(
                            helper,
                            "short address is tower endpoint",
                            !source.getRadioAddress().equals(target.getRadioAddress()));
                    source.revokeRadioEndpoint(
                            created.get("adminSecret"), target.getRadioEndpointId());
                    source.writeEthernetFrame(capture, ethernet, 16);
                });
        helper.runAfterDelay(
                14,
                () -> {
                    TestSupport.assertTrue(
                            helper, "revoked target receives no frames", received.size() == 1);
                    TestSupport.assertTrue(
                            helper,
                            "transport reports plaintext",
                            target.getRadioStatus()
                                    .get("security")
                                    .equals("trusted-local-plaintext-no-tls"));
                    helper.succeed();
                });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void gpsPairingAndGuessedIdsDoNotEnrollRadio(final GameTestHelper helper) {
        final GpsTowerBlockEntity source = tower(helper, SOURCE);
        final GpsTowerBlockEntity target = tower(helper, TARGET);
        final var created = source.createRadioNetwork();
        TestSupport.assertTrue(
                helper,
                "unpaired tower is unauthorized",
                target.getRadioStatus().get("enrolled").equals(false));
        try {
            source.issueRadioInvite("wrong-admin-secret", target.getRadioEndpointId());
            throw TestSupport.failure(helper, "administrator credential was not checked");
        } catch (final SecurityException expected) {
        }
        final var invite =
                source.issueRadioInvite(created.get("adminSecret"), target.getRadioEndpointId());
        try {
            source.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
            throw TestSupport.failure(helper, "invite intended for target enrolled source");
        } catch (final SecurityException expected) {
        }
        target.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        TestSupport.assertTrue(
                helper,
                "intended endpoint enrolled",
                target.getRadioStatus().get("enrolled").equals(true));
        try {
            target.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
            throw TestSupport.failure(helper, "one-use invitation reused");
        } catch (final SecurityException expected) {
        }
        helper.succeed();
    }


    /** The first message on a pair pays for the TLS handshake over delayed radio legs, hence the long first wait.
     * PC tower to robot over the relay tower: telemetry, a command lifecycle, signal loss and reconnect. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 200)
    public static void towerControlsRobotAndSurvivesSignalLoss(final GameTestHelper helper) {
        final GpsTowerBlockEntity pc = tower(helper, SOURCE);
        final GpsTowerBlockEntity relay = tower(helper, TARGET);
        final var created = pc.createRadioNetwork();
        final String admin = created.get("adminSecret");
        final var relayInvite = pc.issueRadioInvite(admin, relay.getRadioEndpointId());
        relay.joinRadioNetwork(relayInvite.get("invite"), relayInvite.get("secret"));

        //? if >=26.1 {
        /*final Robot robot = Entities.ROBOT.get().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        *///?} else {
        final Robot robot = Entities.ROBOT.get().create(helper.getLevel());
        //?}
        final Vec3 home = helper.absoluteVec(Vec3.atCenterOf(TARGET.offset(2, 0, 0)));
        robot.setPos(home.x, home.y, home.z);
        helper.getLevel().addFreshEntity(robot);
        final var endpoint = robot.getRadioEndpoint();
        final String robotId = endpoint.id().toString();
        final var robotInvite = pc.issueRadioInvite(admin, robotId);
        endpoint.joinRadioNetwork(robotInvite.get("invite"), robotInvite.get("secret"));
        endpoint.register("robot");
        pc.setRadioPermissions(admin, pc.getRadioEndpointId(),
                new String[] {"TELEMETRY_READ", "COMMAND_SEND"}, robotId, "robot");
        final String robotAddress = endpoint.address();
        final String command = java.util.UUID.randomUUID().toString();
        final String subscription = pc.subscribeRadioTelemetry(robotAddress, "robot", new String[] {"energy"}, 100);
        endpoint.publish("robot", "energy", "{\"stored\":42}");

        helper.runAfterDelay(40, () -> {
            final var events = pc.pollRadioEvents();
            TestSupport.assertTrue(helper, "subscriber received robot telemetry",
                    events.stream().anyMatch(e -> e.message() != null
                            && new String(e.message().payload(), java.nio.charset.StandardCharsets.UTF_8).contains("42")));
            final String state = pc.sendRadioCommand(robotAddress, "robot", "move", command, "north", 100);
            TestSupport.assertTrue(helper, "command is accepted", state.equals("ACCEPTED"));
        });
        helper.runAfterDelay(47, () -> {
            TestSupport.assertTrue(helper, "robot received the command",
                    endpoint.pollMessages().stream().anyMatch(m -> m.requestId().toString().equals(command)));
            endpoint.commandStatus(command, "RUNNING", "");
        });
        helper.runAfterDelay(54, () -> {
            TestSupport.assertTrue(helper, "running state reached the PC",
                    pc.pollRadioEvents().stream().anyMatch(e -> e.requestId().toString().equals(command)
                            && e.state() == RadioServiceBroker.State.RUNNING));
            endpoint.commandStatus(command, "COMPLETED", "ok");
        });
        helper.runAfterDelay(62, () -> {
            TestSupport.assertTrue(helper, "completed result reached the PC",
                    pc.pollRadioEvents().stream().anyMatch(e -> e.requestId().toString().equals(command)
                            && e.state() == RadioServiceBroker.State.COMPLETED));
            // Leave every tower's 45-block radius.
            robot.setPos(home.x + 200, home.y, home.z);
            boolean refused = false;
            try {
                pc.sendRadioCommand(robotAddress, "robot", "move", java.util.UUID.randomUUID().toString(), "south", 100);
            } catch (final RuntimeException expected) {
                refused = true;
            }
            TestSupport.assertTrue(helper, "command to an out-of-range robot is refused", refused);
            robot.setPos(home.x, home.y, home.z);
        });
        helper.runAfterDelay(70, () -> {
            final String again = java.util.UUID.randomUUID().toString();
            TestSupport.assertTrue(helper, "reconnected robot accepts a new command",
                    pc.sendRadioCommand(robotAddress, "robot", "move", again, "east", 100).equals("ACCEPTED"));
            pc.unsubscribeRadioTelemetry(subscription);
            helper.succeed();
        });
    }

    /** At the default 1024 B/tick a 2 KiB frame needs two ticks, so the second frame trails the first. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void linkBandwidthDelaysLargeEthernetFrames(final GameTestHelper helper) {
        final GpsTowerBlockEntity source = tower(helper, SOURCE);
        final GpsTowerBlockEntity target = tower(helper, TARGET);
        final var created = source.createRadioNetwork();
        final var invitation =
                source.issueRadioInvite(created.get("adminSecret"), target.getRadioEndpointId());
        target.joinRadioNetwork(invitation.get("invite"), invitation.get("secret"));
        final NetworkConnectorBlockEntity output = connector(helper, TARGET.south());
        final NetworkConnectorBlockEntity cableEnd = connector(helper, TARGET.south(3));
        NetworkConnectorBlockEntity.connect(output, cableEnd);
        final List<Long> arrivals = new ArrayList<>();
        final NetworkInterface capture =
                new NetworkInterface() {
                    public byte[] readEthernetFrame() {
                        return null;
                    }

                    public void writeEthernetFrame(
                            final NetworkInterface from, final byte[] frame, final int ttl) {
                        arrivals.add(helper.getLevel().getGameTime());
                    }
                };
        final long[] sentAt = {0};
        helper.runAfterDelay(
                2,
                () -> {
                    cableEnd.adjacentInterface = capture;
                    sentAt[0] = helper.getLevel().getGameTime();
                    source.writeEthernetFrame(capture, new byte[2048], 16);
                    source.writeEthernetFrame(capture, new byte[2048], 16);
                });
        helper.runAfterDelay(
                3,
                () ->
                        TestSupport.assertTrue(
                                helper, "no frame beats its airtime", arrivals.isEmpty()));
        helper.runAfterDelay(
                20,
                () -> {
                    TestSupport.assertTrue(helper, "both frames arrive", arrivals.size() == 2);
                    TestSupport.assertTrue(
                            helper,
                            "frames are spaced by the link airtime (" + arrivals + ")",
                            arrivals.get(1) - arrivals.get(0) >= 2
                                    && arrivals.get(0) - sentAt[0] >= 2);
                    helper.succeed();
                });
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void deviceListShowsOnlyOwnNetworkAndDropsRevoked(final GameTestHelper helper) {
        final GpsTowerBlockEntity pc = tower(helper, SOURCE);
        final GpsTowerBlockEntity relay = tower(helper, TARGET);
        final var created = pc.createRadioNetwork();
        final var invite = pc.issueRadioInvite(created.get("adminSecret"), relay.getRadioEndpointId());
        relay.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        final var foreignTower = tower(helper, SOURCE.offset(0, 0, 8));
        foreignTower.createRadioNetwork();
        final var peers = pc.listRadioDevices();
        TestSupport.assertTrue(helper, "own network lists two devices", peers.size() == 2);
        TestSupport.assertTrue(
                helper, "exactly one entry is self", peers.stream().filter(p -> p.self()).count() == 1);
        TestSupport.assertTrue(
                helper,
                "foreign tower is not listed",
                peers.stream().noneMatch(p -> p.address().equals(foreignTower.getRadioAddress())));
        pc.revokeRadioEndpoint(created.get("adminSecret"), relay.getRadioEndpointId());
        TestSupport.assertTrue(
                helper, "revoked device disappears", pc.listRadioDevices().size() == 1);
        helper.succeed();
    }

    private static GpsTowerBlockEntity tower(final GameTestHelper helper, final BlockPos pos) {
        TestSupport.placeFloor(helper, pos);
        TestSupport.place(
                helper, TestSupport.fakePlayer(helper), new ItemStack(Items.GPS_TOWER.get()), pos);
        return TestSupport.blockEntity(helper, pos, GpsTowerBlockEntity.class);
    }

    private static NetworkConnectorBlockEntity connector(
            final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(
                pos,
                Blocks.NETWORK_CONNECTOR
                        .get()
                        .defaultBlockState()
                        .setValue(NetworkConnectorBlock.FACING, Direction.SOUTH));
        return TestSupport.blockEntity(helper, pos, NetworkConnectorBlockEntity.class);
    }

    private RadioBridgeTests() {}
}
