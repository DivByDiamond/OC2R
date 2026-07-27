package li.cil.oc2.common.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.computer.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.computer.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.computer.ComputerPowerMessage;
import li.cil.oc2.common.network.message.computer.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.computer.ComputerTerminalInputMessage;
import li.cil.oc2.common.network.message.computer.ComputerTerminalOutputMessage;
import li.cil.oc2.common.network.message.computer.FirmwareFlasherMessage;
import li.cil.oc2.common.network.message.computer.OpenComputerInventoryMessage;
import li.cil.oc2.common.network.message.computer.OpenComputerTerminalMessage;
import li.cil.oc2.common.network.message.disk.DiskDriveFloppyMessage;
import li.cil.oc2.common.network.message.file.ClientCanceledImportFileMessage;
import li.cil.oc2.common.network.message.file.ExportedFileMessage;
import li.cil.oc2.common.network.message.file.ImportedFileMessage;
import li.cil.oc2.common.network.message.file.RequestImportedFileMessage;
import li.cil.oc2.common.network.message.file.ServerCanceledImportFileMessage;
import li.cil.oc2.common.network.message.misc.MultipartMessage;
import li.cil.oc2.common.network.message.monitor.KeyboardInputMessage;
import li.cil.oc2.common.network.message.monitor.MonitorFramebufferMessage;
import li.cil.oc2.common.network.message.monitor.MonitorInputMessage;
import li.cil.oc2.common.network.message.monitor.MonitorPowerMessage;
import li.cil.oc2.common.network.message.monitor.MonitorPowerMessageForwarded;
import li.cil.oc2.common.network.message.monitor.MonitorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.monitor.MonitorStateMessage;
import li.cil.oc2.common.network.message.network.BusCableFacadeMessage;
import li.cil.oc2.common.network.message.network.BusInterfaceNameMessage;
import li.cil.oc2.common.network.message.network.NetworkConnectorConnectionsMessage;
import li.cil.oc2.common.network.message.network.NetworkInterfaceCardConfigurationMessage;
import li.cil.oc2.common.network.message.network.NetworkTunnelLinkMessage;
import li.cil.oc2.common.network.message.projector.ProjectorFramebufferMessage;
import li.cil.oc2.common.network.message.projector.ProjectorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.projector.ProjectorStateMessage;
import li.cil.oc2.common.network.message.robot.OpenRobotInventoryMessage;
import li.cil.oc2.common.network.message.robot.OpenRobotTerminalMessage;
import li.cil.oc2.common.network.message.robot.RobotBootErrorMessage;
import li.cil.oc2.common.network.message.robot.RobotBusStateMessage;
import li.cil.oc2.common.network.message.robot.RobotInitializationMessage;
import li.cil.oc2.common.network.message.robot.RobotInitializationRequestMessage;
import li.cil.oc2.common.network.message.robot.RobotPowerMessage;
import li.cil.oc2.common.network.message.robot.RobotRunStateMessage;
import li.cil.oc2.common.network.message.robot.RobotTerminalInputMessage;
import li.cil.oc2.common.network.message.robot.RobotTerminalOutputMessage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = API.MOD_ID)
public final class Network {
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Computer
        registrar.playToClient(ComputerTerminalOutputMessage.TYPE, ComputerTerminalOutputMessage.STREAM_CODEC, ComputerTerminalOutputMessage::handleMessage);
        registrar.playToServer(ComputerTerminalInputMessage.TYPE, ComputerTerminalInputMessage.STREAM_CODEC, ComputerTerminalInputMessage::handleMessage);
        registrar.playToClient(ComputerRunStateMessage.TYPE, ComputerRunStateMessage.STREAM_CODEC, ComputerRunStateMessage::handleMessage);
        registrar.playToClient(ComputerBusStateMessage.TYPE, ComputerBusStateMessage.STREAM_CODEC, ComputerBusStateMessage::handleMessage);
        registrar.playToClient(ComputerBootErrorMessage.TYPE, ComputerBootErrorMessage.STREAM_CODEC, ComputerBootErrorMessage::handleMessage);
        registrar.playToServer(ComputerPowerMessage.TYPE, ComputerPowerMessage.STREAM_CODEC, ComputerPowerMessage::handleMessage);
        registrar.playToServer(MonitorPowerMessage.TYPE, MonitorPowerMessage.STREAM_CODEC, MonitorPowerMessage::handleMessage);
        registrar.playToClient(MonitorPowerMessageForwarded.TYPE, MonitorPowerMessageForwarded.STREAM_CODEC, MonitorPowerMessageForwarded::handleMessage);
        registrar.playToServer(OpenComputerInventoryMessage.TYPE, OpenComputerInventoryMessage.STREAM_CODEC, OpenComputerInventoryMessage::handleMessage);
        registrar.playToServer(OpenComputerTerminalMessage.TYPE, OpenComputerTerminalMessage.STREAM_CODEC, OpenComputerTerminalMessage::handleMessage);

        // Network
        registrar.playToClient(NetworkConnectorConnectionsMessage.TYPE, NetworkConnectorConnectionsMessage.STREAM_CODEC, NetworkConnectorConnectionsMessage::handleMessage);

