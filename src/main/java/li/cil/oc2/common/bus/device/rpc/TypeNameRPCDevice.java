package li.cil.oc2.common.bus.device.rpc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;

import java.util.List;

public record TypeNameRPCDevice(String typeName) implements RPCDevice, ItemDevice {
    @Override
    public List<String> getTypeNames() {
        return singletonList(typeName);
    }

    @Override
    public List<RPCMethodGroup> getMethodGroups() {
        return emptyList();
    }
}
