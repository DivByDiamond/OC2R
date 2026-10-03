package li.cil.oc2.platform;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * {@link CapabilityRegistrar} for Fabric: records providers for the bridge's own lookups and, for
 * the energy capability, also exposes block providers to Team Reborn Energy so other mods see them.
 */
public final class FabricCapabilityRegistrar implements CapabilityRegistrar {
    @Override
    public <T> void registerBlock(
            final BlockCapability<T> capability, final BlockCapabilityProvider<T> provider, final Block... blocks) {
        for (final Block block : blocks) {
            FabricCapabilities.addBlock(capability, provider, block);
        }
        FabricEnergy.exposeBlocks(capability, provider, blocks);
    }

    @Override
    public <T> void registerEntity(
            final EntityCapability<T> capability,
            final EntityType<?> entityType,
            final EntityCapabilityProvider<T> provider) {
        FabricCapabilities.addEntity(capability, entityType, provider);
    }

    @Override
    public <T> void registerItem(
            final ItemCapability<T> capability, final ItemCapabilityProvider<T> provider, final ItemLike... items) {
        for (final ItemLike item : items) {
            final Item asItem = item.asItem();
            FabricCapabilities.addItem(capability, provider, asItem);
        }
    }
}
