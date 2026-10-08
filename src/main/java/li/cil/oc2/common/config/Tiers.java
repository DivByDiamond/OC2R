package li.cil.oc2.common.config;

//? if >=26.1 {
/*import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

// Stand-in for net.minecraft.world.item.Tiers, which Minecraft 26.x replaced with ToolMaterial.
// It keeps the configurable tool tier names of the block operations module.
public enum Tiers {
    WOOD(ToolMaterial.WOOD),
    STONE(ToolMaterial.STONE),
    COPPER(ToolMaterial.COPPER),
    IRON(ToolMaterial.IRON),
    GOLD(ToolMaterial.GOLD),
    DIAMOND(ToolMaterial.DIAMOND),
    NETHERITE(ToolMaterial.NETHERITE);

    private final ToolMaterial material;

    Tiers(final ToolMaterial material) {
        this.material = material;
    }

    public TagKey<Block> getIncorrectBlocksForDrops() {
        return material.incorrectBlocksForDrops();
    }

    public int getUses() {
        return material.durability();
    }
}
*///?}