        // Robot
        registrar.playToClient(RobotTerminalOutputMessage.TYPE, RobotTerminalOutputMessage.STREAM_CODEC, RobotTerminalOutputMessage::handleMessage);
        registrar.playToServer(RobotTerminalInputMessage.TYPE, RobotTerminalInputMessage.STREAM_CODEC, RobotTerminalInputMessage::handleMessage);
        registrar.playToClient(RobotRunStateMessage.TYPE, RobotRunStateMessage.STREAM_CODEC, RobotRunStateMessage::handleMessage);
        registrar.playToClient(RobotBusStateMessage.TYPE, RobotBusStateMessage.STREAM_CODEC, RobotBusStateMessage::handleMessage);
        registrar.playToClient(RobotBootErrorMessage.TYPE, RobotBootErrorMessage.STREAM_CODEC, RobotBootErrorMessage::handleMessage);
        registrar.playToServer(RobotPowerMessage.TYPE, RobotPowerMessage.STREAM_CODEC, RobotPowerMessage::handleMessage);
        registrar.playToServer(RobotInitializationRequestMessage.TYPE, RobotInitializationRequestMessage.STREAM_CODEC, RobotInitializationRequestMessage::handleMessage);
        registrar.playToClient(RobotInitializationMessage.TYPE, RobotInitializationMessage.STREAM_CODEC, RobotInitializationMessage::handleMessage);
        registrar.playToServer(OpenRobotInventoryMessage.TYPE, OpenRobotInventoryMessage.STREAM_CODEC, OpenRobotInventoryMessage::handleMessage);
        registrar.playToServer(OpenRobotTerminalMessage.TYPE, OpenRobotTerminalMessage.STREAM_CODEC, OpenRobotTerminalMessage::handleMessage);

        // Disk / Firmware
        registrar.playToClient(DiskDriveFloppyMessage.TYPE, DiskDriveFloppyMessage.STREAM_CODEC, DiskDriveFloppyMessage::handleMessage);
        registrar.playToClient(FirmwareFlasherMessage.TYPE, FirmwareFlasherMessage.STREAM_CODEC, FirmwareFlasherMessage::handleMessage);

        // Bus interface (bidirectional)
        registrar.playBidirectional(
                BusInterfaceNameMessage.TYPE, BusInterfaceNameMessage.STREAM_CODEC,
                new DirectionalPayloadHandler<>(BusInterfaceNameMessage::handleClientMessage, BusInterfaceNameMessage::handleServerMessage));

        // File import/export
        registrar.playToClient(ExportedFileMessage.TYPE, ExportedFileMessage.STREAM_CODEC, ExportedFileMessage::handleMessage);
        registrar.playToClient(RequestImportedFileMessage.TYPE, RequestImportedFileMessage.STREAM_CODEC, RequestImportedFileMessage::handleMessage);
        registrar.playToServer(ImportedFileMessage.TYPE, ImportedFileMessage.STREAM_CODEC, ImportedFileMessage::handleMessage);
        registrar.playToClient(ServerCanceledImportFileMessage.TYPE, ServerCanceledImportFileMessage.STREAM_CODEC, ServerCanceledImportFileMessage::handleMessage);
        registrar.playToServer(ClientCanceledImportFileMessage.TYPE, ClientCanceledImportFileMessage.STREAM_CODEC, ClientCanceledImportFileMessage::handleMessage);

        // Bus cable / Network config
        registrar.playToClient(BusCableFacadeMessage.TYPE, BusCableFacadeMessage.STREAM_CODEC, BusCableFacadeMessage::handleMessage);
        registrar.playToServer(NetworkInterfaceCardConfigurationMessage.TYPE, NetworkInterfaceCardConfigurationMessage.STREAM_CODEC, NetworkInterfaceCardConfigurationMessage::handleMessage);
        registrar.playToServer(NetworkTunnelLinkMessage.TYPE, NetworkTunnelLinkMessage.STREAM_CODEC, NetworkTunnelLinkMessage::handleMessage);

        // Monitor framebuffer
        registrar.playToServer(MonitorRequestFramebufferMessage.TYPE, MonitorRequestFramebufferMessage.STREAM_CODEC, MonitorRequestFramebufferMessage::handleMessage);
        registrar.playToClient(MonitorFramebufferMessage.TYPE, MonitorFramebufferMessage.STREAM_CODEC, MonitorFramebufferMessage::handleMessage);

        // Projector
        registrar.playToServer(ProjectorRequestFramebufferMessage.TYPE, ProjectorRequestFramebufferMessage.STREAM_CODEC, ProjectorRequestFramebufferMessage::handleMessage);
        registrar.playToClient(ProjectorFramebufferMessage.TYPE, ProjectorFramebufferMessage.STREAM_CODEC, ProjectorFramebufferMessage::handleMessage);
        registrar.playToClient(ProjectorStateMessage.TYPE, ProjectorStateMessage.STREAM_CODEC, ProjectorStateMessage::handleMessage);
        registrar.playToClient(MonitorStateMessage.TYPE, MonitorStateMessage.STREAM_CODEC, MonitorStateMessage::handleMessage);

        // Input
        registrar.playToServer(KeyboardInputMessage.TYPE, KeyboardInputMessage.STREAM_CODEC, KeyboardInputMessage::handleMessage);
        registrar.playToServer(MonitorInputMessage.TYPE, MonitorInputMessage.STREAM_CODEC, MonitorInputMessage::handleMessage);

        // Multipart
        registrar.playToServer(MultipartMessage.TYPE, MultipartMessage.STREAM_CODEC, MultipartMessage::handleMessage);
        MultipartMessage.registerMessage(ImportedFileMessage.class, ImportedFileMessage.STREAM_CODEC);
    }
}
