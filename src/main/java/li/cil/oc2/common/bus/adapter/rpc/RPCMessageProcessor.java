package li.cil.oc2.common.bus.adapter.rpc;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.adapter.DeviceRegistry;
import li.cil.oc2.common.bus.adapter.RPCDeviceBusAdapter;
import li.cil.oc2.common.bus.adapter.rpc.method.MethodInvocation;
import li.cil.oc2.common.bus.adapter.rpc.method.MethodInvoker;

public final class RPCMessageProcessor {

    public static void processMessage(
            final RPCDeviceBusAdapter adapter,
            final byte[] messageData,
            final GsonContext ctx) {
        if (new String(messageData).isBlank()) return;
        try (InputStreamReader stream =
                new InputStreamReader(new ByteArrayInputStream(messageData))) {
            final Message message = ctx.gson.fromJson(stream, Message.class);
            switch (message.type()) {
                case Message.MESSAGE_TYPE_LIST ->
                        ctx.messageWriter.writeDeviceList(ctx.deviceRegistry.devicesWithId);
                case Message.MESSAGE_TYPE_METHODS -> handleMethods(ctx, message);
                case Message.MESSAGE_TYPE_INVOKE_METHOD -> handleInvokeMethod(ctx, message);
                case Message.MESSAGE_TYPE_SUBSCRIBE -> handleSubscribe(adapter, ctx, message);
                case Message.MESSAGE_TYPE_UNSUBSCRIBE -> handleUnsubscribe(adapter, ctx, message);
                default -> ctx.messageWriter.writeError(
                        RPCDeviceBusAdapter.ERROR_UNKNOWN_MESSAGE_TYPE + message.type());
            }
        } catch (final Exception e) {
            ctx.messageWriter.writeError(e.getMessage());
        }
    }

    private static void handleMethods(final GsonContext ctx, final Message message) {
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

    private static void handleInvokeMethod(final GsonContext ctx, final Message message) {
        if (message.data() != null) {
            ctx.methodInvoker.processMethodInvocation(
                    (MethodInvocation) message.data(), false);
        } else {
            ctx.messageWriter.writeError("missing invocation data");
        }
    }

    private static void handleSubscribe(
            final RPCDeviceBusAdapter adapter, final GsonContext ctx, final Message message) {
        if (message.data() != null) {
            ctx.deviceRegistry.subscribe(
                    adapter, (UUID) message.data(), ctx.messageWriter::writeError);
        } else {
            ctx.messageWriter.writeError("missing invocation data");
        }
    }

    private static void handleUnsubscribe(
            final RPCDeviceBusAdapter adapter, final GsonContext ctx, final Message message) {
        if (message.data() != null) {
            ctx.deviceRegistry.unsubscribe(
                    adapter, (UUID) message.data(), ctx.messageWriter::writeError);
        } else {
            ctx.messageWriter.writeError("missing invocation data");
        }
    }

    public record GsonContext(
            Gson gson,
            DeviceRegistry deviceRegistry,
            MessageWriter messageWriter,
            MethodInvoker methodInvoker) {}
}
