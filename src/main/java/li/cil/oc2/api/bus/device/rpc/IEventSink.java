
package li.cil.oc2.api.bus.device.rpc;
import com.google.gson.JsonElement;
import java.util.UUID;

/**
 * This interface handles events coming from RPCEventSources.
 * RPCDeviceBusAdapter implements this to relay events via the built in serial
 */

public interface IEventSink {
    void postEvent(UUID sourceid, JsonElement msg);
}
