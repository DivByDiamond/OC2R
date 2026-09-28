package li.cil.oc2.common.network;

import li.cil.oc2.common.network.message.computer.ComputerBootErrorMessage;
import li.cil.oc2.common.network.message.computer.ComputerBusStateMessage;
import li.cil.oc2.common.network.message.computer.ComputerPowerMessage;
import li.cil.oc2.common.network.message.computer.ComputerRunStateMessage;
import li.cil.oc2.common.network.message.computer.SoundCardBeepMessage;
import li.cil.oc2.common.network.message.computer.SoundCardPcmMessage;
import li.cil.oc2.common.network.message.computer.terminal.ComputerTerminalDiffMessage;
import li.cil.oc2.common.network.message.computer.terminal.ComputerTerminalInputMessage;
import li.cil.oc2.common.network.message.computer.terminal.OpenComputerInventoryMessage;
import li.cil.oc2.common.network.message.computer.terminal.OpenComputerTerminalMessage;
import li.cil.oc2.common.network.message.file.ExportedFileMessage;
import li.cil.oc2.common.network.message.file.ImportedFileMessage;
import li.cil.oc2.common.network.message.file.RequestImportedFileMessage;
import li.cil.oc2.common.network.message.file.cancel.ClientCanceledImportFileMessage;
import li.cil.oc2.common.network.message.file.cancel.ServerCanceledImportFileMessage;
import li.cil.oc2.common.network.message.misc.MultipartMessage;
import li.cil.oc2.common.network.message.monitor.MonitorStateMessage;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorFramebufferMessage;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorPowerMessage;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorPowerMessageForwarded;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.monitor.input.KeyboardInputMessage;
import li.cil.oc2.common.network.message.monitor.input.MonitorInputMessage;
import li.cil.oc2.common.network.message.network.BusInterfaceNameMessage;
import li.cil.oc2.common.network.message.network.NetworkInterfaceCardConfigurationMessage;
import li.cil.oc2.common.network.message.network.connector.NetworkTunnelLinkMessage;
import li.cil.oc2.common.network.message.projector.ProjectorFramebufferMessage;
import li.cil.oc2.common.network.message.projector.ProjectorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.projector.ProjectorStateMessage;
import li.cil.oc2.common.network.message.robot.RobotBootErrorMessage;
import li.cil.oc2.common.network.message.robot.RobotBusStateMessage;
import li.cil.oc2.common.network.message.robot.RobotInitializationRequestMessage;
import li.cil.oc2.common.network.message.robot.inventory.OpenRobotInventoryMessage;
import li.cil.oc2.common.network.message.robot.state.RobotInitializationMessage;
import li.cil.oc2.common.network.message.robot.state.RobotPowerMessage;
import li.cil.oc2.common.network.message.robot.state.RobotRunStateMessage;
import li.cil.oc2.common.network.message.robot.terminal.OpenRobotTerminalMessage;
import li.cil.oc2.common.network.message.robot.terminal.RobotTerminalDiffMessage;
import li.cil.oc2.common.network.message.robot.terminal.RobotTerminalInputMessage;
import li.cil.oc2.platform.MessageRegistrar;

public final class Network {
    private Network() {
    }

