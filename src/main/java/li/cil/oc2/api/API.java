package li.cil.oc2.api;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

public final class API {
    private API() {}

    public static final String MOD_ID = "oc2r";

    ///////////////////////////////////////////////////////////////////

    /**
     * IMC message for registering Gson type adapters for method parameter serialization and
     * deserialization.
     *
     * <p>Must be called with a supplier that provides an instance of {@link
     * RPCMethodParameterTypeAdapter}.
     *
     * <p>It can be necessary to register additional serializers when implementing {@link
     * RPCMethod}s that use custom parameter types.
     *
     * @see GsonBuilder#registerTypeAdapter(Type, Object)
     * @see RPCMethod
     * @see Callback
     */
    public static final String IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER =
            "addRPCMethodParameterTypeAdapter";
}