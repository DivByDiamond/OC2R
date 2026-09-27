package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Handle to a registered block, valid once the loader has completed registration. */
public interface BlockHolder<T extends Block> extends Supplier<T>, ItemLike {
    ResourceLocation getId();

    default BlockState defaultBlockState() {
        return get().defaultBlockState();
    }

    @Override
    default Item asItem() {
        return get().asItem();
    }
}
