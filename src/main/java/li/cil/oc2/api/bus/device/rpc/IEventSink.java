package li.cil.oc2.api.bus.device.rpc;

import com.google.gson.JsonElement;
import java.util.UUID;

/**
 * This interface handles events coming from RPCEventSources. RPCDeviceBusAdapter implements this to
 * relay events via the built in serial.
 */
public interface IEventSink {
    /**
     * Posts an event from an RPCEventSource to this sink.
     *
     * @param sourceid the unique identifier of the event source.
     * @param msg the event message payload.
     */
    void postEvent(UUID sourceid, JsonElement msg);
}