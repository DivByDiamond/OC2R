package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = API.MOD_ID)
public final class Network {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ComputerTerminalOutputMessage.TYPE,
                ComputerTerminalOutputMessage.STREAM_CODEC,
                ComputerTerminalOutputMessage::handleMessage);
        registrar.playToServer(
                ComputerTerminalInputMessage.TYPE,
                ComputerTerminalInputMessage.STREAM_CODEC,
                ComputerTerminalInputMessage::handleMessage);
        registrar.playToClient(
                ComputerRunStateMessage.TYPE,
                ComputerRunStateMessage.STREAM_CODEC,
                ComputerRunStateMessage::handleMessage);
        registrar.playToClient(
                ComputerBusStateMessage.TYPE,
                ComputerBusStateMessage.STREAM_CODEC,
                ComputerBusStateMessage::handleMessage);
        registrar.playToClient(
                ComputerBootErrorMessage.TYPE,
                ComputerBootErrorMessage.STREAM_CODEC,
                ComputerBootErrorMessage::handleMessage);
        registrar.playToServer(
                ComputerPowerMessage.TYPE,
                ComputerPowerMessage.STREAM_CODEC,
                ComputerPowerMessage::handleMessage);
        registrar.playToServer(
                MonitorPowerMessage.TYPE,
                MonitorPowerMessage.STREAM_CODEC,
                MonitorPowerMessage::handleMessage);
        registrar.playToClient(
                MonitorPowerMessageForwarded.TYPE,
                MonitorPowerMessageForwarded.STREAM_CODEC,
                MonitorPowerMessageForwarded::handleMessage);
        registrar.playToServer(
                OpenComputerInventoryMessage.TYPE,
                OpenComputerInventoryMessage.STREAM_CODEC,
                OpenComputerInventoryMessage::handleMessage);
        registrar.playToServer(
                OpenComputerTerminalMessage.TYPE,
                OpenComputerTerminalMessage.STREAM_CODEC,
                OpenComputerTerminalMessage::handleMessage);

        registrar.playToClient(
                NetworkConnectorConnectionsMessage.TYPE,
                NetworkConnectorConnectionsMessage.STREAM_CODEC,
                NetworkConnectorConnectionsMessage::handleMessage);

        registrar.playToClient(
                RobotTerminalOutputMessage.TYPE,
                RobotTerminalOutputMessage.STREAM_CODEC,
                RobotTerminalOutputMessage::handleMessage);
        registrar.playToServer(
                RobotTerminalInputMessage.TYPE,
                RobotTerminalInputMessage.STREAM_CODEC,
                RobotTerminalInputMessage::handleMessage);
        registrar.playToClient(
                RobotRunStateMessage.TYPE,
                RobotRunStateMessage.STREAM_CODEC,
                RobotRunStateMessage::handleMessage);
        registrar.playToClient(
                RobotBusStateMessage.TYPE,
                RobotBusStateMessage.STREAM_CODEC,
                RobotBusStateMessage::handleMessage);
        registrar.playToClient(
                RobotBootErrorMessage.TYPE,
                RobotBootErrorMessage.STREAM_CODEC,
                RobotBootErrorMessage::handleMessage);
        registrar.playToServer(
                RobotPowerMessage.TYPE,
                RobotPowerMessage.STREAM_CODEC,
                RobotPowerMessage::handleMessage);
        registrar.playToServer(
                RobotInitializationRequestMessage.TYPE,
                RobotInitializationRequestMessage.STREAM_CODEC,
                RobotInitializationRequestMessage::handleMessage);
        registrar.playToClient(
                RobotInitializationMessage.TYPE,
                RobotInitializationMessage.STREAM_CODEC,
                RobotInitializationMessage::handleMessage);
        registrar.playToServer(
                OpenRobotInventoryMessage.TYPE,
                OpenRobotInventoryMessage.STREAM_CODEC,
                OpenRobotInventoryMessage::handleMessage);
        registrar.playToServer(
                OpenRobotTerminalMessage.TYPE,
                OpenRobotTerminalMessage.STREAM_CODEC,
                OpenRobotTerminalMessage::handleMessage);

        registrar.playToClient(
                DiskDriveFloppyMessage.TYPE,
                DiskDriveFloppyMessage.STREAM_CODEC,
                DiskDriveFloppyMessage::handleMessage);
        registrar.playToClient(
                FirmwareFlasherMessage.TYPE,
                FirmwareFlasherMessage.STREAM_CODEC,
                FirmwareFlasherMessage::handleMessage);

        registrar.playBidirectional(
                BusInterfaceNameMessage.TYPE,
                BusInterfaceNameMessage.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        BusInterfaceNameMessage::handleClientMessage,
                        BusInterfaceNameMessage::handleServerMessage));

        registrar.playToClient(
                ExportedFileMessage.TYPE,
                ExportedFileMessage.STREAM_CODEC,
                ExportedFileMessage::handleMessage);
        registrar.playToClient(
                RequestImportedFileMessage.TYPE,
                RequestImportedFileMessage.STREAM_CODEC,
                RequestImportedFileMessage::handleMessage);
        registrar.playToServer(
                ImportedFileMessage.TYPE,
                ImportedFileMessage.STREAM_CODEC,
                ImportedFileMessage::handleMessage);
        registrar.playToClient(
                ServerCanceledImportFileMessage.TYPE,
                ServerCanceledImportFileMessage.STREAM_CODEC,
                ServerCanceledImportFileMessage::handleMessage);
        registrar.playToServer(
                ClientCanceledImportFileMessage.TYPE,
                ClientCanceledImportFileMessage.STREAM_CODEC,
                ClientCanceledImportFileMessage::handleMessage);

        registrar.playToClient(
                BusCableFacadeMessage.TYPE,
                BusCableFacadeMessage.STREAM_CODEC,
                BusCableFacadeMessage::handleMessage);

        registrar.playToServer(
                NetworkInterfaceCardConfigurationMessage.TYPE,
                NetworkInterfaceCardConfigurationMessage.STREAM_CODEC,
                NetworkInterfaceCardConfigurationMessage::handleMessage);
        registrar.playToServer(
                NetworkTunnelLinkMessage.TYPE,
                NetworkTunnelLinkMessage.STREAM_CODEC,
                NetworkTunnelLinkMessage::handleMessage);

        registrar.playToServer(
                MonitorRequestFramebufferMessage.TYPE,
                MonitorRequestFramebufferMessage.STREAM_CODEC,
                MonitorRequestFramebufferMessage::handleMessage);
        registrar.playToClient(
                MonitorFramebufferMessage.TYPE,
                MonitorFramebufferMessage.STREAM_CODEC,
                MonitorFramebufferMessage::handleMessage);

        registrar.playToServer(
                ProjectorRequestFramebufferMessage.TYPE,
                ProjectorRequestFramebufferMessage.STREAM_CODEC,
                ProjectorRequestFramebufferMessage::handleMessage);
        registrar.playToClient(
                ProjectorFramebufferMessage.TYPE,
                ProjectorFramebufferMessage.STREAM_CODEC,
                ProjectorFramebufferMessage::handleMessage);
        registrar.playToClient(
                ProjectorStateMessage.TYPE,
                ProjectorStateMessage.STREAM_CODEC,
                ProjectorStateMessage::handleMessage);
        registrar.playToClient(
                MonitorStateMessage.TYPE,
                MonitorStateMessage.STREAM_CODEC,
                MonitorStateMessage::handleMessage);

        registrar.playToServer(
                KeyboardInputMessage.TYPE,
                KeyboardInputMessage.STREAM_CODEC,
                KeyboardInputMessage::handleMessage);

        registrar.playToServer(
                MonitorInputMessage.TYPE,
                MonitorInputMessage.STREAM_CODEC,
                MonitorInputMessage::handleMessage);

        registrar.playToServer(
                MultipartMessage.TYPE,
                MultipartMessage.STREAM_CODEC,
                MultipartMessage::handleMessage);
        MultipartMessage.registerMessage(
                ImportedFileMessage.class, ImportedFileMessage.STREAM_CODEC);
    }

    public static void sendToServer(final CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToClient(final CustomPacketPayload message, final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    public static void sendToClientsTrackingChunk(
            final CustomPacketPayload message, final LevelChunk chunk) {
        // Chunk#getLevel can return something that is not a ServerLevel — most
        // notably on Valkyrien Skies ship worlds used by Create: Aeronautics
        // contraptions. The unconditional cast used to throw ClassCastException
        // out of the VM runner thread, which froze the computer in an "appears
        // on, UART never updates" state. Bail out cleanly instead.
        if (chunk.getLevel() instanceof final ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), message);
            return;
        }

        // Fallback for non-ServerLevel worlds (e.g. VS2 ship worlds): the
        // regular chunk-tracking API doesn't apply, so broadcast the message
        // to every player who currently has the host level loaded. This is
        // less efficient than chunk-tracking (we send to everyone, not just
        // to players who can see this chunk), but it's the best we can do
        // without a hard dependency on Valkyrien Skies. Players on the same
        // ship — or near it in the parent level — will still get the
        // terminal output and the block face will render correctly.
        final Level hostLevel = chunk.getLevel();
        if (hostLevel == null) {
            return;
        }
        final MinecraftServer server = hostLevel.getServer();
        if (server == null) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Only send to players who actually have a client level — avoids
            // trying to serialize the payload for players in the wrong
            // dimension or in the middle of disconnect/login.
            if (player.connection == null || !player.connection.isAcceptingMessages()) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, message);
        }
    }

    public static void sendToClientsTrackingBlockEntity(
            final CustomPacketPayload message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        final MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        if (!server.isSameThread()) {
            throw new IllegalStateException(
                    "Attempting to send network message to BlockEntity from non-server "
                            + "thread ["
                            + Thread.currentThread()
                            + "]. This is not supported, "
                            + "because looking up the chunk from the level is required. "
                            + "Consider caching the containing chunk and using "
                            + "sendToClientsTrackingChunk() directly, instead.");
        }

        final BlockPos blockPos = blockEntity.getBlockPos();
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            sendToClientsTrackingChunk(message, chunk);
        }
    }

    public static void sendToClientsTrackingEntity(
            final CustomPacketPayload message, final Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, message);
    }
}
