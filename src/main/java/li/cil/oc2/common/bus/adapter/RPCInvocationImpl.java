/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.oc2.api.bus.device.rpc.RPCInvocation;
import li.cil.oc2.api.bus.device.rpc.RPCParameter;
import java.util.Optional;

record RPCInvocationImpl(JsonArray parameters, Gson gson) implements RPCInvocation {
    @Override
    public JsonArray getParameters() {
        return parameters;
    }

    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public Optional<Object[]> tryDeserializeParameters(final RPCParameter... parameterTypes) {
        if (parameterTypes.length != parameters.size()) {
            return Optional.empty();
        }

        final Object[] result = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            final RPCParameter parameterInfo = parameterTypes[i];
            try {
                result[i] = gson.fromJson(parameters.get(i), parameterInfo.getType());
            } catch (final Throwable e) {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }
}
