package li.cil.oc2.common.event;

import li.cil.oc2.common.bus.device.data.FileSystems;
import li.cil.oc2.common.bus.device.rpc.filter.RPCItemStackTagFilters;
import li.cil.oc2.common.bus.device.vm.item.network.NetworkTunnelDevice;
import li.cil.oc2.common.serialization.BlobStorageEvents;
import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import li.cil.oc2.common.vm.memory.Allocator;

/**
 * Single place that subscribes common-side listeners to {@link
 * li.cil.oc2.platform.event.CommonEvents}. Called once from mod construction so no listener
 * depends on a loader-specific subscriber annotation.
 */
public final class CommonEventListeners {
    private CommonEventListeners() {}

    public static void register() {
        ForgeEventHandlers.register();
        BlobStorageEvents.register();
        FileSystems.register();
        RPCItemStackTagFilters.register();
        NetworkTunnelDevice.registerEvents();
        Allocator.register();
        ChunkUtils.register();
    }
}
