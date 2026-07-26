/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.element;

import javax.annotation.Nullable;
import java.util.Set;

abstract class GroupQueryResult<TEntry, TQuery> {
    @Nullable
    public abstract TQuery getQuery();

    public abstract Set<TEntry> getEntries();
}
