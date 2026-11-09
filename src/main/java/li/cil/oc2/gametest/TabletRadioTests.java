package li.cil.oc2.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import li.cil.oc2.common.components.DataComponents;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.tablet.TabletItem;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
import li.cil.oc2.common.tablet.TabletMachine;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.common.tablet.radio.TabletDevice;
import li.cil.oc2.common.tablet.radio.TabletHolder;
import li.cil.oc2.common.tablet.radio.TabletRadio;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
 * The tablet as a radio client on the real stack: towers, a dock station, a real tablet machine with
 * its guest-facing device, and a mock player holding the item. The holder is driven through the
 * manager the way its server tick does, only without a real connection.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TabletRadioTests {
    private static final String WIDE = RobotDockRadioTests.WIDE_TEMPLATE;
    private static final int ROW = 7;
    private static final BlockPos PC = new BlockPos(4, TestSupport.WORK_Y, ROW);
    private static final BlockPos RELAY = new BlockPos(10, TestSupport.WORK_Y, ROW);
    private static final BlockPos DOCK = new BlockPos(50, TestSupport.WORK_Y, ROW);
    private static final int GRACE = 45;

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void tabletAttachesInRangeAndExchangesMessagesWithPc(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String request = UUID.randomUUID().toString(), reply = UUID.randomUUID().toString();
        final List<RadioMessage> pcInbox = new ArrayList<>(), tabletInbox = new ArrayList<>();
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(20);
                    site.tick();
                    TestSupport.assertTrue(helper, "tablet is not attached before it is enrolled", !site.attached());
                    site.enroll();
                    TestSupport.assertTrue(helper, "tablet is attached 20 blocks from the tower", site.attached());
                    TestSupport.assertTrue(helper, "tablet holds its identity", site.radio().active());
                    TestSupport.assertTrue(helper, "the item carries the rotated secret",
                            !site.secretInItem().isEmpty());
                    TestSupport.assertTrue(helper, "the tablet has an ORA address", site.address.matches("[0-9A-Z]{4}:[0-9A-Z]{4}"));
                    site.pc.registerRadioService("chat");
                    site.device().registerRadioService("chat");
                    site.grantPc("MESSAGE_SEND", "TELEMETRY_READ", "COMMAND_SEND");
                    site.device().sendRadioMessage(site.pcAddress, "chat", "text", request, "hello pc");
                    // No MESSAGE_SEND scope on another service: the broker refuses the send itself.
                    TestSupport.assertTrue(helper, "send to a service without permission is refused",
                            site.refused(() -> site.device().sendRadioMessage(
                                    site.pcAddress, "other", "text", UUID.randomUUID().toString(), "x")));
                })
                .thenWaitUntil(() -> {
                    site.tick();
                    pcInbox.addAll(site.pc.pollRadioMessages());
                    TestSupport.assertTrue(helper, "PC received the tablet message",
                            pcInbox.stream().anyMatch(m -> m.requestId().toString().equals(request)
                                    && new String(m.payload(), StandardCharsets.UTF_8).equals("hello pc")));
                })
                .thenExecute(() -> site.pc.sendRadioMessage(site.address, "chat", "text", reply, "hello tablet"))
                .thenWaitUntil(() -> {
                    site.tick();
                    tabletInbox.addAll(site.device().pollRadioMessages());
                    TestSupport.assertTrue(helper, "tablet received the PC message",
                            tabletInbox.stream().anyMatch(m -> m.requestId().toString().equals(reply)
                                    && new String(m.payload(), StandardCharsets.UTF_8).equals("hello tablet")));
                })
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "the PC lists the tablet online",
                            site.pc.listRadioDevices().stream().anyMatch(p -> p.address().equals(site.address) && p.online()));
                    TestSupport.assertTrue(helper, "the tablet lists the PC online",
                            site.device().listRadioDevices().stream().anyMatch(p -> p.address().equals(site.pcAddress) && p.online()));
                    site.grantPc("TELEMETRY_READ");
                    TestSupport.assertTrue(helper, "PC message after MESSAGE_SEND was withdrawn is refused",
                            site.refused(() -> site.pc.sendRadioMessage(site.address, "chat", "text",
                                    UUID.randomUUID().toString(), "x")));
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void tabletPingsAndAnswersTelemetryAndCommandsFromPc(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String ping = UUID.randomUUID().toString(), command = UUID.randomUUID().toString();
        final String[] subscription = new String[1];
        final List<RadioServiceBroker.Event> pcEvents = new ArrayList<>(), tabletEvents = new ArrayList<>();
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(10);
                    site.tick();
                    site.enroll();
                    site.device().registerRadioService("chat");
                    site.grantPc("TELEMETRY_READ", "COMMAND_SEND");
                    site.device().pingRadio(site.pcAddress, ping);
                    subscription[0] = site.pc.subscribeRadioTelemetry(site.address, "chat", new String[] {"energy"}, 100);
                    site.device().publishRadioTelemetry("chat", "energy", "{\"stored\":7}");
                })
                .thenWaitUntil(() -> {
                    site.tick();
                    tabletEvents.addAll(site.device().pollRadioEvents());
                    TestSupport.assertTrue(helper, "tablet ping is answered",
                            tabletEvents.stream().anyMatch(e -> e.requestId().toString().equals(ping)
                                    && e.state() == RadioServiceBroker.State.COMPLETED));
                })
                .thenWaitUntil(() -> {
                    site.tick();
                    pcEvents.addAll(site.pc.pollRadioEvents());
                    TestSupport.assertTrue(helper, "PC received the tablet telemetry",
                            pcEvents.stream().anyMatch(e -> e.message() != null
                                    && e.requestId().toString().equals(subscription[0])
                                    && new String(e.message().payload(), StandardCharsets.UTF_8).contains("7")));
                })
                .thenExecute(() -> TestSupport.assertTrue(helper, "PC command to the tablet is accepted",
                        site.pc.sendRadioCommand(site.address, "chat", "do", command, "go", 200).equals("ACCEPTED")))
                .thenWaitUntil(() -> {
                    site.tick();
                    TestSupport.assertTrue(helper, "tablet received the command", site.device().pollRadioMessages().stream()
                            .anyMatch(m -> m.requestId().toString().equals(command)));
                })
                .thenExecute(() -> site.device().updateRadioCommand(command, "COMPLETED", "done"))
                .thenWaitUntil(() -> {
                    site.tick();
                    pcEvents.addAll(site.pc.pollRadioEvents());
                    TestSupport.assertTrue(helper, "PC sees the command completed",
                            pcEvents.stream().anyMatch(e -> e.requestId().toString().equals(command)
                                    && e.state() == RadioServiceBroker.State.COMPLETED));
                })
                .thenExecute(() -> {
                    site.grantPc("TELEMETRY_READ");
                    TestSupport.assertTrue(helper, "command without COMMAND_SEND is refused",
                            site.refused(() -> site.pc.sendRadioCommand(site.address, "chat", "do",
                                    UUID.randomUUID().toString(), "go", 200)));
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void tabletOnTheTowerRadiusAttachesAndJustOutsideDoesNot(final GameTestHelper helper) {
        final Site site = new Site(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(45);
                    site.tick();
                    site.enroll();
                    TestSupport.assertTrue(helper, "tablet exactly 45 blocks from the tower attaches", site.attached());
                    site.moveTo(45.01);
                    TestSupport.assertTrue(helper, "tablet 45.01 blocks from the tower does not attach", !site.attached());
                    TestSupport.assertTrue(helper, "the PC sees it offline",
                            site.pc.listRadioDevices().stream().noneMatch(p -> p.address().equals(site.address) && p.online()));
                    site.moveTo(0);
                    TestSupport.assertTrue(helper, "it attaches again at the tower", site.attached());
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void uninvitedAndWronglyInvitedTabletsAreRejected(final GameTestHelper helper) {
        final Site site = new Site(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(5);
                    site.tick();
                    TestSupport.assertTrue(helper, "an uninvited tablet is not enrolled",
                            site.device().getRadioStatus().get("enrolled").equals(false));
                    TestSupport.assertTrue(helper, "an uninvited tablet cannot list devices", site.refused(site.device()::listRadioDevices));
                    TestSupport.assertTrue(helper, "guessing an invitation id is refused", site.refused(
                            () -> site.device().joinRadioNetwork(UUID.randomUUID().toString(), "guess")));
                    // A real invitation of another endpoint does not admit this tablet.
                    final var other = site.pc.issueRadioInvite(site.admin, UUID.randomUUID().toString());
                    TestSupport.assertTrue(helper, "an invitation for another endpoint is refused", site.refused(
                            () -> site.device().joinRadioNetwork(other.get("invite"), other.get("secret"))));
                    TestSupport.assertTrue(helper, "the tablet got no identity or secret",
                            !site.radio().active() && site.secretInItem().isEmpty());
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void revokedTabletLosesTheNetworkAndCannotReactivate(final GameTestHelper helper) {
        final Site site = new Site(helper);
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(5);
                    site.tick();
                    site.enroll();
                    TestSupport.assertTrue(helper, "tablet is attached", site.attached());
                    site.pc.revokeRadioEndpoint(site.admin, site.id.toString());
                    TestSupport.assertTrue(helper, "a revoked tablet is not enrolled",
                            site.device().getRadioStatus().get("enrolled").equals(false));
                    TestSupport.assertTrue(helper, "a revoked tablet cannot list devices", site.refused(site.device()::listRadioDevices));
                    TestSupport.assertTrue(helper, "the PC cannot reach it",
                            site.refused(() -> site.pc.pingRadio(site.address, UUID.randomUUID().toString())));
                    TestSupport.assertTrue(helper, "revocation does not lock the local tablet terminal",
                            TabletManager.canUse(site.player, site.id));
                })
                // Drop and pick the tablet up again: the secret in the item is of a revoked identity.
                .thenExecuteFor(GRACE, site::tickIdle)
                .thenExecute(() -> {
                    site.tick();
                    TestSupport.assertTrue(helper, "a revoked identity does not activate again",
                            !site.radio().active() && !site.radio().denied().isEmpty());
                    TestSupport.assertTrue(helper, "the refusal text hides the secret",
                            !site.radio().denied().contains(site.secretInItem()));
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void cloneOfTheItemCannotRunASecondIdentity(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final ItemStack[] copy = new ItemStack[1];
        final String[] first = new String[1];
        final TabletRadio[] cloneRadio = new TabletRadio[1];
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(5);
                    site.tick();
                    site.enroll();
                    first[0] = site.secretInItem();
                    copy[0] = site.stack.copy();
                    TestSupport.assertTrue(helper, "the copy has the same tablet id and secret",
                            TabletItem.getId(copy[0]).orElseThrow().equals(site.id) && first[0].equals(secretOf(copy[0])));
                    // The copy's machine appears while the original is active, e.g. through another player.
                    cloneRadio[0] = new TabletRadio(site.id, helper.getLevel());
                    cloneRadio[0].update(new TabletHolder(site.player, copy[0]), 1);
                    TestSupport.assertTrue(helper, "the clone does not become active while the original is",
                            !cloneRadio[0].active() && cloneRadio[0].denied().contains("already active"));
                    TestSupport.assertTrue(helper, "the original keeps its identity", site.radio().active() && site.attached());
                })
                .thenExecuteFor(GRACE, site::tickIdle)
                .thenExecute(() -> {
                    // The original is suspended. The copy holds the previous generation's secret only if
                    // nothing rotated it; it activates once and rotates, which locks the original out.
                    cloneRadio[0].update(new TabletHolder(site.player, copy[0]), 1000);
                    TestSupport.assertTrue(helper, "the clone activates once the original is suspended", cloneRadio[0].active());
                    TestSupport.assertTrue(helper, "the clone rotated its secret", !first[0].equals(secretOf(copy[0])));
                    site.tick();
                    TestSupport.assertTrue(helper, "the original cannot activate with its older secret",
                            !site.radio().active() && !TabletManager.canUse(site.player, site.id));
                    TestSupport.assertEquals(helper, "the original item still has its older secret", 0,
                            first[0].equals(site.secretInItem()) ? 0 : 1);
                    cloneRadio[0].release(li.cil.oc2.common.radio.RadioClientLeases.Reason.DROPPED);
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void droppingTabletReleasesRadioBeforeMachineSave(final GameTestHelper helper) {
        final Site site = new Site(helper);
        site.moveTo(5);
        site.tick();
        site.enroll();
        site.tickIdle();
        TestSupport.assertTrue(helper, "drop releases radio immediately", !site.leaseHeld());
        TestSupport.assertTrue(helper, "machine remains during save grace", TabletManager.isLive(site.id));
        site.finish();
        helper.succeed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void cloneCannotUseTheOriginalMachineOrReplaceItsHeldStack(final GameTestHelper helper) {
        final Site site = new Site(helper);
        site.moveTo(5);
        site.tick();
        site.enroll();
        final ItemStack copy = site.stack.copy();
        // Advance the original credential; this makes the snapshot a stale clone.
        site.radio().release(li.cil.oc2.common.radio.RadioClientLeases.Reason.DROPPED);
        site.tick();
        site.player.setItemInHand(InteractionHand.MAIN_HAND, copy);
        TabletManager.openTerminal(site.player, site.id);
        TestSupport.assertTrue(helper, "stale clone cannot open the original terminal",
                !(site.player.containerMenu instanceof li.cil.oc2.common.container.tablet.AbstractTabletContainer));
        TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        TestSupport.assertTrue(helper, "stale stack cannot inherit the original radio", !site.radio().active());
        site.player.setItemInHand(InteractionHand.MAIN_HAND, site.stack);
        site.tick();
        TestSupport.assertTrue(helper, "original can resume after the clone is rejected", site.radio().active());
        site.finish();
        helper.succeed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void droppedTabletSuspendsAndResumesWithANewSecretAndTheSameAddress(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final String[] before = new String[1];
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(5);
                    site.tick();
                    site.enroll();
                    before[0] = site.secretInItem();
                    TestSupport.assertTrue(helper, "tablet is online", site.listedOnline());
                })
                .thenExecuteFor(GRACE, site::tickIdle)
                .thenExecute(() -> {
                    TestSupport.assertTrue(helper, "nobody holds the identity after the drop",
                            !site.leaseHeld() && !TabletManager.isLive(site.id));
                    TestSupport.assertTrue(helper, "the PC sees the tablet offline", !site.listedOnline());
                    TestSupport.assertTrue(helper, "the PC cannot reach it",
                            site.refused(() -> site.pc.pingRadio(site.address, UUID.randomUUID().toString())));
                    TestSupport.assertEquals(helper, "the dropped item keeps its secret", 0,
                            before[0].equals(site.secretInItem()) ? 0 : 1);
                    site.tick();
                    TestSupport.assertTrue(helper, "picking it up resumes the identity", site.radio().active() && site.attached());
                    TestSupport.assertTrue(helper, "the secret was rotated again", !before[0].equals(site.secretInItem()));
                    TestSupport.assertEquals(helper, "the address is unchanged", 0,
                            site.address.equals(site.device().getRadioAddress()) ? 0 : 1);
                    TestSupport.assertTrue(helper, "the PC sees the tablet online again", site.listedOnline());
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 400)
    public static void tabletRoamsBetweenDockAndTowerKeepingItsAddress(final GameTestHelper helper) {
        final Site site = new Site(helper);
        TestSupport.placeFloor(helper, DOCK);
        helper.setBlock(DOCK, Blocks.CHARGER.get().defaultBlockState());
        final var relay = RadioBridgeTests.tower(helper, RELAY);
        final var invite = site.pc.issueRadioInvite(site.admin, relay.getRadioEndpointId());
        relay.joinRadioNetwork(invite.get("invite"), invite.get("secret"));
        helper.startSequence()
                .thenExecute(() -> {
                    site.moveTo(53);
                    site.tick();
                    site.enroll();
                    TestSupport.assertTrue(helper, "tablet 7 blocks from the dock attaches through it", site.attached());
                    TestSupport.assertEquals(helper, "legs through tower, relay, dock and tablet", 3,
                            site.pc.estimateRadioRoute(site.address).hops());
                    site.moveTo(56.5);
                    TestSupport.assertTrue(helper, "10.5 blocks from the dock and 50.5 from the relay is outside both radii",
                            !site.attached());
                    site.moveTo(32);
                    TestSupport.assertTrue(helper, "tablet roams to a tower in range", site.attached());
                    TestSupport.assertTrue(helper, "the tower route is shorter than the dock route",
                            site.pc.estimateRadioRoute(site.address).hops() < 3);
                    TestSupport.assertEquals(helper, "the address is unchanged by roaming", 0,
                            site.address.equals(site.device().getRadioAddress()) ? 0 : 1);
                    site.moveTo(53);
                    TestSupport.assertTrue(helper, "tablet roams back to the dock", site.attached()
                            && site.pc.estimateRadioRoute(site.address).hops() == 3);
                    site.finish();
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void tabletRadioIsOnTheGuestDeviceBus(final GameTestHelper helper) {
        final Site site = new Site(helper);
        final TabletMachine machine = site.machine();
        helper.startSequence()
                .thenExecute(machine.virtualMachine().busController::scanDevices)
                .thenWaitUntil(() -> {
                    site.tick();
                    TestSupport.assertTrue(helper, "the tablet device with the radio calls is on the bus",
                            machine.virtualMachine().busController.getDevices().stream().anyMatch(
                                    device -> device instanceof RPCDevice rpc && rpc.getTypeNames().contains("tablet")
                                            && rpc.getTypeNames().contains("gps_tower")
                                            && hasMethod(rpc, "sendRadioMessage")
                                            && hasMethod(rpc, "listRadioDevices")
                                            && hasMethod(rpc, "pingRadio")));
                })
                .thenExecute(site::finish)
                .thenSucceed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void tabletTransferKeepsAddressAndDeathReleasesImmediately(final GameTestHelper helper) {
        final Site site = new Site(helper);
        site.moveTo(5);
        site.tick();
        site.enroll();
        final String oldSecret = site.secretInItem();
        final ServerPlayer recipient = TestSupport.serverPlayer(helper);
        recipient.setPos(site.player.getX(), site.player.getY(), site.player.getZ());
        site.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        recipient.setItemInHand(InteractionHand.MAIN_HAND, site.stack);
        TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        TestSupport.assertTrue(helper, "transfer keeps radio active", site.radio().active());
        TestSupport.assertTrue(helper, "transfer rotates the credential", !oldSecret.equals(site.secretInItem()));
        TestSupport.assertTrue(helper, "transfer keeps address", site.address.equals(site.device().getRadioAddress()));
        recipient.setHealth(0);
        TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        TestSupport.assertTrue(helper, "death immediately releases the identity", !site.leaseHeld());
        recipient.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        site.finish();
        helper.succeed();
    }

    @GameTest(template = WIDE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void aSecondHolderCannotOpenTheActiveTabletEvenWithItsCurrentSecret(final GameTestHelper helper) {
        final Site site = new Site(helper);
        site.moveTo(5);
        site.tick();
        site.enroll();
        final ServerPlayer other = TestSupport.serverPlayer(helper);
        other.setItemInHand(InteractionHand.MAIN_HAND, site.stack.copy());
        TabletManager.openTerminal(other, site.id);
        TabletManager.openInventory(other, site.id);
        TestSupport.assertTrue(helper, "copy opens no terminal or inventory",
                !(other.containerMenu instanceof li.cil.oc2.common.container.tablet.AbstractTabletContainer));
        TestSupport.assertTrue(helper, "copy cannot send input or power actions",
                TabletManager.viewedMachine(other, site.id).isEmpty() && !TabletManager.canUse(other, site.id));
        TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        TestSupport.assertTrue(helper, "the legitimate holder keeps its radio", site.radio().active());
        other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        site.finish();
        helper.succeed();
    }

    private static boolean hasMethod(final RPCDevice rpc, final String name) {
        return rpc.getMethodGroups().stream().anyMatch(group -> group.getName().equals(name));
    }

    private static String secretOf(final ItemStack stack) {
        return stack.getOrDefault(DataComponents.TABLET_RADIO_SECRET.get(), "");
    }

    /** One PC tower, one network and one tablet held by a mock player standing on the tower's row. */
    private static final class Site {
        final GameTestHelper helper;
        final GpsTowerBlockEntity pc;
        final String admin;
        final String pcId;
        final String pcAddress;
        final ServerPlayer player;
        final ItemStack stack = new ItemStack(Items.TABLET.get());
        final UUID id = TabletItem.getOrCreateId(stack);
        final Vec3 origin;
        String address = "";

        Site(final GameTestHelper helper) {
            this.helper = helper;
            pc = RadioBridgeTests.tower(helper, PC);
            admin = pc.createRadioNetwork().get("adminSecret");
            pcId = pc.getRadioEndpointId();
            pcAddress = pc.getRadioAddress();
            player = TestSupport.serverPlayer(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            origin = helper.absoluteVec(Vec3.atLowerCornerOf(PC));
        }

        /** Stands the holder at an X offset from the PC tower's centre. */
        void moveTo(final double dx) {
            player.setPos(origin.x + dx, origin.y, origin.z);
        }

        TabletMachine machine() {
            final TabletMachine machine = TabletManager.machineOf(helper.getLevel().getServer(), id);
            TestSupport.assertNotNull(helper, machine, "tablet machine");
            return machine;
        }

        TabletRadio radio() {
            return machine().radio();
        }

        TabletDevice device() {
            return new TabletDevice(radio());
        }

        void tick() {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        }

        void tickIdle() {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            TabletManager.tickFor(helper.getLevel().getServer(), Set.of());
        }

        /** Invites the tablet with message rights towards the PC's chat service and joins through the guest device. */
        void enroll() {
            final var invite = pc.issueRadioInviteWithPermissions(
                    admin, id.toString(), new String[] {"MESSAGE_SEND"}, pcId, "chat");
            device().joinRadioNetwork(invite.get("invite"), invite.get("secret"));
            address = device().getRadioAddress();
        }

        /** Narrows what the PC may do to the tablet's chat service. */
        void grantPc(final String... permissions) {
            pc.setRadioPermissions(admin, pcId, permissions, id.toString(), "chat");
        }

        boolean attached() {
            return device().getRadioStatus().get("attached").equals(true);
        }

        boolean listedOnline() {
            return pc.listRadioDevices().stream().anyMatch(p -> p.address().equals(address) && p.online());
        }

        boolean leaseHeld() {
            return li.cil.oc2.common.blockentity.radio.RadioNetworkData.get(helper.getLevel()).leases().active(id);
        }

        String secretInItem() {
            return secretOf(stack);
        }

        boolean refused(final Runnable call) {
            try {
                call.run();
                return false;
            } catch (final RuntimeException expected) { // NOPMD - the refusal is the result
                return true;
            }
        }

        void finish() {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            try {
                TabletManager.discard(helper.getLevel().getServer(), id);
            } catch (final IOException e) {
                throw TestSupport.failure(helper, "cannot remove test state: " + e);
            }
        }
    }

    private TabletRadioTests() {}
}
