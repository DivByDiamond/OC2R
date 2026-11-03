package li.cil.oc2.common.blockentity.disk;

import net.minecraft.world.item.ItemStack;

/** Common render input for the legacy drive and the two-slot media station. */
public interface FloppyVisualSource {
    ItemStack getRenderedFloppy();
}
