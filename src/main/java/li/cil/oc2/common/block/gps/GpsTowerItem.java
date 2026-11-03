package li.cil.oc2.common.block.gps;

import li.cil.oc2.common.item.block.ModBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** One inventory item places the complete tower or leaves the world untouched. */
public final class GpsTowerItem extends ModBlockItem {
    public GpsTowerItem(final Block block) {
        super(block);
    }

    @Override
    protected boolean placeBlock(final BlockPlaceContext context, final BlockState state) {
        return GpsTowerStructure.place(context, state);
    }
}
