/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.adapter;

import com.google.gson.JsonArray;
import li.cil.ceres.api.Serialized;
import java.util.UUID;

@Serialized
public final class MethodInvocation {
    public UUID deviceId;
    public String methodName;
    public JsonArray parameters;

    @SuppressWarnings("unused") // For deserialization.
    public MethodInvocation() {
    }

    public MethodInvocation(final UUID deviceId, final String methodName, final JsonArray parameters) {
        this.deviceId = deviceId;
        this.methodName = methodName;
        this.parameters = parameters;
    }
}
