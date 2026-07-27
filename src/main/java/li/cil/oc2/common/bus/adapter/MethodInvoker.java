package li.cil.oc2.common.bus.adapter;

import com.google.gson.Gson;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCInvocation;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;

class MethodInvoker {
    private final Gson gson;
    private final MessageWriter messageWriter;
    private final Function<UUID, RPCDeviceList> deviceLookup;
    private final Consumer<MethodInvocation> syncSetter;

    MethodInvoker(
            final Gson gson,
            final MessageWriter messageWriter,
            final Function<UUID, RPCDeviceList> deviceLookup,
            final Consumer<MethodInvocation> syncSetter) {
        this.gson = gson;
        this.messageWriter = messageWriter;
        this.deviceLookup = deviceLookup;
        this.syncSetter = syncSetter;
    }

    void processMethodInvocation(
            final MethodInvocation methodInvocation, final boolean isMainThread) {
        final RPCDevice device = deviceLookup.apply(methodInvocation.deviceId);
        if (device == null) {
            messageWriter.writeError(RPCDeviceBusAdapter.ERROR_UNKNOWN_DEVICE);
            return;
        }

        final RPCInvocation invocation = new RPCInvocationImpl(methodInvocation.parameters, gson);

        String error = RPCDeviceBusAdapter.ERROR_UNKNOWN_METHOD;
        for (final RPCMethodGroup methodGroup : device.getMethodGroups()) {
            if (!Objects.equals(methodGroup.getName(), methodInvocation.methodName)) continue;
            final Optional<RPCMethod> overload = methodGroup.findOverload(invocation);
            if (overload.isPresent()) {
                invokeMethod(methodInvocation, isMainThread, overload.get(), invocation);
                return;
            }
            error = RPCDeviceBusAdapter.ERROR_INVALID_PARAMETER_SIGNATURE;
        }
        messageWriter.writeError(error);
    }

    private void invokeMethod(
            final MethodInvocation methodInvocation,
            final boolean isMainThread,
            final RPCMethod method,
            final RPCInvocation invocation) {
        if (method.isSynchronized() && !isMainThread) {
            syncSetter.accept(methodInvocation);
            return;
        }
        try {
            final Object result = method.invoke(invocation);
            messageWriter.writeResult(result);
        } catch (final Throwable e) {
            messageWriter.writeError(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }
}