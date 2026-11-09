package li.cil.oc2.gametest;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.WirelessAccessPointBlock;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.blockentity.network.WirelessAccessPointBlockEntity;
import li.cil.oc2.common.bus.adapter.rpc.RPCInvocationImpl;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
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

/** The AP's own enrolled identity, exercised through actual ObjectDevice callback discovery. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AccessPointRadioTests {
    private static final BlockPos TOWER = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos POINT = TOWER.offset(8, 2, 0);
    private static final String CHAT = "chat";

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void accessPointOwnRpcExchangesMessagesAndPing(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String request = UUID.randomUUID().toString(), reply = UUID.randomUUID().toString();
        final String ping = UUID.randomUUID().toString();
        final List<RadioServiceBroker.Event> events = new ArrayList<>();
        helper.startSequence().thenExecute(() -> {
            site.enroll();
            TestSupport.assertTrue(helper, "AP has its own ORA address", !site.address.equals(site.pc.getRadioAddress()));
            TestSupport.assertTrue(helper, "ochat discovers AP radio", site.rpc.getTypeNames().contains("gps_tower"));
            site.call("sendRadioMessage", site.pc.getRadioAddress(), CHAT, "text", request, "from AP");
        }).thenWaitUntil(() -> TestSupport.assertTrue(helper, "AP message reaches tower PC",
                site.pc.pollRadioMessages().stream().anyMatch(message -> message.requestId().toString().equals(request))))
                .thenExecute(() -> site.pc.sendRadioMessage(site.address, CHAT, "text", reply, "from PC"))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "tower PC message reaches AP guest",
                        site.messages().stream().anyMatch(message -> message.requestId().toString().equals(reply))))
                .thenExecute(() -> site.call("pingRadio", site.pc.getRadioAddress(), ping))
                .thenWaitUntil(() -> {
                    events.addAll(site.events());
                    TestSupport.assertTrue(helper, "AP ping completes", events.stream().anyMatch(event ->
                            event.requestId().toString().equals(ping) && event.state() == RadioServiceBroker.State.COMPLETED));
                }).thenExecute(site::finish).thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void accessPointRpcPublishesTelemetryAndCompletesGuestCommands(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String command = UUID.randomUUID().toString();
        final String[] subscription = new String[1];
        final List<RadioServiceBroker.Event> events = new ArrayList<>();
        helper.startSequence().thenExecute(() -> {
            site.enroll();
            subscription[0] = site.pc.subscribeRadioTelemetry(site.address, CHAT, new String[] {"energy"}, 100);
            site.call("publishRadioTelemetry", CHAT, "energy", "7");
        }).thenWaitUntil(() -> {
            events.addAll(site.pc.pollRadioEvents());
            TestSupport.assertTrue(helper, "PC receives AP telemetry", events.stream().anyMatch(event ->
                    event.requestId().toString().equals(subscription[0]) && event.message() != null));
        }).thenExecute(() -> site.pc.sendRadioCommand(site.address, CHAT, "work", command, "run", 100))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "AP guest receives command",
                        site.messages().stream().anyMatch(message -> message.requestId().toString().equals(command))))
                .thenExecute(() -> site.call("updateRadioCommand", command, "COMPLETED", "guest result"))
                .thenWaitUntil(() -> {
                    events.addAll(site.pc.pollRadioEvents());
                    TestSupport.assertTrue(helper, "PC receives actual guest completion", events.stream().anyMatch(event ->
                            event.requestId().toString().equals(command) && event.state() == RadioServiceBroker.State.COMPLETED));
                }).thenExecute(site::finish).thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void stoppedApGuestDisconnectsCommandsButKeepsInfrastructureAndSavedServices(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String command = UUID.randomUUID().toString();
        final List<RadioServiceBroker.Event> events = new ArrayList<>();
        helper.startSequence().thenExecute(() -> {
            site.enroll();
            site.pc.sendRadioCommand(site.address, CHAT, "work", command, "run", 100);
        }).thenWaitUntil(() -> TestSupport.assertTrue(helper, "command reached AP before guest stopped",
                site.messages().stream().anyMatch(message -> message.requestId().toString().equals(command))))
                .thenExecute(() -> {
                    site.rpc.unmount();
                    site.point.serverTick();
                    TestSupport.assertTrue(helper, "AP Ethernet and coverage remain attached",
                            Boolean.TRUE.equals(site.point.getRadioStatus().get("attached")));
                    TestSupport.assertTrue(helper, "stopped guest is not online",
                            Boolean.FALSE.equals(site.point.getRadioStatus().get("guestOnline")));
                    TestSupport.assertTrue(helper, "guest callbacks reject stopped bus", site.refused("listRadioDevices"));
                }).thenWaitUntil(() -> {
                    events.addAll(site.pc.pollRadioEvents());
                    TestSupport.assertTrue(helper, "pending command disconnects", events.stream().anyMatch(event ->
                            event.requestId().toString().equals(command)
                                    && event.state() == RadioServiceBroker.State.DISCONNECTED));
                }).thenExecute(() -> {
                    final var saved = site.point.saveWithoutMetadata(helper.getLevel().registryAccess());
                    helper.setBlock(POINT, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    helper.setBlock(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
                    site.point = TestSupport.blockEntity(helper, POINT, WirelessAccessPointBlockEntity.class);
                    site.point.loadAdditional(saved, helper.getLevel().registryAccess());
                    site.rpc = new ObjectDevice(site.point);
                    site.rpc.mount();
                    site.point.serverTick();
                    TestSupport.assertTrue(helper, "address survives save and reload",
                            site.address.equals(site.call("getRadioAddress")));
                    TestSupport.assertTrue(helper, "named service survives save and reload",
                            site.pc.discoverRadioServices(site.address).contains(CHAT));
                    TestSupport.assertTrue(helper, "old command is not replayed", site.messages().isEmpty());
                    TestSupport.assertTrue(helper, "old command cannot complete after restart",
                            site.refused("updateRadioCommand", command, "COMPLETED", "late"));
                    site.finish();
                }).thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void clonedAndRevokedAccessPointsCannotBorrowAuthenticatedRpc(final GameTestHelper helper) {
        final Site site = new Site(helper);
        site.enroll();
        final var saved = site.point.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(POINT.east(2), Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var copy = TestSupport.blockEntity(helper, POINT.east(2), WirelessAccessPointBlockEntity.class);
        copy.loadAdditional(saved, helper.getLevel().registryAccess());
        final var clone = new ObjectDevice(copy);
        clone.mount();
        copy.serverTick();
        TestSupport.assertTrue(helper, "clone has no authenticated callbacks", refused(() -> copy.listRadioDevices())
                && refused(() -> copy.getRadioAddress()) && refused(() -> copy.registerRadioService(CHAT))
                && refused(() -> copy.pollRadioMessages()) && refused(() -> copy.pollRadioEvents())
                && refused(() -> copy.sendRadioMessage(site.address, CHAT, "text", UUID.randomUUID().toString(), "x")));
        TestSupport.assertTrue(helper, "clone is not attached or lit", !Boolean.TRUE.equals(copy.getRadioStatus().get("attached"))
                && !copy.getBlockState().getValue(WirelessAccessPointBlock.LIT));
        TestSupport.assertTrue(helper, "original remains online", site.point.listRadioDevices().stream()
                .anyMatch(peer -> peer.address().equals(site.address) && peer.online()));
        site.pc.revokeRadioEndpoint(site.admin, site.point.getRadioEndpointId());
        site.point.serverTick();
        TestSupport.assertTrue(helper, "revocation denies authenticated callbacks", site.refused("listRadioDevices")
                && site.refused("getRadioAddress") && site.refused("registerRadioService", CHAT));
        TestSupport.assertTrue(helper, "revoked AP indicator is off", !site.point.getBlockState().getValue(WirelessAccessPointBlock.LIT));
        clone.unmount();
        site.finish();
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void runningComputerDiscoversAndUsesAccessPointThroughBusCableBelowIt(final GameTestHelper helper) {
        final var player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final var computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        TestSupport.place(helper, player, new ItemStack(Items.BUS_CABLE.get()), TestSupport.CABLE_POS);
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.WEST);
        final BlockPos pointPos = TestSupport.CABLE_POS.above();
        helper.setBlock(pointPos, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        TestSupport.connectInterface(helper, TestSupport.CABLE_POS, Direction.UP);
        final var point = TestSupport.blockEntity(helper, pointPos, WirelessAccessPointBlockEntity.class);
        final var tower = RadioBridgeTests.tower(helper, new BlockPos(8, TestSupport.WORK_Y, 8));
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final var invitation = tower.issueRadioInviteWithPermissions(admin, point.getRadioEndpointId(),
                new String[] {"MESSAGE_SEND"}, tower.getRadioEndpointId(), CHAT);
        point.joinRadioNetwork(invitation.get("invite"), invitation.get("secret"));
        tower.registerRadioService(CHAT);
        final String request = UUID.randomUUID().toString();
        final RPCDevice[] attached = new RPCDevice[1];
        helper.startSequence().thenExecute(computer::start)
                .thenWaitUntil(() -> {
                    attached[0] = computer.devices().stream()
                            .filter(device -> device instanceof RPCDevice rpc
                                    && rpc.getTypeNames().contains("wireless_access_point"))
                            .map(device -> (RPCDevice) device).findFirst().orElse(null);
                    TestSupport.assertTrue(helper, "running guest mounts the AP through the cable below it",
                            computer.virtualMachine().isRunning() && attached[0] != null
                                    && Boolean.TRUE.equals(point.getRadioStatus().get("guestOnline")));
                }).thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the discovered AP exposes messenger RPC", attached[0].getTypeNames().contains("gps_tower"));
                    invoke(attached[0], "registerRadioService", CHAT);
                    invoke(attached[0], "sendRadioMessage", tower.getRadioAddress(), CHAT, "text", request, "below bus");
                }).thenWaitUntil(() -> TestSupport.assertTrue(helper, "real computer AP callback sends to tower",
                        tower.pollRadioMessages().stream().anyMatch(message -> message.requestId().toString().equals(request))))
                .thenExecute(computer::stop)
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "stopped computer leaves AP infrastructure on but guest off",
                        Boolean.TRUE.equals(point.getRadioStatus().get("attached"))
                                && Boolean.FALSE.equals(point.getRadioStatus().get("guestOnline"))))
                .thenSucceed();
    }

    @GameTest(template = RobotDockRadioTests.WIDE_TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void accessPointCannotUplinkThroughAnotherAccessPoint(final GameTestHelper helper) {
        final BlockPos towerPos = new BlockPos(4, TestSupport.WORK_Y, 7);
        final var tower = RadioBridgeTests.tower(helper, towerPos);
        final String admin = tower.createRadioNetwork().get("adminSecret");
        final BlockPos firstPos = towerPos.offset(42, 0, 0), secondPos = towerPos.offset(50, 0, 0);
        helper.setBlock(firstPos, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        helper.setBlock(secondPos, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
        final var first = TestSupport.blockEntity(helper, firstPos, WirelessAccessPointBlockEntity.class);
        final var second = TestSupport.blockEntity(helper, secondPos, WirelessAccessPointBlockEntity.class);
        for (final var point : new WirelessAccessPointBlockEntity[] {first, second}) {
            final var invitation = tower.issueRadioInvite(admin, point.getRadioEndpointId());
            point.joinRadioNetwork(invitation.get("invite"), invitation.get("secret"));
            point.onDeviceMounted();
            point.serverTick();
        }
        TestSupport.assertTrue(helper, "first AP has a direct tower uplink", Boolean.TRUE.equals(first.getRadioStatus().get("attached")));
        TestSupport.assertTrue(helper, "AP beyond tower range cannot chain through neighboring AP",
                Boolean.FALSE.equals(second.getRadioStatus().get("attached"))
                        && Boolean.FALSE.equals(second.getRadioStatus().get("guestOnline"))
                        && !second.getBlockState().getValue(WirelessAccessPointBlock.LIT));
        TestSupport.assertTrue(helper, "out-of-range AP cannot send guest radio traffic",
                refused(() -> second.pingRadio(first.getRadioAddress(), UUID.randomUUID().toString())));
        first.onDeviceUnmounted();
        second.onDeviceUnmounted();
        first.serverTick();
        second.serverTick();
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void immediateGuestRemountCannotDeliverPreviousCommandsOrMessages(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String command = UUID.randomUUID().toString(), message = UUID.randomUUID().toString();
        final List<RadioServiceBroker.Event> events = new ArrayList<>();
        helper.startSequence().thenExecute(() -> {
            site.enroll();
            site.pc.sendRadioCommand(site.address, CHAT, "work", command, "old", 100);
            site.pc.sendRadioMessage(site.address, CHAT, "text", message, "old");
            site.rpc.unmount();
            site.rpc.mount();
            // No server tick between stop, remount and the first RPC: it must drain pending cleanup itself.
            TestSupport.assertTrue(helper, "immediate remount has no old inbox", site.messages().isEmpty());
        }).thenIdle(15).thenExecute(() -> {
            TestSupport.assertTrue(helper, "in-flight old traffic is not delivered after remount", site.messages().isEmpty());
            events.addAll(site.pc.pollRadioEvents());
            TestSupport.assertTrue(helper, "pre-stop command is disconnected", events.stream().anyMatch(event ->
                    event.requestId().toString().equals(command) && event.state() == RadioServiceBroker.State.DISCONNECTED));
            TestSupport.assertTrue(helper, "old request cannot report completion", site.refused("updateRadioCommand", command, "COMPLETED", "late"));
            site.finish();
        }).thenSucceed();
    }

    private static Object invoke(final RPCDevice rpc, final String method, final Object... parameters) {
        final var gson = new Gson();
        final var invocation = new RPCInvocationImpl(JsonParser.parseString(gson.toJson(parameters)).getAsJsonArray(), gson);
        final var callback = rpc.getMethodGroups().stream().filter(group -> group.getName().equals(method))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Missing AP callback " + method));
        try {
            return callback.findOverload(invocation).orElseThrow().invoke(invocation);
        } catch (final Throwable failure) { // NOPMD - RPC invocation declares Throwable.
            throw new IllegalStateException("AP callback failed: " + method, failure);
        }
    }

    private static boolean refused(final Runnable call) {
        try {
            call.run();
            return false;
        } catch (final RuntimeException expected) { // NOPMD - rejection is the result.
            return true;
        }
    }

    private static final class Site {
        private static final Gson GSON = new Gson();
        final GameTestHelper helper;
        final GpsTowerBlockEntity pc;
        final String admin;
        WirelessAccessPointBlockEntity point;
        ObjectDevice rpc;
        String address;

        Site(final GameTestHelper helper) {
            this.helper = helper;
            pc = RadioBridgeTests.tower(helper, TOWER);
            admin = pc.createRadioNetwork().get("adminSecret");
            helper.setBlock(POINT, Blocks.WIRELESS_ACCESS_POINT.get().defaultBlockState());
            point = TestSupport.blockEntity(helper, POINT, WirelessAccessPointBlockEntity.class);
            rpc = new ObjectDevice(point);
            rpc.mount();
        }

        void enroll() {
            final var invitation = pc.issueRadioInviteWithPermissions(admin, point.getRadioEndpointId(),
                    new String[] {"MESSAGE_SEND", "TELEMETRY_READ", "COMMAND_SEND"}, pc.getRadioEndpointId(), CHAT);
            call("joinRadioNetwork", invitation.get("invite"), invitation.get("secret"));
            address = (String) call("getRadioAddress");
            pc.setRadioPermissions(admin, pc.getRadioEndpointId(),
                    new String[] {"MESSAGE_SEND", "TELEMETRY_READ", "COMMAND_SEND"}, point.getRadioEndpointId(), CHAT);
            pc.registerRadioService(CHAT);
            call("registerRadioService", CHAT);
            point.serverTick();
            TestSupport.assertTrue(helper, "connected AP enables its radio pulse texture",
                    point.getBlockState().getValue(WirelessAccessPointBlock.LIT));
        }

        Object call(final String method, final Object... parameters) {
            final var invocation = new RPCInvocationImpl(JsonParser.parseString(GSON.toJson(parameters)).getAsJsonArray(), GSON);
            final var callback = rpc.getMethodGroups().stream().filter(group -> group.getName().equals(method))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Missing AP callback " + method));
            try {
                return callback.findOverload(invocation).orElseThrow().invoke(invocation);
            } catch (final Throwable failure) { // NOPMD - RPC invocation declares Throwable.
                throw new IllegalStateException("AP callback failed: " + method, failure);
            }
        }

        @SuppressWarnings("unchecked")
        List<RadioMessage> messages() {
            return (List<RadioMessage>) call("pollRadioMessages");
        }

        @SuppressWarnings("unchecked")
        List<RadioServiceBroker.Event> events() {
            return (List<RadioServiceBroker.Event>) call("pollRadioEvents");
        }

        boolean refused(final String method, final Object... parameters) {
            return AccessPointRadioTests.refused(() -> call(method, parameters));
        }

        void finish() {
            rpc.unmount();
            point.serverTick();
        }
    }

    private AccessPointRadioTests() {}
}
