package li.cil.oc2.common.bus.adapter.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.common.bus.adapter.EmptyMethodGroup;
import li.cil.oc2.common.bus.adapter.RPCDeviceWithIdentifier;

public class MessageWriter {
    private static final byte[] MESSAGE_DELIMITER = "\0".getBytes();
    private static final byte[] MESSAGE_DELIMITER2 = "\r".getBytes();

    private final Gson gson;
    private final BooleanSupplier crmode;
    private final Consumer<ByteBuffer> receiveBufferSink;

    public MessageWriter(
            final Gson gson,
            final BooleanSupplier crmode,
            final Consumer<ByteBuffer> receiveBufferSink) {
        this.gson = gson;
        this.crmode = crmode;
        this.receiveBufferSink = receiveBufferSink;
    }

    public void writeDeviceList(final List<RPCDeviceWithIdentifier> devices) {
        writeMessage(Message.MESSAGE_TYPE_LIST, devices);
    }

    public void writeDeviceMethods(final List<? extends RPCMethodGroup> methodGroups) {
        writeMessage(Message.MESSAGE_TYPE_METHODS, flattenMethodGroups(methodGroups));
    }

    public void writeResult(final Object result) {
        writeMessage(Message.MESSAGE_TYPE_RESULT, result);
    }

    public void writeError(final String message) {
        writeMessage(Message.MESSAGE_TYPE_ERROR, message);
    }

    public void postEvent(final UUID deviceId, final JsonElement msg) {
        writeMessage(Message.MESSAGE_TYPE_EVENT, new Object[] {deviceId, msg});
    }

    private List<Object> flattenMethodGroups(final List<? extends RPCMethodGroup> methodGroups) {
        final List<Object> result = new ArrayList<>();
        for (final RPCMethodGroup methodGroup : methodGroups) {
            final Set<RPCMethod> overloads = methodGroup.getOverloads();
            if (overloads.isEmpty()) {
                result.add(new EmptyMethodGroup(methodGroup.getName()));
            } else {
                result.addAll(overloads);
            }
        }
        return result;
    }

    private void writeMessage(final String type, @Nullable final Object data) {
        final String json = gson.toJson(new Message(type, data));
        final byte[] bytes = json.getBytes();
        final ByteBuffer buffer = ByteBuffer.allocate(bytes.length + MESSAGE_DELIMITER.length * 2);

        if (crmode.getAsBoolean()) {
            buffer.put(MESSAGE_DELIMITER2);
        } else {
            buffer.put(MESSAGE_DELIMITER);
        }

        buffer.put(bytes);

        if (crmode.getAsBoolean()) {
            buffer.put(MESSAGE_DELIMITER2);
        } else {
            buffer.put(MESSAGE_DELIMITER);
        }

        buffer.flip();
        receiveBufferSink.accept(buffer);
    }
}