    /** Registers every custom payload with {@code bridge}; called once per loader on setup. */
    public static void initialize(final MessageRegistrar bridge) {
        // Computer
        bridge.registerClientbound(ComputerTerminalDiffMessage.TYPE,
                ComputerTerminalDiffMessage.STREAM_CODEC,
                ComputerTerminalDiffMessage::handleMessage);
        bridge.registerServerbound(ComputerTerminalInputMessage.TYPE,
                ComputerTerminalInputMessage.STREAM_CODEC,
                ComputerTerminalInputMessage::handleMessage);
        bridge.registerClientbound(ComputerRunStateMessage.TYPE,
                ComputerRunStateMessage.STREAM_CODEC,
                ComputerRunStateMessage::handleMessage);
        bridge.registerClientbound(ComputerBusStateMessage.TYPE,
                ComputerBusStateMessage.STREAM_CODEC,
                ComputerBusStateMessage::handleMessage);
        bridge.registerClientbound(ComputerBootErrorMessage.TYPE,
                ComputerBootErrorMessage.STREAM_CODEC,
                ComputerBootErrorMessage::handleMessage);
        bridge.registerClientbound(SoundCardBeepMessage.TYPE,
                SoundCardBeepMessage.STREAM_CODEC,
                SoundCardBeepMessage::handleMessage);
        bridge.registerClientbound(SoundCardPcmMessage.TYPE,
                SoundCardPcmMessage.STREAM_CODEC,
                SoundCardPcmMessage::handleMessage);
        bridge.registerServerbound(ComputerPowerMessage.TYPE,
                ComputerPowerMessage.STREAM_CODEC,
                ComputerPowerMessage::handleMessage);
        bridge.registerServerbound(MonitorPowerMessage.TYPE,
                MonitorPowerMessage.STREAM_CODEC,
                MonitorPowerMessage::handleMessage);
        bridge.registerClientbound(MonitorPowerMessageForwarded.TYPE,
                MonitorPowerMessageForwarded.STREAM_CODEC,
                MonitorPowerMessageForwarded::handleMessage);
        bridge.registerServerbound(OpenComputerInventoryMessage.TYPE,
                OpenComputerInventoryMessage.STREAM_CODEC,
                OpenComputerInventoryMessage::handleMessage);
        bridge.registerServerbound(OpenComputerTerminalMessage.TYPE,
                OpenComputerTerminalMessage.STREAM_CODEC,
                OpenComputerTerminalMessage::handleMessage);

        // Network
        bridge.registerServerbound(NetworkTunnelLinkMessage.TYPE,
                NetworkTunnelLinkMessage.STREAM_CODEC,
                NetworkTunnelLinkMessage::handleMessage);

        // Robot
        bridge.registerClientbound(RobotTerminalDiffMessage.TYPE,
                RobotTerminalDiffMessage.STREAM_CODEC,
                RobotTerminalDiffMessage::handleMessage);
        bridge.registerServerbound(RobotTerminalInputMessage.TYPE,
                RobotTerminalInputMessage.STREAM_CODEC,
                RobotTerminalInputMessage::handleMessage);
        bridge.registerClientbound(RobotRunStateMessage.TYPE,
                RobotRunStateMessage.STREAM_CODEC,
                RobotRunStateMessage::handleMessage);
        bridge.registerClientbound(RobotBusStateMessage.TYPE,
                RobotBusStateMessage.STREAM_CODEC,
                RobotBusStateMessage::handleMessage);
        bridge.registerClientbound(RobotBootErrorMessage.TYPE,
                RobotBootErrorMessage.STREAM_CODEC,
                RobotBootErrorMessage::handleMessage);
        bridge.registerServerbound(RobotPowerMessage.TYPE,
                RobotPowerMessage.STREAM_CODEC,
                RobotPowerMessage::handleMessage);
        bridge.registerServerbound(RobotInitializationRequestMessage.TYPE,
                RobotInitializationRequestMessage.STREAM_CODEC,
                RobotInitializationRequestMessage::handleMessage);
        bridge.registerClientbound(RobotInitializationMessage.TYPE,
                RobotInitializationMessage.STREAM_CODEC,
                RobotInitializationMessage::handleMessage);
        bridge.registerServerbound(OpenRobotInventoryMessage.TYPE,
                OpenRobotInventoryMessage.STREAM_CODEC,
                OpenRobotInventoryMessage::handleMessage);
        bridge.registerServerbound(OpenRobotTerminalMessage.TYPE,
                OpenRobotTerminalMessage.STREAM_CODEC,
                OpenRobotTerminalMessage::handleMessage);

        // Disk / Firmware
        // (contents ride on the block entities' update tags via sendBlockUpdated)

        // Bus interface (client-to-server input from the GUI)
        bridge.registerServerbound(
                BusInterfaceNameMessage.TYPE,
                BusInterfaceNameMessage.STREAM_CODEC,
                BusInterfaceNameMessage::handleServerMessage);

        // File import/export
        bridge.registerClientbound(ExportedFileMessage.TYPE,
                ExportedFileMessage.STREAM_CODEC,
                ExportedFileMessage::handleMessage);
        bridge.registerClientbound(RequestImportedFileMessage.TYPE,
                RequestImportedFileMessage.STREAM_CODEC,
                RequestImportedFileMessage::handleMessage);
        bridge.registerServerbound(ImportedFileMessage.TYPE,
                ImportedFileMessage.STREAM_CODEC,
                ImportedFileMessage::handleMessage);
        bridge.registerClientbound(ServerCanceledImportFileMessage.TYPE,
                ServerCanceledImportFileMessage.STREAM_CODEC,
                ServerCanceledImportFileMessage::handleMessage);
        bridge.registerServerbound(ClientCanceledImportFileMessage.TYPE,
                ClientCanceledImportFileMessage.STREAM_CODEC,
                ClientCanceledImportFileMessage::handleMessage);

        // Bus cable / Network config
        bridge.registerServerbound(NetworkInterfaceCardConfigurationMessage.TYPE,
                NetworkInterfaceCardConfigurationMessage.STREAM_CODEC,
                NetworkInterfaceCardConfigurationMessage::handleMessage);

        // Monitor framebuffer
        bridge.registerServerbound(MonitorRequestFramebufferMessage.TYPE,
                MonitorRequestFramebufferMessage.STREAM_CODEC,
                MonitorRequestFramebufferMessage::handleMessage);
        bridge.registerClientbound(MonitorFramebufferMessage.TYPE,
                MonitorFramebufferMessage.STREAM_CODEC,
                MonitorFramebufferMessage::handleMessage);

        // Projector
        bridge.registerServerbound(ProjectorRequestFramebufferMessage.TYPE,
                ProjectorRequestFramebufferMessage.STREAM_CODEC,
                ProjectorRequestFramebufferMessage::handleMessage);
        bridge.registerClientbound(ProjectorFramebufferMessage.TYPE,
                ProjectorFramebufferMessage.STREAM_CODEC,
                ProjectorFramebufferMessage::handleMessage);
        bridge.registerClientbound(ProjectorStateMessage.TYPE,
                ProjectorStateMessage.STREAM_CODEC,
                ProjectorStateMessage::handleMessage);
        bridge.registerClientbound(MonitorStateMessage.TYPE,
                MonitorStateMessage.STREAM_CODEC,
                MonitorStateMessage::handleMessage);

        // Input
        bridge.registerServerbound(KeyboardInputMessage.TYPE,
                KeyboardInputMessage.STREAM_CODEC,
                KeyboardInputMessage::handleMessage);
        bridge.registerServerbound(MonitorInputMessage.TYPE,
                MonitorInputMessage.STREAM_CODEC,
                MonitorInputMessage::handleMessage);

        // Multipart (client->server and server->client for large payloads)
        bridge.registerBidirectional(
                MultipartMessage.TYPE,
                MultipartMessage.STREAM_CODEC,
                MultipartMessage::handleMessage,
                MultipartMessage::handleMessage);
        MultipartMessage.registerMessage(ImportedFileMessage.class, ImportedFileMessage.STREAM_CODEC);
        MultipartMessage.registerMessage(ExportedFileMessage.class, ExportedFileMessage.STREAM_CODEC);
    }
}
