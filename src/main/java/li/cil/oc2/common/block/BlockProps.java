package li.cil.oc2.common.block;

//? if >=26.1 {
/*import li.cil.oc2.platform.RegistrationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
*///?}
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Block properties that carry the id of the block being registered where the game requires one. */
public final class BlockProps {
    private BlockProps() {}

    public static BlockBehaviour.Properties of() {
        //? if >=26.1 {
        /*return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, RegistrationContext.current()));
        *///?} else {
        return BlockBehaviour.Properties.of();
        //?}
    }
}
