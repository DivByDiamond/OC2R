package li.cil.oc2.gametest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.robot.movement.MovementDirection;
import li.cil.oc2.common.entity.robot.rotation.RotationDirection;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
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
 * The robot dock station (charger) as a 10 block access point. Everything sits inside the wide
 * test structure along one row: the PC tower, a relay tower 6 blocks on, and the dock 40 blocks
 * past the relay. The PC tower is 46 blocks from the dock, so the dock can only uplink through the
 * relay, and a robot beside the dock is outside every tower radius, so it can only reach the
 * network through the dock.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotDockRadioTests {
    /** 68 x 9 x 15 blocks of air; wide enough for a tower radius plus a dock radius. */
    static final String WIDE_TEMPLATE = "radio_wide";
    private static final int ROW = 7;
    private static final BlockPos PC = new BlockPos(4, TestSupport.WORK_Y, ROW);
    private static final BlockPos RELAY = new BlockPos(10, TestSupport.WORK_Y, ROW);
    private static final BlockPos DOCK = new BlockPos(50, TestSupport.WORK_Y, ROW);
    /** 46.5 blocks from the relay: no tower covers this dock. */
    private static final BlockPos ISOLATED_DOCK = new BlockPos(57, TestSupport.WORK_Y, ROW);

    @GameTest(template = WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void dockStationAttachesRobotAndPcReachesItThroughTower(final GameTestHelper helper) {
        final Site site = Site.build(helper, DOCK, DOCK.offset(9, 0, 0));
        final String command = UUID.randomUUID().toString(), ping = UUID.randomUUID().toString();
        final String[] subscription = new String[1];
        helper.startSequence()
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "robot 9 blocks from the dock is attached", site.robot.attached());
                    // The network creator holds every permission; narrow it to telemetry first.
                    site.grantPermissions("TELEMETRY_READ");
                    site.assertCommandRefused("command without COMMAND_SEND permission is refused");
                    site.grantPermissions("TELEMETRY_READ", "COMMAND_SEND");
                    site.robot.boot();
                    subscription[0] = site.pc.subscribeRadioTelemetry(site.robotAddress, "robot", new String[] {"energy"}, 100);
                    site.robot.endpoint().publish("robot", "energy", "{\"stored\":42}");
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "telemetry crossed the dock and the towers",
                        site.drain().stream().anyMatch(e -> e.message() != null
                                && e.requestId().toString().equals(subscription[0])
                                && new String(e.message().payload(), StandardCharsets.UTF_8).contains("42"))))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "device list shows the robot online", site.listedOnline());
                    final RadioServiceBroker.Estimate estimate = site.pc.estimateRadioRoute(site.robotAddress);
                    // PC tower to relay, relay to dock, dock to robot.
                    TestSupport.assertEquals(helper, "radio legs through the dock", 3, estimate.hops());
                    TestSupport.assertTrue(helper, "delivery delay stays within the cap (" + estimate.nominalTicks() + ")",
                            estimate.nominalTicks() >= 1 && estimate.nominalTicks() <= Config.radioMaxDeliveryTicks);
                    site.pc.pingRadio(site.robotAddress, ping);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "ping is answered over the dock",
                        site.saw(ping, RadioServiceBroker.State.COMPLETED)))
                .thenWaitUntil(site.robot::assertRunning)
                .thenExecute(() -> TestSupport.assertTrue(helper, "command is accepted",
                        site.sendCommand(command, "forward").equals("ACCEPTED")))
                .thenWaitUntil(() -> site.assertReceived(command))
                .thenExecute(() -> site.robot.endpoint().bindCommandAction(command, site.robot.move(MovementDirection.FORWARD)))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "command completes after the real move",
                        site.saw(command, RadioServiceBroker.State.COMPLETED)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "completed command was never reported failed",
                            !site.saw(command, RadioServiceBroker.State.FAILED));
                    TestSupport.assertTrue(helper, "robot really moved one block",
                            Math.abs(site.robot.robot.position().distanceTo(site.robot.home) - 1) < 1e-3);
                    // Permissions are rechecked for a robot behind a dock just as behind a tower.
                    site.grantPermissions("TELEMETRY_READ");
                    site.assertCommandRefused("command after COMMAND_SEND was withdrawn is refused");
                    // The attachment exists only because of the dock.
                    TestSupport.breakBlock(helper, DOCK);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "robot detaches once the dock is gone",
                        !site.robot.attached() && !site.listedOnline()))
                .thenSucceed();
    }

    @GameTest(template = WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void robotJustOutsideDockRadiusDoesNotAttach(final GameTestHelper helper) {
        // The robot's centre is exactly 10 blocks from the dock's centre.
        final Site site = Site.build(helper, DOCK, DOCK.offset(10, 0, 0));
        site.grantPermissions("TELEMETRY_READ", "COMMAND_SEND");
        helper.startSequence()
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "robot exactly on the 10 block radius attaches", site.robot.attached());
                    TestSupport.assertTrue(helper, "device list shows the robot online", site.listedOnline());
                    site.robot.placeAtOffset(0.01);
                    TestSupport.assertTrue(helper, "robot 10.01 blocks from the dock does not attach", !site.robot.attached());
                    TestSupport.assertTrue(helper, "device list shows the robot offline", !site.listedOnline());
                    site.assertCommandRefused("command to an unattached robot is refused");
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void dockWithoutTowerInRangeIsIsolated(final GameTestHelper helper) {
        final Site site = Site.build(helper, ISOLATED_DOCK, ISOLATED_DOCK.offset(2, 0, 0));
        site.grantPermissions("TELEMETRY_READ", "COMMAND_SEND");
        helper.startSequence()
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "a dock without a tower in range attaches nothing", !site.robot.attached());
                    site.assertCommandRefused("command through an isolated dock is refused");
                    // A dock that a tower covers, 9 blocks from the same robot, does attach it.
                    site.placeDock(DOCK);
                    TestSupport.assertTrue(helper, "robot attaches through a dock with a tower uplink", site.robot.attached());
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void leavingDockRadiusLosesSignalAndReturningReconnects(final GameTestHelper helper) {
        final Site site = Site.build(helper, DOCK, DOCK.offset(9, 0, 0));
        site.grantPermissions("TELEMETRY_READ", "COMMAND_SEND");
        final String lost = UUID.randomUUID().toString(), after = UUID.randomUUID().toString();
        final int[] actionsBefore = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    site.robot.boot();
                    site.pc.subscribeRadioTelemetry(site.robotAddress, "robot", new String[] {"energy"}, 100);
                    site.robot.endpoint().publish("robot", "energy", "{}");
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "telemetry arrives over the dock",
                        site.drain().stream().anyMatch(e -> e.message() != null)))
                .thenWaitUntil(site.robot::assertRunning)
                .thenExecute(() -> {
                    actionsBefore[0] = site.robot.lastActionId();
                    TestSupport.assertTrue(helper, "command is accepted", site.sendCommand(lost, "forward").equals("ACCEPTED"));
                    // Signal loss: the robot leaves the 10 block dock radius before the command arrives.
                    site.robot.placeAtOffset(3);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "command during signal loss ends DISCONNECTED or TIMED_OUT",
                        site.saw(lost, RadioServiceBroker.State.DISCONNECTED) || site.saw(lost, RadioServiceBroker.State.TIMED_OUT)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "device list shows the robot offline", !site.listedOnline());
                    site.assertCommandRefused("command to a robot outside the dock radius is refused");
                    site.robot.placeAtOffset(0);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "returning robot attaches again",
                        site.robot.attached() && site.listedOnline()))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "no robot action ran during signal loss",
                            site.robot.lastActionId() == actionsBefore[0]);
                    try {
                        site.robot.endpoint().bindCommandAction(lost, site.robot.lastActionId());
                        throw TestSupport.failure(helper, "the lost command is still known after the reconnect");
                    } catch (final IllegalArgumentException expected) {
                    }
                    TestSupport.assertTrue(helper, "reconnected robot accepts a new command",
                            site.sendCommand(after, "forward").equals("ACCEPTED"));
                })
                .thenWaitUntil(() -> site.assertReceived(after))
                .thenExecute(() -> site.robot.endpoint().bindCommandAction(after, site.robot.move(MovementDirection.FORWARD)))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "new command completes after the reconnect",
                        site.saw(after, RadioServiceBroker.State.COMPLETED)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the lost command was never delivered", !site.received(lost));
                    TestSupport.assertTrue(helper, "the lost command never got a result",
                            !site.saw(lost, RadioServiceBroker.State.COMPLETED) && !site.saw(lost, RadioServiceBroker.State.FAILED));
                    // Only the new command's move ran; nothing was replayed after the reconnect.
                    TestSupport.assertEquals(helper, "robot actions since the lost command", 1,
                            site.robot.lastActionId() - actionsBefore[0]);
                    TestSupport.assertTrue(helper, "robot moved exactly one block",
                            Math.abs(site.robot.robot.position().distanceTo(site.robot.home) - 1) < 1e-3);
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void commandResultsFollowTheRealActionOutcome(final GameTestHelper helper) {
        final Site site = Site.build(helper, DOCK, DOCK.offset(9, 0, 0));
        site.grantPermissions("TELEMETRY_READ", "COMMAND_SEND");
        final String command = UUID.randomUUID().toString(), other = UUID.randomUUID().toString();
        final int[] earlier = new int[1], blocked = new int[1];
        helper.startSequence()
                .thenExecute(site.robot::boot)
                .thenWaitUntil(site.robot::assertRunning)
                .thenExecute(() -> {
                    earlier[0] = site.robot.turn(RotationDirection.LEFT);
                    TestSupport.assertTrue(helper, "command is accepted", site.sendCommand(command, "down").equals("ACCEPTED"));
                    TestSupport.assertTrue(helper, "second command is accepted", site.sendCommand(other, "down").equals("ACCEPTED"));
                })
                .thenWaitUntil(() -> {
                    site.assertReceived(command);
                    site.assertReceived(other);
                })
                .thenExecute(() -> {
                    for (final String nominal : new String[] {"COMPLETED", "FAILED"}) {
                        try {
                            site.robot.endpoint().commandStatus(command, nominal, "claimed");
                            throw TestSupport.failure(helper, "guest declared " + nominal);
                        } catch (final IllegalArgumentException expected) {
                        }
                    }
                    expectBindRefused(helper, site, command, earlier[0], "an action queued before the command arrived");
                    expectBindRefused(helper, site, command, site.robot.lastActionId() + 1, "an action not queued yet");
                    // Moving down into the stone floor cannot succeed; the robot reports FAILURE itself.
                    blocked[0] = site.robot.move(MovementDirection.DOWNWARD);
                    site.robot.endpoint().bindCommandAction(command, blocked[0]);
                    try {
                        site.robot.endpoint().bindCommandAction(other, blocked[0]);
                        throw TestSupport.failure(helper, "one action was bound to two commands");
                    } catch (final IllegalStateException expected) {
                    }
                    site.drain();
                    TestSupport.assertTrue(helper, "no result is invented before the action ends",
                            !site.saw(command, RadioServiceBroker.State.COMPLETED) && !site.saw(command, RadioServiceBroker.State.FAILED));
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "blocked move is reported FAILED",
                        site.saw(command, RadioServiceBroker.State.FAILED)))
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "blocked move is never reported COMPLETED",
                            !site.saw(command, RadioServiceBroker.State.COMPLETED));
                    TestSupport.assertTrue(helper, "the robot itself reports the move as failed",
                            site.robot.robot.getMovementController().findActionResult(blocked[0])
                                    == li.cil.oc2.common.entity.robot.action.RobotActionResult.FAILURE);
                    TestSupport.assertTrue(helper, "robot is back on its block",
                            site.robot.robot.position().distanceTo(site.robot.home) < 1e-3);
                    TestSupport.assertTrue(helper, "the unbound command got no result",
                            !site.saw(other, RadioServiceBroker.State.COMPLETED) && !site.saw(other, RadioServiceBroker.State.FAILED));
                })
                .thenSucceed();
    }

    private static void expectBindRefused(
            final GameTestHelper helper, final Site site, final String command, final int actionId, final String what) {
        try {
            site.robot.endpoint().bindCommandAction(command, actionId);
            throw TestSupport.failure(helper, "command was bound to " + what);
        } catch (final IllegalArgumentException expected) {
        }
    }

    /** The towers, the dock and one enrolled robot of a test, plus what the PC and robot received. */
    private static final class Site {
        final GameTestHelper helper;
        final GpsTowerBlockEntity pc;
        final String admin;
        final RadioTestRobot robot;
        final String robotId;
        final String robotAddress;
        private final List<RadioServiceBroker.Event> events = new ArrayList<>();
        private final List<RadioMessage> inbox = new ArrayList<>();

        private Site(final GameTestHelper helper, final GpsTowerBlockEntity pc, final String admin, final RadioTestRobot robot) {
            this.helper = helper;
            this.pc = pc;
            this.admin = admin;
            this.robot = robot;
            this.robotId = robot.endpoint().id().toString();
            final var invite = pc.issueRadioInvite(admin, robotId);
            robot.endpoint().joinRadioNetwork(invite.get("invite"), invite.get("secret"));
            robot.endpoint().register("robot");
            this.robotAddress = robot.endpoint().address();
        }

        static Site build(final GameTestHelper helper, final BlockPos dock, final BlockPos robotBlock) {
            final GpsTowerBlockEntity pc = RadioBridgeTests.tower(helper, PC);
            final GpsTowerBlockEntity relay = RadioBridgeTests.tower(helper, RELAY);
            final String admin = pc.createRadioNetwork().get("adminSecret");
            final var relayInvite = pc.issueRadioInvite(admin, relay.getRadioEndpointId());
            relay.joinRadioNetwork(relayInvite.get("invite"), relayInvite.get("secret"));
            placeDock(helper, dock);
            return new Site(helper, pc, admin, RadioTestRobot.spawn(helper, robotBlock));
        }

        private static void placeDock(final GameTestHelper helper, final BlockPos pos) {
            TestSupport.placeFloor(helper, pos);
            helper.setBlock(pos, Blocks.CHARGER.get().defaultBlockState());
        }

        void placeDock(final BlockPos pos) {
            placeDock(helper, pos);
        }

        void grantPermissions(final String... permissions) {
            pc.setRadioPermissions(admin, pc.getRadioEndpointId(), permissions, robotId, "robot");
        }

        String sendCommand(final String request, final String payload) {
            return pc.sendRadioCommand(robotAddress, "robot", "move", request, payload, 200);
        }

        void assertCommandRefused(final String what) {
            boolean refused = false;
            try {
                sendCommand(UUID.randomUUID().toString(), "forward");
            } catch (final RuntimeException expected) {
                refused = true;
            }
            TestSupport.assertTrue(helper, what, refused);
        }

        boolean listedOnline() {
            return pc.listRadioDevices().stream().anyMatch(peer -> peer.address().equals(robotAddress) && peer.online());
        }

        List<RadioServiceBroker.Event> drain() {
            events.addAll(pc.pollRadioEvents());
            return events;
        }

        boolean saw(final String request, final RadioServiceBroker.State state) {
            return drain().stream().anyMatch(e -> e.requestId().toString().equals(request) && e.state() == state);
        }

        boolean received(final String request) {
            inbox.addAll(robot.endpoint().pollMessages());
            return inbox.stream().anyMatch(m -> m.requestId().toString().equals(request));
        }

        void assertReceived(final String request) {
            TestSupport.assertTrue(helper, "robot received command " + request, received(request));
        }
    }

    private RobotDockRadioTests() {}
}
