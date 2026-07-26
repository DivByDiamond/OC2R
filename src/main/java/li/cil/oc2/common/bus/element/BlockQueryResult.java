package li.cil.oc2.common.bus.element;

import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;

import java.util.Set;

public final class BlockQueryResult extends GroupQueryResult<BlockEntry, BlockDeviceQuery> {
    private final BlockDeviceQuery query;
    private final Set<BlockEntry> entries;

    public BlockQueryResult(final BlockDeviceQuery query, final Set<BlockEntry> entries) {
        this.query = query;
        this.entries = entries;
    }

    public BlockDeviceQuery getQuery() {
        return query;
    }

    @Override
    public Set<BlockEntry> getEntries() {
        return entries;
    }
}
