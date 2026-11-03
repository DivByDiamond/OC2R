package li.cil.oc2.common.integration.create;

import java.util.List;

/** Description of one current Create scroll box; option values are zero-based enum ordinals. */
public record CreateControlSetting(int id, String label, int value, String formatted,
                                   boolean active, List<String> options) {
    /** Publishes an immutable snapshot independent of caller-owned option lists. */
    public CreateControlSetting {
        options = List.copyOf(options);
    }
}
