package li.cil.oc2.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.blockentity.radio.OraEndpoint;
import li.cil.oc2.common.blockentity.radio.RadioNetworkData;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioClientLeases;
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
 * Credential lifecycle, request idempotency and latency reporting on the real radio stack: two
 * towers, and clients that are plain enrolled radio endpoints (the host of a client, for example a
 * tablet item, drives its tick itself).
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RadioIdentityTests {
    private static final BlockPos PC = new BlockPos(2, TestSupport.WORK_Y, 2);
    private static final BlockPos RELAY = PC.offset(5, 0, 0);

    /** Two towers and a way to ask the PC tower what it received. */
    private static final class Site {
        final GameTestHelper helper;
        final GpsTowerBlockEntity pc;
        final String admin;
        final List<RadioServiceBroker.Event> events = new ArrayList<>();
        final List<OraEndpoint> clients = new ArrayList<>();
        // The address of a client as issued at enrollment; a rotated client cannot report it itself.
        final java.util.Map<OraEndpoint, String> addresses = new java.util.HashMap<>();

        Site(final GameTestHelper helper) {
            this.helper = helper;
            pc = RadioBridgeTests.tower(helper, PC);
            final GpsTowerBlockEntity relay = RadioBridgeTests.tower(helper, RELAY);
            admin = pc.createRadioNetwork().get("adminSecret");
            final var invite = pc.issueRadioInvite(admin, relay.getRadioEndpointId());
            relay.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        }

        OraEndpoint client(final int dx, final AtomicBoolean online) {
            final BlockPos at = helper.absolutePos(PC.offset(dx, 0, 6));
            final UUID owner = UUID.randomUUID();
            final OraEndpoint endpoint = new OraEndpoint(helper::getLevel, () -> owner, () -> at.getX() + 0.5, () -> at.getZ() + 0.5, online::get);
            enroll(endpoint);
            clients.add(endpoint);
            return endpoint;
        }

        void enroll(final OraEndpoint endpoint) {
            final var invite = pc.issueRadioInvite(admin, endpoint.id().toString());
            endpoint.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
            addresses.put(endpoint, endpoint.address());
        }

        /** What the host of every client does each tick. */
        void tickClients() {
            for (final OraEndpoint client : clients) if (client.enrolled()) client.tick();
        }

        boolean completed(final String request) {
            tickClients();
            events.addAll(pc.pollRadioEvents());
            return events.stream().anyMatch(e -> e.requestId().toString().equals(request) && e.state() == RadioServiceBroker.State.COMPLETED);
        }

        String ping(final OraEndpoint client) {
            final String request = UUID.randomUUID().toString();
            pc.pingRadio(addresses.get(client), request);
            return request;
        }

        boolean refused(final OraEndpoint client) {
            try {
                ping(client);
                return false;
            } catch (final RuntimeException expected) { // NOPMD - the refusal is the result
                return true;
            }
        }
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void rotationRevocationAndReenrollmentDoNotResetOtherClients(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final OraEndpoint x = site.client(1, new AtomicBoolean(true));
        final OraEndpoint y = site.client(3, new AtomicBoolean(true));
        final RadioClientLeases leases = RadioNetworkData.get(helper.getLevel()).leases();
        final var registry = RadioNetworkData.get(helper.getLevel()).registry();
        final String[] request = new String[2];
        final String[] oldSecret = new String[1];
        final String[] rotated = new String[1];
        final String[] oldYAddress = new String[1];
        helper.startSequence()
                .thenExecute(() -> {
                    request[0] = site.ping(x);
                    request[1] = site.ping(y);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "both clients answer a ping",
                        site.completed(request[0]) && site.completed(request[1])))
                .thenExecute(() -> {
                    oldSecret[0] = x.credential();
                    rotated[0] = leases.activate(x.id(), x.credential(), x.dimension()).secret();
                    TestSupport.assertTrue(helper, "a second holder of the same identity is refused", leasesRefuse(leases, x, rotated[0]));
                    TestSupport.assertTrue(helper, "the old key no longer enrolls", !x.enrolled());
                    TestSupport.assertTrue(helper, "the old key no longer authenticates", registry.authenticate(x.id(), oldSecret[0]).isEmpty());
                    TestSupport.assertTrue(helper, "traffic to the rotated client is refused", site.refused(x));
                    request[1] = site.ping(y);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "another client is untouched by the rotation", site.completed(request[1])))
                .thenExecute(() -> {
                    x.identity(x.id(), rotated[0]); // the holder stores the new key
                    TestSupport.assertTrue(helper, "the new key enrolls", x.enrolled());
                    x.session();
                    request[0] = site.ping(x);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "the client reconnects with its new key", site.completed(request[0])))
                .thenExecute(() -> {
                    oldYAddress[0] = y.address();
                    oldSecret[0] = y.credential();
                    site.pc.revokeRadioEndpoint(site.admin, y.id().toString());
                    TestSupport.assertTrue(helper, "a revoked client is not enrolled", !y.enrolled());
                    request[0] = site.ping(x);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "revoking one client leaves the others working", site.completed(request[0])))
                .thenExecute(() -> {
                    site.enroll(y);
                    TestSupport.assertTrue(helper, "re-enrollment issues a new address", y.enrolled() && !y.address().equals(oldYAddress[0]));
                    TestSupport.assertEquals(helper, "enrollment generation", 2, registry.member(y.id()).orElseThrow().generation());
                    TestSupport.assertTrue(helper, "the credential of the prior generation is rejected",
                            registry.authenticate(y.id(), oldSecret[0]).isEmpty());
                    y.session();
                    request[1] = site.ping(y);
                })
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "the re-enrolled client works", site.completed(request[1])))
                .thenSucceed();
    }

    private static boolean leasesRefuse(final RadioClientLeases leases, final OraEndpoint x, final String secret) {
        try {
            leases.activate(x.id(), secret, x.dimension());
            return false;
        } catch (final SecurityException expected) { // NOPMD - the refusal is the result
            return true;
        }
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void pingAndTelemetryReportRttHopsAndAgeSeparately(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final AtomicBoolean online = new AtomicBoolean(true);
        final OraEndpoint x = site.client(2, online);
        x.register("sensor");
        final String address = x.address();
        final String subscription = site.pc.subscribeRadioTelemetry(address, "sensor", new String[] {"energy"}, 6);
        final String[] request = new String[1];
        helper.startSequence()
                .thenExecute(() -> request[0] = site.ping(x))
                .thenWaitUntil(() -> TestSupport.assertTrue(helper, "ping completes", site.completed(request[0])))
                .thenExecute(() -> {
                    final RadioServiceBroker.Event pong = site.events.stream()
                            .filter(e -> e.requestId().toString().equals(request[0]) && e.state() == RadioServiceBroker.State.COMPLETED)
                            .findFirst().orElseThrow();
                    final RadioServiceBroker.Estimate estimate = site.pc.estimateRadioRoute(address);
                    TestSupport.assertTrue(helper, "RTT is a measured nanosecond value", pong.roundTripNanos() > 0);
                    TestSupport.assertTrue(helper, "hop count is reported with the ping", pong.route() != null && pong.route().hops() == estimate.hops());
                    TestSupport.assertTrue(helper, "one-way estimate is ticks and nominal milliseconds",
                            pong.route().nominalTicks() >= 1 && pong.route().nominalTicks() <= Config.radioMaxDeliveryTicks
                                    && pong.route().nominalMillis() == pong.route().nominalTicks() * RadioServiceBroker.NOMINAL_TICK_MILLIS);
                    x.publish("sensor", "energy", "{\"stored\":1}"); // the first handshake took a while; sample now
                })
                .thenWaitUntil(() -> {
                    site.tickClients();
                    final var status = site.pc.getRadioTelemetryStatus(subscription);
                    TestSupport.assertTrue(helper, "a fresh sample is online with an age",
                            status.size() == 1 && status.get(0).state() == RadioServiceBroker.Freshness.ONLINE
                                    && status.get(0).ageMillis() == status.get(0).ageTicks() * RadioServiceBroker.NOMINAL_TICK_MILLIS);
                })
                .thenWaitUntil(() -> {
                    site.tickClients();
                    TestSupport.assertTrue(helper, "an unrefreshed sample turns stale",
                            site.pc.getRadioTelemetryStatus(subscription).get(0).state() == RadioServiceBroker.Freshness.STALE);
                })
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "a stale sample reports an age past the limit",
                            site.pc.getRadioTelemetryStatus(subscription).get(0).ageTicks() >= 6);
                    online.set(false);
                    TestSupport.assertTrue(helper, "a publisher that left is offline",
                            site.pc.getRadioTelemetryStatus(subscription).get(0).state() == RadioServiceBroker.Freshness.OFFLINE);
                })
                .thenSucceed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void retriedRequestsAreDeliveredOnce(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final OraEndpoint x = site.client(2, new AtomicBoolean(true));
        x.register("svc");
        final String address = x.address();
        final String command = UUID.randomUUID().toString(), note = UUID.randomUUID().toString();
        final List<RadioMessage> inbox = new ArrayList<>();
        helper.startSequence()
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "command is accepted", site.pc.sendRadioCommand(address, "svc", "move", command, "up", 200).equals("ACCEPTED"));
                    site.pc.sendRadioCommand(address, "svc", "move", command, "up", 200); // the retry
                    site.pc.sendRadioMessage(address, "svc", "note", note, "hi");
                    site.pc.sendRadioMessage(address, "svc", "note", note, "hi");
                    try {
                        site.pc.sendRadioCommand(address, "svc", "move", command, "down", 200);
                        throw TestSupport.failure(helper, "a request ID was reused for another command");
                    } catch (final IllegalArgumentException expected) {
                        TestSupport.assertNotNull(helper, expected.getMessage(), "rejection reason");
                    }
                })
                .thenWaitUntil(() -> {
                    site.tickClients();
                    inbox.addAll(x.pollMessages());
                    TestSupport.assertTrue(helper, "the command and the message arrived", inbox.size() >= 2);
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    site.tickClients();
                    inbox.addAll(x.pollMessages());
                    TestSupport.assertEquals(helper, "deliveries of the retried command", 1,
                            inbox.stream().filter(m -> m.requestId().toString().equals(command)).count());
                    TestSupport.assertEquals(helper, "deliveries of the retried message", 1,
                            inbox.stream().filter(m -> m.requestId().toString().equals(note)).count());
                })
                .thenSucceed();
    }

    private RadioIdentityTests() {}
}
