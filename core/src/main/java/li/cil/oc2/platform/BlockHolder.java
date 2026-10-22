package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Handle to a registered block, valid once the loader has completed registration. */
public interface BlockHolder<T extends Block> extends Supplier<T>, ItemLike {
    /**
     * The id of the registered block.
     *
     * @return the id of the block.
     */
    ResourceLocation getId();

    /**
     * The registry key of the registered block.
     *
     * @return the key of the block.
     */
    default ResourceKey<Block> key() {
        return ResourceKey.create(Registries.BLOCK, getId());
    }

    /**
     * The default state of the registered block.
     *
     * @return the block's default state.
     */
    default BlockState defaultBlockState() {
        return get().defaultBlockState();
    }

    @Override
    default Item asItem() {
        return get().asItem();
    }
}
