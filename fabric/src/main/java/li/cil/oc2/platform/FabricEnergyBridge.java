package li.cil.oc2.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Stateless {@link EnergyBridge} on top of the generic {@link CapabilityBridge}, exactly as on
 * NeoForge: energy is a capability under the shared {@code neoforge:energy} id.
 */
public final class FabricEnergyBridge implements EnergyBridge {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("neoforge", "energy");
    private static final BlockCapability<EnergyStorage> BLOCK = BlockCapability.createSided(ID, EnergyStorage.class);
    private static final EntityCapability<EnergyStorage> ENTITY = EntityCapability.createSided(ID, EnergyStorage.class);
    private static final ItemCapability<EnergyStorage> ITEM = ItemCapability.createVoid(ID, EnergyStorage.class);

    @Override
    @Nullable
    public EnergyStorage getBlockEnergy(final Level level, final BlockPos pos, @Nullable final Direction side) {
        return Platform.capabilities().getBlockCapability(BLOCK, level, pos, side);
    }

    @Override
    @Nullable
    public EnergyStorage getEntityEnergy(final Entity entity, @Nullable final Direction side) {
        return Platform.capabilities().getEntityCapability(ENTITY, entity, side);
    }

    @Override
    @Nullable
    public EnergyStorage getItemEnergy(final ItemStack stack) {
        return Platform.capabilities().getItemCapability(ITEM, stack);
    }
}
