package li.cil.oc2.common.bus.element.group;

import java.util.Set;
import javax.annotation.Nullable;

public abstract class GroupQueryResult<TEntry, TQuery> {
    @Nullable
    public abstract TQuery getQuery();

    public abstract Set<TEntry> getEntries();
}