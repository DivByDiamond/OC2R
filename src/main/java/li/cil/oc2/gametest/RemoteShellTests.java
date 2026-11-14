/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.blockentity.network.vxlan.VxlanBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.network.NetworkInterfaceCardItem;
import li.cil.oc2.common.vxlan.TunnelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
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

/**
 * The bundled OnyxOS guest runs {@code rshd}, and a peer on the far side of a real VXLAN hub reaches
 * it with ping and a TCP session, the way a PC with a Linux VXLAN device would with {@code nc}. The
 * hub, the tunnel manager and its UDP socket are the production ones; only the PC is played by
 * {@link VxlanTcpPeer} over the same datagrams.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RemoteShellTests {
    // computer | connector ... cable ... connector | hub: a connector polls the one block it is placed
    // against, and a hub only floods, so each end of the cable needs a connector of its own.
    private static final BlockPos NEAR_CONNECTOR = TestSupport.COMPUTER_POS.east();
    private static final BlockPos HUB = NEAR_CONNECTOR.east(4);
    private static final BlockPos FAR_CONNECTOR = HUB.west();
    private static final byte[] PEER_IP = {(byte) 192, (byte) 168, 77, 1};
    private static final byte[] GUEST_IP = {(byte) 192, (byte) 168, 77, 2};
    private static final int SHELL_PORT = 2323;
    private static final String PASSWORD = "hubpass";
    private static final long RESEND_TICKS = 20;

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 40_000)
    public static void onyxGuestServesARemoteShellThroughTheVxlanHub(final GameTestHelper helper) {
        final Run run = new Run();
        final Player player = TestSupport.fakePlayer(helper);
        TestSupport.placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        final ItemStack card = new ItemStack(Items.NETWORK_INTERFACE_CARD.get());
        for (final Direction side : Direction.values()) {
            NetworkInterfaceCardItem.setSideConfiguration(card, side, true);
        }
        computer.install(DeviceTypes.CARD, card);

        helper.startSequence()
            .thenExecute(() -> run.openTunnel(helper))
            .thenExecute(computer::start)
            .thenWaitUntil(() -> computer.assertScreenContains("login:", "the guest reaches its login prompt"))
            .thenExecute(() -> computer.type("root\r"))
            .thenWaitUntil(() -> computer.assertScreenContains("osh$", "root gets a shell"))
            .thenExecute(() -> computer.type("netcfg set 0 192.168.77.2 255.255.255.0 0.0.0.0 0.0.0.0\r"))
            .thenWaitUntil(() -> computer.assertScreenContains("Network configured", "netcfg applies the hub address"))
            .thenExecute(() -> computer.type("rshd -f " + SHELL_PORT + " " + PASSWORD + "\r"))
            .thenWaitUntil(() -> computer.assertScreenContains("listening on TCP port " + SHELL_PORT,
                "rshd listens"))
            .thenWaitUntil(() -> run.resolve(helper, computer))
            .thenExecute(() -> run.ping(helper))
            .thenWaitUntil(() -> TestSupport.assertTrue(helper, "the guest answers ping over the hub", run.pingAnswered(helper)))
            .thenExecute(() -> run.connect(helper))
            .thenWaitUntil(() -> run.expect(helper, "Password: ", "rshd asks for the password"))
            .thenExecute(() -> run.say(helper, PASSWORD + "\n"))
            .thenWaitUntil(() -> run.expect(helper, "osh$ ", "the password is accepted"))
            .thenExecute(() -> run.say(helper, "echo hub-ok\n"))
            .thenWaitUntil(() -> run.expect(helper, "hub-ok\nosh$ ", "a command runs and its output comes back"))
            .thenExecute(() -> run.say(helper, "cd /etc\n"))
            .thenWaitUntil(() -> run.expect(helper, "osh$ ", "cd is accepted"))
            .thenExecute(() -> run.say(helper, "pwd\n"))
            .thenWaitUntil(() -> run.expect(helper, "/etc\nosh$ ", "the directory persists between lines"))
            .thenExecute(() -> run.say(helper, "exit\n"))
            .thenWaitUntil(() -> run.expect(helper, "bye\n", "exit ends the session"))
            .thenWaitUntil(() -> TestSupport.assertTrue(helper, "the guest closes the connection", run.closedByGuest(helper)))
            .thenExecute(run::closeTunnel)
            .thenSucceed();
    }

    /** The tunnel and the peer of one test run. */
    private static final class Run {
        private DatagramChannel socket;
        private VxlanTcpPeer peer;
        private boolean enabledBefore;
        private String remoteBefore;
        private int remotePortBefore;
        private String bindBefore;
        private int bindPortBefore;
        private long tick;

        void openTunnel(final GameTestHelper helper) {
            try {
                final InetAddress loopback = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
                socket = DatagramChannel.open();
                socket.configureBlocking(false);
                socket.bind(new InetSocketAddress(loopback, 0));
                final int bindPort;
                try (DatagramSocket probe = new DatagramSocket(0, loopback)) {
                    bindPort = probe.getLocalPort();
                }
                enabledBefore = Config.enable;
                remoteBefore = Config.remoteHost;
                remotePortBefore = Config.remotePort;
                bindBefore = Config.bindHost;
                bindPortBefore = Config.bindPort;
                Config.enable = true;
                Config.remoteHost = loopback.getHostAddress();
                Config.remotePort = ((InetSocketAddress) socket.getLocalAddress()).getPort();
                Config.bindHost = loopback.getHostAddress();
                Config.bindPort = bindPort;
                TunnelManager.initialize();
                final Player player = TestSupport.fakePlayer(helper);
                helper.setBlock(HUB, Blocks.VXLAN_HUB.get().defaultBlockState());
                TestSupport.useOn(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()),
                    TestSupport.COMPUTER_POS, Direction.EAST);
                TestSupport.useOn(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), HUB, Direction.WEST);
                final ConnectionResult linked = NetworkConnectorBlockEntity.connect(
                    TestSupport.blockEntity(helper, NEAR_CONNECTOR, NetworkConnectorBlockEntity.class),
                    TestSupport.blockEntity(helper, FAR_CONNECTOR, NetworkConnectorBlockEntity.class));
                TestSupport.assertTrue(helper, "the connectors are linked: " + linked,
                    linked == ConnectionResult.SUCCESS || linked == ConnectionResult.ALREADY_CONNECTED);
                final int vni = TestSupport.blockEntity(helper, HUB, VxlanBlockEntity.class).getVni();
                peer = new VxlanTcpPeer(socket, new InetSocketAddress(loopback, bindPort), vni, PEER_IP, GUEST_IP);
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        void resolve(final GameTestHelper helper, final ComputerFixture computer) {
            tick++;
            try {
                peer.poll();
                if (!peer.resolvedGuest() && tick % RESEND_TICKS == 1) {
                    peer.arpRequest();
                }
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
            TestSupport.assertTrue(helper, "the guest resolves through the hub; datagrams from the hub: "
                + peer.datagrams() + ", card visible to the connector: "
                + (computer.networkInterface(Direction.EAST) != null), peer.resolvedGuest());
        }

        void ping(final GameTestHelper helper) {
            try {
                peer.ping(1);
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        boolean pingAnswered(final GameTestHelper helper) {
            tick++;
            try {
                peer.poll();
                if (!peer.gotEchoReply() && tick % RESEND_TICKS == 0) {
                    peer.ping(2);
                }
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
            return peer.gotEchoReply();
        }

        void connect(final GameTestHelper helper) {
            try {
                peer.connect(SHELL_PORT);
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        void say(final GameTestHelper helper, final String text) {
            try {
                peer.send(text);
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        /** Succeeds once the guest has sent the text; what came before it is dropped. */
        void expect(final GameTestHelper helper, final String text, final String what) {
            try {
                peer.poll();
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
            TestSupport.assertTrue(helper, "no reset while waiting for: " + what, !peer.wasReset());
            final int at = peer.received().indexOf(text);
            TestSupport.assertTrue(helper, what + "; received " + peer.received(), at >= 0);
            peer.consume(at + text.length());
        }

        boolean closedByGuest(final GameTestHelper helper) {
            try {
                peer.poll();
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
            return peer.guestClosed();
        }

        void closeTunnel() {
            final TunnelManager manager = TunnelManager.instance();
            if (manager != null) {
                manager.shutdown();
            }
            try {
                socket.close();
            } catch (final IOException failure) {
                throw new UncheckedIOException(failure);
            }
            Config.enable = enabledBefore;
            Config.remoteHost = remoteBefore;
            Config.remotePort = remotePortBefore;
            Config.bindHost = bindBefore;
            Config.bindPort = bindPortBefore;
            TunnelManager.initialize();
        }
    }
}
