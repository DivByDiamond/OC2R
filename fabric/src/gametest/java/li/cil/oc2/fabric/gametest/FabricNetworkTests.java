package li.cil.oc2.fabric.gametest;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.platform.FabricMenuBridge;
import li.cil.oc2.platform.FabricMessageRegistrar;
import li.cil.oc2.platform.FabricNetworkBridge;
import li.cil.oc2.platform.MessageContext;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/**
 * Exercises {@link FabricMessageRegistrar}, {@link FabricNetworkBridge} and {@link FabricMenuBridge}
 * on the dedicated test server.
 *
 * <p>Limit: no real client connects to the test server, so nothing travels over a socket. Outgoing
 * packets are read from the embedded channel behind the mock players' connections, and the
 * serverbound receiver path is driven by handing a {@link ServerboundCustomPayloadPacket} to the
 * player's {@link ServerGamePacketListenerImpl}, which is what the network layer does after
 * decoding. Wire encoding and clientbound receivers (they only exist on the physical client, see
 * {@code FabricClientReceivers}) are not covered.
 */
public final class FabricNetworkTests implements FabricGameTest {
    private static final String NAMESPACE = "oc2r_gametest";

    private static final CustomPacketPayload.Type<Serverbound> SERVERBOUND = typeOf("serverbound");
    private static final CustomPacketPayload.Type<Clientbound> CLIENTBOUND = typeOf("clientbound");
    private static final CustomPacketPayload.Type<Both> BOTH = typeOf("both");

