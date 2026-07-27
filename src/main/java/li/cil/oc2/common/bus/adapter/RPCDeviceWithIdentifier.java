package li.cil.oc2.common.bus.adapter;

import java.util.UUID;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;

public record RPCDeviceWithIdentifier(UUID identifier, RPCDevice device) {}