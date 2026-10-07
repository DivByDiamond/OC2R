package li.cil.oc2.platform;

import net.minecraft.world.item.ItemStack;

/**
 * Loader-independent item inventory contract, matching the shape of NeoForge's
 * {@code IItemHandler}/{@code IItemHandlerModifiable} pair (the de-facto Forge item capability
 * standard). Implementations are the mod's own inventories, exposed to the loader's capability
 * system through {@link CapabilityRegistrar}; {@link CapabilityBridge} wraps whatever another
 * block, entity or item exposes back into this same shape.
 *
 * <p>Method signatures deliberately mirror the NeoForge interfaces so a NeoForge inventory
 * implements both without any bridging code.
 */
public interface ItemHandler {
    /** The number of slots in this inventory. */
    int getSlots();

    /** The stack in {@code slot}; the returned stack must not be modified. */
    ItemStack getStackInSlot(int slot);

    /**
     * Replaces the stack in {@code slot} without validation, bypassing insertion rules.
     *
     * @throws UnsupportedOperationException if this inventory is read-only
     */
    void setStackInSlot(int slot, ItemStack stack);

    /** Inserts {@code stack} into {@code slot}, returning the remainder. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Removes up to {@code amount} items from {@code slot}, returning the extracted stack. */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    /** The maximum stack size accepted by {@code slot}. */
    int getSlotLimit(int slot);

    /** Whether {@code stack} could ever be inserted into {@code slot}, ignoring current contents. */
    boolean isItemValid(int slot, ItemStack stack);
}
