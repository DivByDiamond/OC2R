package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/** Handle to a registered item, valid once the loader has completed registration. */
public interface ItemHolder<T extends Item> extends Supplier<T>, ItemLike {
    /**
     * The id of the registered item.
     *
     * @return the id of the item.
     */
    ResourceLocation getId();

    /**
     * The registry key of the registered item.
     *
     * @return the key of the item.
     */
    default ResourceKey<Item> key() {
        return ResourceKey.create(Registries.ITEM, getId());
    }

    @Override
    default Item asItem() {
        return get();
    }
}
