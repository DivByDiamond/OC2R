package li.cil.oc2.common.item.tool;

import li.cil.oc2.common.item.ModItem;

public final class BlockOperationsModule extends ModItem {
    public static final int DURABILITY = 2500;

    public BlockOperationsModule() {
        super(createProperties().durability(DURABILITY));
    }
}
