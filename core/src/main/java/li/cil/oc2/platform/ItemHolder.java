package li.cil.oc2.platform;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/** Handle to a registered item, valid once the loader has completed registration. */
public interface ItemHolder<T extends Item> extends Supplier<T>, ItemLike {
    ResourceLocation getId();

    @Override
    default Item asItem() {
        return get();
    }
}
