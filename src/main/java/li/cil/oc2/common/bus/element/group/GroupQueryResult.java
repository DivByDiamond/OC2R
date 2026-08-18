package li.cil.oc2.common.bus.element.group;

import java.util.Set;
import javax.annotation.Nullable;

public abstract class GroupQueryResult<E, Q> {
    @Nullable
    public abstract Q getQuery();

    public abstract Set<E> getEntries();
}