package li.cil.oc2.common.bus.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.adapter.rpc.Message;
import li.cil.oc2.common.bus.adapter.rpc.MessageWriter;
import li.cil.oc2.common.bus.adapter.rpc.RPCMessageProcessor;
import li.cil.oc2.common.bus.adapter.rpc.method.MethodInvocation;
import li.cil.oc2.common.bus.adapter.rpc.method.MethodInvoker;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.serialization.gson.SideJsonDeserializer;
import li.cil.oc2.common.serialization.gson.UnsignedByteArrayJsonSerializer;
import li.cil.oc2.common.serialization.gson.rpc.EmptyRPCMethodGroupSerializer;
import li.cil.oc2.common.serialization.gson.rpc.RPCDeviceWithIdentifierJsonSerializer;
import li.cil.oc2.common.serialization.gson.rpc.RPCMethodJsonSerializer;
import li.cil.oc2.common.serialization.gson.rpc.invocation.MessageJsonDeserializer;
import li.cil.oc2.common.serialization.gson.rpc.invocation.MethodInvocationJsonDeserializer;
import li.cil.sedna.api.device.Steppable;
import li.cil.sedna.api.device.serial.SerialDevice;

public final class RPCDeviceBusAdapter implements Steppable, IEventSink {
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 4 * Constants.KILOBYTE;

    public static final String ERROR_MESSAGE_TOO_LARGE = "message too large";
    public static final String ERROR_UNKNOWN_MESSAGE_TYPE = "unknown message type: ";
    public static final String ERROR_UNKNOWN_DEVICE = "unknown device";
    public static final String ERROR_UNKNOWN_METHOD = "unknown method";
    public static final String ERROR_INVALID_PARAMETER_SIGNATURE = "invalid parameter signature";

    private final SerialDevice serialDevice;
    private final Gson gson;
    private final DeviceRegistry deviceRegistry;
    private final MessageWriter messageWriter;
    private final MethodInvoker methodInvoker;
    private final Lock pauseLock = new ReentrantLock();
    private boolean isPaused;
    private boolean crmode;
    @Serialized private ByteBuffer transmitBuffer;
    @Serialized private ByteBuffer receiveBuffer;
    @Serialized private MethodInvocation synchronizedInvocation;

    public RPCDeviceBusAdapter(final SerialDevice serialDevice) {
        this(serialDevice, DEFAULT_MAX_MESSAGE_SIZE);
    }

    public RPCDeviceBusAdapter(final SerialDevice serialDevice, final int maxMessageSize) {
        this.serialDevice = serialDevice;
        this.transmitBuffer = ByteBuffer.allocate(maxMessageSize);
        this.gson =
                RPCMethodParameterTypeAdapters.beginBuildGson()
                        .registerTypeAdapter(byte[].class, new UnsignedByteArrayJsonSerializer())
                        .registerTypeAdapter(
                                MethodInvocation.class, new MethodInvocationJsonDeserializer())
                        .registerTypeAdapter(Message.class, new MessageJsonDeserializer())
                        .registerTypeAdapter(
                                RPCDeviceWithIdentifier.class,
                                new RPCDeviceWithIdentifierJsonSerializer())
                        .registerTypeHierarchyAdapter(
                                RPCMethod.class, new RPCMethodJsonSerializer())
                        .registerTypeAdapter(
                                EmptyMethodGroup.class, new EmptyRPCMethodGroupSerializer())
                        .registerTypeAdapter(Side.class, new SideJsonDeserializer())
                        .create();
        this.deviceRegistry = new DeviceRegistry();
        this.messageWriter = new MessageWriter(gson, () -> crmode, buf -> receiveBuffer = buf);
        this.methodInvoker =
                new MethodInvoker(
                        gson,
                        messageWriter,
                        id -> deviceRegistry.devicesById.get(id),
                        mi -> synchronizedInvocation = mi);
    }

    public void mountDevices() {
        deviceRegistry.mountDevices();
    }

    public void unmountDevices() {
        deviceRegistry.unmountDevices();
    }

    public void disposeDevices() {
        deviceRegistry.disposeDevices(this);
    }

    public void reset() {
        transmitBuffer.clear();
        receiveBuffer = null;
        synchronizedInvocation = null;
    }

    public void pause() {
        if (isPaused) return;
        pauseLock.lock();
        isPaused = true;
        pauseLock.unlock();
    }

    public void resume(final DeviceBusController controller, final boolean didDevicesChange) {
        isPaused = false;
        if (didDevicesChange) {
            deviceRegistry.rebuild(controller);
        }
    }

    public void tick() {
        if (isPaused) return;
        if (synchronizedInvocation != null) {
            final MethodInvocation mi = synchronizedInvocation;
            methodInvoker.processMethodInvocation(mi, true);
            synchronizedInvocation = null;
        }
    }

    @Override
    public void step(final int cycles) {
        if (isPaused || !pauseLock.tryLock()) return;
        try {
            readFromDevice();
            writeToDevice();
        } finally {
            pauseLock.unlock();
        }
    }

    private void readFromDevice() {
        while (receiveBuffer == null
                && synchronizedInvocation == null) {
            final int value = serialDevice.read();
            if (value < 0) break;
            if (value == 0 || value == 13) {
                crmode = value == 13;
                if (transmitBuffer.limit() > 0) {
                    transmitBuffer.flip();
                    if (transmitBuffer.hasRemaining()) {
                        final byte[] message = new byte[transmitBuffer.remaining()];
                        transmitBuffer.get(message);
                        processMessage(message);
                    }
                } else {
                    messageWriter.writeError(ERROR_MESSAGE_TOO_LARGE);
                }
                transmitBuffer.clear();
            } else if (transmitBuffer.hasRemaining()) {
                transmitBuffer.put((byte) value);
            } else {
                transmitBuffer.clear();
                transmitBuffer.limit(0);
            }
        }
    }

    private void writeToDevice() {
        if (receiveBuffer == null) return;
        while (receiveBuffer.hasRemaining() && serialDevice.canPutByte()) {
            serialDevice.putByte(receiveBuffer.get());
        }
        serialDevice.flush();
        if (!receiveBuffer.hasRemaining()) {
            receiveBuffer = null;
        }
    }

    private void processMessage(final byte[] messageData) {
        RPCMessageProcessor.processMessage(this, messageData,
                new RPCMessageProcessor.GsonContext(gson, deviceRegistry, messageWriter, methodInvoker));
    }

    @Override
    public void postEvent(final UUID deviceId, final JsonElement msg) {
        messageWriter.postEvent(deviceId, msg);
    }
}