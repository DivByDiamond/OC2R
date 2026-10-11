package li.cil.oc2.common.item;

//? if >=26.1 {
/*import li.cil.oc2.platform.RegistrationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}
import net.minecraft.world.item.Item;

/** Item properties that carry the id of the item being registered where the game requires one. */
public final class ItemProps {
    private ItemProps() {}

    public static Item.Properties of() {
        //? if >=26.1 {
        /*return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, RegistrationContext.current()));
        *///?} else {
        return new Item.Properties();
        //?}
    }
}
