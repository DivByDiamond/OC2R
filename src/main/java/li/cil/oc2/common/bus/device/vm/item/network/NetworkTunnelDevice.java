package li.cil.oc2.common.bus.device.vm.item.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.time.Duration;
import java.util.*;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.bus.device.vm.item.AbstractNetworkInterfaceDevice;
import li.cil.oc2.common.item.network.NetworkTunnelItem;
import li.cil.oc2.common.util.tick.TickUtils;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.world.item.ItemStack;

public final class NetworkTunnelDevice extends AbstractNetworkInterfaceDevice {
    public NetworkTunnelDevice(final ItemStack identity) {
        super(identity);
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        final VMDeviceLoadResult result = super.mount(context);
        if (result.wasSuccessful()) {
            NetworkTunnelItem.getTunnelId(identity)
                    .ifPresent(id -> TunnelManager.registerEndpoint(id, getNetworkInterface()));
        }
        return result;
    }

    /** Subscribes the tunnel pump to the server tick and shutdown events. */
    public static void registerEvents() {
        TunnelManager.register();
    }

    @Override
    public void unmount() {
        super.unmount();
        TunnelManager.unregisterEndpoint(getNetworkInterface());
    }

    private static final class TunnelManager {
        private static final int BYTES_PER_TICK =
                32 * 1024 / TickUtils.toTicks(Duration.ofSeconds(1)); // bytes / sec -> bytes / tick
        private static final int MIN_ETHERNET_FRAME_SIZE = 42;

        private static final BiMap<UUID, Set<NetworkInterface>> TUNNELS = HashBiMap.create();

        public static void registerEndpoint(
                final UUID id, final NetworkInterface networkInterface) {
            TUNNELS.computeIfAbsent(id, unused -> new HashSet<>()).add(networkInterface);
        }

        public static void unregisterEndpoint(final NetworkInterface networkInterface) {
            for (final Set<NetworkInterface> tunnel : TUNNELS.values()) {
                tunnel.remove(networkInterface);
            }
        }

        static void register() {
            CommonEvents.SERVER_TICK_START.register(server -> pumpMessages());
            CommonEvents.SERVER_STOPPED.register(server -> TUNNELS.clear());
        }

        private static void pumpMessages() {
            final Iterator<Set<NetworkInterface>> iterator = TUNNELS.values().iterator();
            while (iterator.hasNext()) {
                final Set<NetworkInterface> tunnel = iterator.next();
                if (tunnel.isEmpty()) {
                    iterator.remove();
                } else {
                    pumpMessages(tunnel);
                }
            }
        }

        private static void pumpMessages(final Collection<NetworkInterface> tunnel) {
            for (final NetworkInterface source : tunnel) {
                int byteBudget = BYTES_PER_TICK;
                byte[] frame = source.readEthernetFrame();
                while (frame != null && byteBudget > 0) {
                    byteBudget -=
                            Math.max(
                                    frame.length,
                                    MIN_ETHERNET_FRAME_SIZE); // Avoid bogus packets messing with
                    // us.
                    for (final NetworkInterface destination : tunnel) {
                        if (!destination.equals(source)) {
                            destination.writeEthernetFrame(source, frame, 1);
                        }
                    }
                    frame = source.readEthernetFrame();
                }
            }
        }
    }
}