package li.cil.oc2.common.blockentity.network.switches;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.blockentity.network.switches.host.HostEntry;
import li.cil.oc2.common.blockentity.network.switches.host.LuaHostEntry;
import li.cil.oc2.common.blockentity.network.switches.port.PacketProcessor;
import net.minecraft.nbt.*;

final class SwitchHostTable {
    private final Map<Long, HostEntry> hostTable = new ConcurrentHashMap<>();

    HostEntry get(long mac) {
        return hostTable.get(mac);
    }

    void put(long mac, int side, long tickTime) {
        if (hostTable.size() <= 256) {
            hostTable.put(mac, new HostEntry(side, tickTime));
        }
    }

    void removeExpired(long threshold) {
        hostTable.entrySet().removeIf(e -> e.getValue().timestamp < threshold);
    }

    List<LuaHostEntry> getHostTable(long now) {
        return hostTable.entrySet().stream()
                .map(
                        e ->
                                new LuaHostEntry(
                                        PacketProcessor.macLongToString(e.getKey()),
                                        now - e.getValue().timestamp,
                                        e.getValue().iface))
                .toList();
    }

    void save(final List<Tag> hosts) {
        for (Map.Entry<Long, HostEntry> host : hostTable.entrySet()) {
            // NOPMD: a distinct CompoundTag is required per entry as each is added to the list
            CompoundTag thisHost = new CompoundTag(); // NOPMD allocation depends on loop iteration / per-item state
            thisHost.put("mac", LongTag.valueOf(host.getKey()));
            thisHost.put("side", IntTag.valueOf(host.getValue().iface));
            thisHost.put("timestamp", LongTag.valueOf(host.getValue().timestamp));
            hosts.add(thisHost);
        }
    }

    void load(List<Tag> hosts) {
        for (Tag host_ : hosts) {
            CompoundTag host = (CompoundTag) host_;
            // NOPMD: entry depends on per-iteration host data
            hostTable.put(
                    host.getLong("mac"),
                    new HostEntry(host.getInt("side"), host.getLong("timestamp"))); // NOPMD allocation depends on loop iteration / per-item state
        }
    }
}