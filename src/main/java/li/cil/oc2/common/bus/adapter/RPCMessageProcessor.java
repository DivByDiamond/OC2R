package li.cil.oc2.common.bus.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;

final class RPCMessageProcessor {

    static void processMessage(
            final RPCDeviceBusAdapter adapter,
            final byte[] messageData,
            final GsonContext ctx) {
        if (new String(messageData).isBlank()) return;
        try (final InputStreamReader stream =
                new InputStreamReader(new ByteArrayInputStream(messageData))) {
            final Message message = ctx.gson.fromJson(stream, Message.class);
            switch (message.type()) {
                case Message.MESSAGE_TYPE_LIST ->
                        ctx.messageWriter.writeDeviceList(ctx.deviceRegistry.devicesWithId);
                case Message.MESSAGE_TYPE_METHODS -> {
                    if (message.data() != null) {
                        final UUID deviceId = (UUID) message.data();
                        final RPCDevice device = ctx.deviceRegistry.devicesById.get(deviceId);
                        if (device != null) {
                            ctx.messageWriter.writeDeviceMethods(device.getMethodGroups());
                        } else {
                            ctx.messageWriter.writeError("unknown device");
                        }
                    } else {
                        ctx.messageWriter.writeError("missing device id");
                    }
                }
                case Message.MESSAGE_TYPE_INVOKE_METHOD -> {
                    if (message.data() != null) {
                        ctx.methodInvoker.processMethodInvocation(
                                (MethodInvocation) message.data(), false);
                    } else {
                        ctx.messageWriter.writeError("missing invocation data");
                    }
                }
                case Message.MESSAGE_TYPE_SUBSCRIBE -> {
                    if (message.data() != null) {
                        ctx.deviceRegistry.subscribe(
                                adapter, (UUID) message.data(), ctx.messageWriter::writeError);
                    } else {
                        ctx.messageWriter.writeError("missing invocation data");
                    }
                }
                case Message.MESSAGE_TYPE_UNSUBSCRIBE -> {
                    if (message.data() != null) {
                        ctx.deviceRegistry.unsubscribe(
                                adapter, (UUID) message.data(), ctx.messageWriter::writeError);
                    } else {
                        ctx.messageWriter.writeError("missing invocation data");
                    }
                }
                default -> ctx.messageWriter.writeError(
                        RPCDeviceBusAdapter.ERROR_UNKNOWN_MESSAGE_TYPE + message.type());
            }
        } catch (final Exception e) {
            ctx.messageWriter.writeError(e.getMessage());
        }
    }

    record GsonContext(
            com.google.gson.Gson gson,
            DeviceRegistry deviceRegistry,
            MessageWriter messageWriter,
            MethodInvoker methodInvoker) {}
}