    record Serverbound(int value) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SERVERBOUND;
        }
    }

    record Clientbound(int value) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return CLIENTBOUND;
        }
    }

    record Both(int value) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return BOTH;
        }
    }

    record Received(CustomPacketPayload payload, Player player, Connection connection) {}

    private static final List<Received> SERVER_RECEIVED = new ArrayList<>();
    private static final List<Boolean> WORK_RAN = new ArrayList<>();

    static {
        final FabricMessageRegistrar registrar = new FabricMessageRegistrar();
        registrar.registerServerbound(SERVERBOUND,
                ByteBufCodecs.VAR_INT.map(Serverbound::new, Serverbound::value).cast(),
                (payload, context) -> receive(payload, context));
        registrar.registerClientbound(CLIENTBOUND,
                ByteBufCodecs.VAR_INT.map(Clientbound::new, Clientbound::value).cast(),
                (payload, context) -> {
                    throw new IllegalStateException("clientbound handler must not run on a dedicated server");
                });
        registrar.registerBidirectional(BOTH,
                ByteBufCodecs.VAR_INT.map(Both::new, Both::value).cast(),
                (payload, context) -> receive(payload, context),
                (payload, context) -> {
                    throw new IllegalStateException("clientbound handler must not run on a dedicated server");
                });
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(final String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NAMESPACE, path));
    }

    private static void receive(final CustomPacketPayload payload, final MessageContext context) {
        synchronized (SERVER_RECEIVED) {
            SERVER_RECEIVED.add(new Received(payload, context.player(), context.connection()));
        }
        context.enqueueWork(() -> {
            synchronized (SERVER_RECEIVED) {
                WORK_RAN.add(true);
            }
        });
    }

    /**
     * Reads the packets the server wrote to a mock player's connection. The mock player's
     * connection is backed by an {@link EmbeddedChannel} with an empty pipeline, so outgoing
     * packets pile up as objects in its outbound queue.
     */
    private static final class CapturingConnection {
        final Connection connection;
        private final EmbeddedChannel channel;
        private final List<Packet<?>> sentPackets = new ArrayList<>();

        CapturingConnection(final Connection connection) throws ReflectiveOperationException {
            this.connection = connection;
            final Field field = findField(Connection.class, "channel");
            field.setAccessible(true);
            channel = (EmbeddedChannel) field.get(connection);
            sent(); // drop what the login sequence wrote
            sentPackets.clear();
        }

        List<Packet<?>> sent() {
            channel.flush();
            Object message;
            while ((message = channel.readOutbound()) != null) {
                if (message instanceof Packet<?> packet) {
                    sentPackets.add(packet);
                }
            }
            return sentPackets;
        }

        int count(final Class<? extends CustomPacketPayload> type) {
            int result = 0;
            for (final Packet<?> packet : sent()) {
                if (packet instanceof ClientboundCustomPayloadPacket custom && type.isInstance(custom.payload())) {
                    result++;
                }
            }
            return result;
        }
    }

    private static CapturingConnection attach(final GameTestHelper helper, final ServerPlayer player)
            throws ReflectiveOperationException {
        return new CapturingConnection(player.connection.connection);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sendToPlayerReachesTheConnection(final GameTestHelper helper)
            throws ReflectiveOperationException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final CapturingConnection connection = attach(helper, player);
        final FabricNetworkBridge bridge = new FabricNetworkBridge();

        bridge.sendToPlayer(new Clientbound(7), player);
        bridge.sendToPlayer(new Both(8), player);

        if (connection.count(Clientbound.class) != 1 || connection.count(Both.class) != 1) {
            helper.fail("expected one clientbound packet of each type, got " + connection.sent());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void sendToTrackingPlayersDoesNotThrow(final GameTestHelper helper)
            throws ReflectiveOperationException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ServerPlayer other = helper.makeMockServerPlayerInLevel();
        final CapturingConnection connection = attach(helper, player);
        final FabricNetworkBridge bridge = new FabricNetworkBridge();
        final ChunkPos chunk = new ChunkPos(player.blockPosition());

        // Mock players are placed through the PlayerList, so the chunk map tracks them like real
        // ones, but the entity tracker only registers them after a few ticks. Keep sending until the
        // lookups find the player; the sends themselves must never throw, found or not.
        helper.succeedWhen(() -> {
            // Refresh the tracking views the way a moving player would.
            helper.getLevel().getChunkSource().chunkMap.move(player);
            helper.getLevel().getChunkSource().chunkMap.move(other);
            bridge.sendToPlayersTrackingChunk(new Clientbound(1), helper.getLevel(), chunk);
            bridge.sendToPlayersTrackingEntity(new Clientbound(2), other);
            boolean viaChunk = false;
            boolean viaEntity = false;
            for (final Packet<?> packet : connection.sent()) {
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof Clientbound message) {
                    viaChunk |= message.value() == 1;
                    viaEntity |= message.value() == 2;
                }
            }
            if (!viaChunk || !viaEntity) {
                helper.fail("not yet tracked: viaChunk=" + viaChunk + " viaEntity=" + viaEntity);
            }
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void serverboundReceiverRunsHandlerWithContext(final GameTestHelper helper)
            throws ReflectiveOperationException {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final CapturingConnection connection = attach(helper, player);
        synchronized (SERVER_RECEIVED) {
            SERVER_RECEIVED.clear();
            WORK_RAN.clear();
        }

        player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new Serverbound(42)));
        player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new Both(43)));

        synchronized (SERVER_RECEIVED) {
            if (SERVER_RECEIVED.size() != 2) {
                helper.fail("expected 2 handler invocations, got " + SERVER_RECEIVED.size());
                return;
            }
            for (final Received received : SERVER_RECEIVED) {
                if (received.player() != player) {
                    helper.fail("context.player() is not the sending player");
                }
                if (received.connection() != connection.connection) {
                    helper.fail("context.connection() is not the player's connection");
                }
            }
            if (!(SERVER_RECEIVED.get(0).payload() instanceof Serverbound first) || first.value() != 42
                    || !(SERVER_RECEIVED.get(1).payload() instanceof Both second) || second.value() != 43) {
                helper.fail("payloads were not delivered intact: " + SERVER_RECEIVED);
            }
        }
        // enqueueWork schedules on the server thread; it must have run within a couple of ticks.
        helper.succeedWhen(() -> {
            synchronized (SERVER_RECEIVED) {
                if (WORK_RAN.size() != 2) {
                    helper.fail("enqueueWork tasks ran: " + WORK_RAN.size());
                }
            }
        });
    }

    // Menu ---------------------------------------------------------------------------------------

    private static final class TestMenu extends AbstractContainerMenu {
        final FriendlyByteBuf data;

        TestMenu(final MenuType<?> type, final int id, final FriendlyByteBuf data) {
            super(type, id);
            this.data = data;
        }

        @Override
        public ItemStack quickMoveStack(final Player player, final int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }
    }

    private static MenuType<TestMenu> menuType;

    @SuppressWarnings("unchecked")
    private static synchronized MenuType<TestMenu> menuType() throws ReflectiveOperationException {
        if (menuType == null) {
            final MenuType<TestMenu>[] holder = new MenuType[1];
            holder[0] = new FabricMenuBridge().createMenuType((id, inventory, data) -> new TestMenu(holder[0], id, data));
            // The menu registry is frozen once the server runs and Fabric only opens extended menus
            // whose type is registered, so unfreeze it just long enough to add the test type.
            final Registry<MenuType<?>> registry = BuiltInRegistries.MENU;
            final Field frozen = findField(registry.getClass(), "frozen");
            frozen.setAccessible(true);
            frozen.setBoolean(registry, false);
            try {
                Registry.register(registry, ResourceLocation.fromNamespaceAndPath(NAMESPACE, "test_menu"), holder[0]);
            } finally {
                frozen.setBoolean(registry, true);
            }
            menuType = holder[0];
        }
        return menuType;
    }

    private static Field findField(final Class<?> type, final String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                // try the superclass
            }
        }
        throw new NoSuchFieldException(name);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void openMenuExtraDataRoundTrips(final GameTestHelper helper) throws ReflectiveOperationException {
        final MenuType<TestMenu> type = menuType();
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final CapturingConnection connection = attach(helper, player);
        final BlockPos pos = new BlockPos(11, 22, 33);
        final MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("test");
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player opener) {
                return new TestMenu(type, id, null);
            }
        };

        new FabricMenuBridge().openMenu(player, provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeUtf("hello");
            buffer.writeVarInt(300);
        });

        if (!(player.containerMenu instanceof TestMenu)) {
            helper.fail("the provider's menu was not opened, container is " + player.containerMenu);
            return;
        }
        // Pull the byte array out of the open-screen packet Fabric sent.
        byte[] bytes = null;
        int syncId = -1;
        for (final Packet<?> packet : connection.sent()) {
            if (packet instanceof ClientboundCustomPayloadPacket custom
                    && "OpenScreenPayload".equals(custom.payload().getClass().getSimpleName())) {
                final Object payload = custom.payload();
                final var dataMethod = payload.getClass().getDeclaredMethod("data");
                dataMethod.setAccessible(true);
                bytes = (byte[]) dataMethod.invoke(payload);
                final var syncMethod = payload.getClass().getDeclaredMethod("syncId");
                syncMethod.setAccessible(true);
                syncId = (Integer) syncMethod.invoke(payload);
            }
        }
        if (bytes == null) {
            helper.fail("no open screen payload was sent: " + connection.sent());
            return;
        }
        if (syncId != player.containerMenu.containerId) {
            helper.fail("sync id mismatch: " + syncId + " vs " + player.containerMenu.containerId);
        }

        // Client side: the type's codec carries the bytes over the wire and feeds the factory.
        @SuppressWarnings("unchecked")
        final ExtendedScreenHandlerType<TestMenu, byte[]> extended = (ExtendedScreenHandlerType<TestMenu, byte[]>) type;
        final RegistryFriendlyByteBuf wire =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        extended.getPacketCodec().encode(wire, bytes);
        final byte[] decoded = extended.getPacketCodec().decode(wire);
        final FriendlyByteBuf data = extended.create(syncId, player.getInventory(), decoded).data;
        if (!pos.equals(data.readBlockPos()) || !"hello".equals(data.readUtf()) || data.readVarInt() != 300
                || data.isReadable()) {
            helper.fail("extra data did not round-trip");
        }
        helper.succeed();
    }
}
