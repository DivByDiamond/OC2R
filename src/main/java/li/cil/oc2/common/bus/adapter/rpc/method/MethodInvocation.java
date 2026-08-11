package li.cil.oc2.common.bus.adapter.rpc.method;

import com.google.gson.JsonArray;
import java.util.UUID;
import li.cil.ceres.api.Serialized;

@Serialized
public final class MethodInvocation {
    public UUID deviceId;
    public String methodName;
    public JsonArray parameters;

    @SuppressWarnings("unused") // For deserialization.
    public MethodInvocation() {}

    public MethodInvocation(
            final UUID deviceId, final String methodName, final JsonArray parameters) {
        this.deviceId = deviceId;
        this.methodName = methodName;
        this.parameters = parameters;
    }
}