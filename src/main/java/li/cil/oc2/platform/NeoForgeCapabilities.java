package li.cil.oc2.platform;

//? if <26.1 {
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
//?}
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
//? if >=26.1 {
/*import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
*///?} else {
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * Maps the loader-independent capability keys of {@link li.cil.oc2.platform} onto NeoForge's own
 * capability keys and value types.
 *
 * <p>Two kinds of keys exist: NeoForge's standard capabilities (energy, item handler, fluid
 * handler), which are addressed through NeoForge's constants so other mods keep seeing them under
 * their usual ids, and our own capabilities, whose NeoForge key is derived from the core key's id
 * (same ids as before the split, so third-party lookups are unaffected).
 *
 * <p>The same mapping adapts values in both directions: core values handed to NeoForge on
 * registration and NeoForge values handed back to core on lookup. Every wrapper compares equal
 * only to a wrapper over the <em>same</em> delegate instance (identity of the wrapped storage), for
 * the reason documented on {@link EnergyStorageWrapper}.
 */
final class NeoForgeCapabilities {
    private static final Map<CapKey, Object> BLOCK_KEYS = new ConcurrentHashMap<>();
    private static final Map<CapKey, Object> ENTITY_KEYS = new ConcurrentHashMap<>();
    private static final Map<CapKey, Object> ITEM_KEYS = new ConcurrentHashMap<>();

    private NeoForgeCapabilities() {}

    @SuppressWarnings("unchecked")
    static <T> BlockCapability<T, Object> blockKey(final li.cil.oc2.platform.BlockCapability<T> key) {
        return (BlockCapability<T, Object>)
                BLOCK_KEYS.computeIfAbsent(new CapKey(key), NeoForgeCapabilities::createBlockKey);
    }

    @SuppressWarnings("unchecked")
    static <T> EntityCapability<T, Object> entityKey(
            final li.cil.oc2.platform.EntityCapability<T> key) {
        return (EntityCapability<T, Object>)
                ENTITY_KEYS.computeIfAbsent(new CapKey(key), NeoForgeCapabilities::createEntityKey);
    }

    @SuppressWarnings("unchecked")
    static <T> ItemCapability<T, Object> itemKey(
            final li.cil.oc2.platform.ItemCapability<T> key) {
        return (ItemCapability<T, Object>)
                ITEM_KEYS.computeIfAbsent(new CapKey(key), NeoForgeCapabilities::createItemKey);
    }

    /**
     * Wraps a core value into the type NeoForge's key expects, for registration.
     *
     * @param container the item stack the handler was built from, for item fluid handlers; the
     *     value is ignored for every other type
     */
    static Object toNeoForge(
            final Class<?> type, final Object value, @Nullable final ItemStack container) {
        if (value == null) {
            return null;
        }
        if (type == EnergyStorage.class) {
            //? if >=26.1 {
            /*return new NeoForgeTransferAdapters.EnergyHandlerWrapper((EnergyStorage) value);
            *///?} else {
            return new EnergyStorageWrapper((EnergyStorage) value);
            //?}
        }
        if (type == ItemHandler.class) {
            //? if >=26.1 {
            /*return new NeoForgeTransferAdapters.ItemResourceHandlerWrapper((ItemHandler) value);
            *///?} else {
            return new ItemHandlerWrapper((ItemHandler) value);
            //?}
        }
        if (type == FluidHandler.class) {
            //? if >=26.1 {
            /*return new NeoForgeTransferAdapters.FluidResourceHandlerWrapper((FluidHandler) value);
            *///?} else {
            return new FluidHandlerWrapper((FluidHandler) value, container);
            //?}
        }
        return value;
    }

    /** Wraps a NeoForge value into the core type, for lookups. */
    @Nullable
    static Object fromNeoForge(final Class<?> type, @Nullable final Object value) {
        if (value == null) {
            return null;
        }
        // Values we registered ourselves come back wrapped by NeoForge's key; hand out the
        // original so identity survives the round trip.
        if (type == EnergyStorage.class) {
            //? if >=26.1 {
            /*return value instanceof final NeoForgeTransferAdapters.EnergyHandlerWrapper wrapper
                    ? wrapper.storage()
                    : new NeoForgeTransferAdapters.EnergyStorageView((EnergyHandler) value);
            *///?} else {
            return value instanceof final EnergyStorageWrapper wrapper
                    ? wrapper.storage
                    : new EnergyStorageView((IEnergyStorage) value);
            //?}
        }
        if (type == ItemHandler.class) {
            //? if >=26.1 {
            /*return value instanceof final NeoForgeTransferAdapters.ItemResourceHandlerWrapper wrapper
                    ? wrapper.delegate()
                    : new NeoForgeTransferAdapters.ItemHandlerView((ResourceHandler<ItemResource>) value);
            *///?} else {
            return value instanceof final ItemHandlerWrapper wrapper
                    ? wrapper.handler
                    : new ItemHandlerView((IItemHandler) value);
            //?}
        }
        if (type == FluidHandler.class) {
            //? if >=26.1 {
            /*return value instanceof final NeoForgeTransferAdapters.FluidResourceHandlerWrapper wrapper
                    ? wrapper.delegate()
                    : new NeoForgeTransferAdapters.FluidHandlerView((ResourceHandler<FluidResource>) value);
            *///?} else {
            return value instanceof final FluidHandlerWrapper wrapper
                    ? wrapper.handler
                    : new FluidHandlerView((IFluidHandler) value);
            //?}
        }
        return value;
    }

    /**
     * The context NeoForge's item lookups expect: 26.x addresses the standard item capabilities
     * (energy, items, fluids) through an {@code ItemAccess} over the stack, everything else, and
     * every lookup before 26.x, uses no context.
     */
    @Nullable
    static Object itemContext(final Class<?> type, final ItemStack stack) {
//? if >=26.1 {
/*        if (!stack.isEmpty()
                && (type == EnergyStorage.class
                        || type == ItemHandler.class
                        || type == FluidHandler.class)) {
            return ItemAccess.forStack(stack);
        }
*///?}
        return null;
    }

    private static Object createBlockKey(final CapKey key) {
        if (key.type == EnergyStorage.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
            //?}
        }
        if (key.type == ItemHandler.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK;
            //?}
        }
        if (key.type == FluidHandler.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;
            //?}
        }
        return key.sided
                ? BlockCapability.createSided(key.id, key.type)
                : BlockCapability.createVoid(key.id, key.type);
    }

    private static Object createEntityKey(final CapKey key) {
        if (key.type == EnergyStorage.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Energy.ENTITY;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ENTITY;
            //?}
        }
        if (key.type == ItemHandler.class) {
            //? if >=26.1 {
            /*return key.sided
                    ? net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY_AUTOMATION
                    : net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY;
            //?}
        }
        if (key.type == FluidHandler.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Fluid.ENTITY;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ENTITY;
            //?}
        }
        return key.sided
                ? EntityCapability.createSided(key.id, key.type)
                : EntityCapability.createVoid(key.id, key.type);
    }

    private static Object createItemKey(final CapKey key) {
        if (key.type == EnergyStorage.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Energy.ITEM;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM;
            //?}
        }
        if (key.type == ItemHandler.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Item.ITEM;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM;
            //?}
        }
        if (key.type == FluidHandler.class) {
            //? if >=26.1 {
            /*return net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM;
            *///?} else {
            return net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM;
            //?}
        }
        return ItemCapability.createVoid(key.id, key.type);
    }

    /** Cache key: the core key's identity is what distinguishes our own capabilities. */
    private record CapKey(ResourceLocation id, Class<?> type, boolean sided) {
        private CapKey(final li.cil.oc2.platform.BlockCapability<?> key) {
            this(key.getId(), key.getType(), key.isSided());
        }

        private CapKey(final li.cil.oc2.platform.EntityCapability<?> key) {
            this(key.getId(), key.getType(), key.isSided());
        }

        private CapKey(final li.cil.oc2.platform.ItemCapability<?> key) {
            this(key.getId(), key.getType(), true);
        }
    }

//? if <26.1 {
    /**
     * {@link IEnergyStorage} view over a core {@link EnergyStorage} whose {@link #equals(Object)}
     * and {@link #hashCode()} delegate to the reference identity of the wrapped storage.
     *
     * <p>Capability providers are re-queried on every device scan, and the device bus compares
     * devices through {@code ObjectDevice} → {@code EnergyStorageDevice} (an
     * {@code IdentityProxy}) → this wrapper. Two wrappers over the same storage must therefore be
     * equal, or the bus would treat the storage as a different device on every scan and re-add it
     * on every neighbor update (observed as a cascading rescan storm hanging a GameTestServer).
     * Wrappers over different storages must never be equal. Caching wrappers instead (an earlier
     * attempt) leaked one entry per query, so equality is used rather than a map.
     */
    static final class EnergyStorageWrapper implements IEnergyStorage {
        private final EnergyStorage storage;

        private EnergyStorageWrapper(final EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            return storage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return storage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive();
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final EnergyStorageWrapper other && other.storage.equals(storage);
        }

        @Override
        public int hashCode() {
            return storage.hashCode();
        }
    }

    /** Core {@link EnergyStorage} view over a NeoForge storage, identity-stable like above. */
    static final class EnergyStorageView implements EnergyStorage {
        private final IEnergyStorage storage;

        private EnergyStorageView(final IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            return storage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return storage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive();
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final EnergyStorageView other && other.storage.equals(storage);
        }

        @Override
        public int hashCode() {
            return storage.hashCode();
        }
    }

    /** NeoForge {@link IItemHandler} view over a core {@link ItemHandler}. */
    static final class ItemHandlerWrapper implements IItemHandlerModifiable {
        private final ItemHandler handler;

        private ItemHandlerWrapper(final ItemHandler handler) {
            this.handler = handler;
        }

        ItemHandler delegate() {
            return handler;
        }

        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(final int slot, final ItemStack stack) {
            handler.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(
                final int slot, final int amount, final boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final ItemHandlerWrapper other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    /** Core {@link ItemHandler} view over a NeoForge {@link IItemHandler}. */
    static final class ItemHandlerView implements ItemHandler {
        private final IItemHandler handler;

        ItemHandlerView(final IItemHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(final int slot, final ItemStack stack) {
            if (handler instanceof final IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, stack);
                return;
            }
            throw new UnsupportedOperationException(
                    "The queried item handler is read-only and does not support setStackInSlot.");
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(
                final int slot, final int amount, final boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final ItemHandlerView other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    /**
     * NeoForge {@link IFluidHandlerItem} view over a core {@link FluidHandler}. Item registrations
     * pass the originating stack so {@link #getContainer()} answers what NeoForge expects; block
     * and entity registrations pass {@code null} and expose an empty container.
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "IFluidHandlerItem.getContainer() contractually exposes the stack")
    static final class FluidHandlerWrapper implements IFluidHandlerItem {
        private final FluidHandler handler;
        private final ItemStack container;

        private FluidHandlerWrapper(final FluidHandler handler, @Nullable final ItemStack container) {
            this.handler = handler;
            this.container = container == null ? ItemStack.EMPTY : container;
        }

        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(final int tank) {
            return toNeoForgeFluid(handler.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(final int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(final int tank, final net.neoforged.neoforge.fluids.FluidStack stack) {
            return handler.isFluidValid(tank, toCoreFluid(stack));
        }

        @Override
        public int fill(final net.neoforged.neoforge.fluids.FluidStack stack, final IFluidHandler.FluidAction action) {
            return handler.fill(toCoreFluid(stack), toCoreAction(action));
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(
                final net.neoforged.neoforge.fluids.FluidStack resource,
                final IFluidHandler.FluidAction action) {
            return toNeoForgeFluid(handler.drain(toCoreFluid(resource), toCoreAction(action)));
        }

        @Override
        public net.neoforged.neoforge.fluids.FluidStack drain(final int maxDrain, final IFluidHandler.FluidAction action) {
            return toNeoForgeFluid(handler.drain(maxDrain, toCoreAction(action)));
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final FluidHandlerWrapper other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    /** Core {@link FluidHandler} view over a NeoForge {@link IFluidHandler}. */
    static final class FluidHandlerView implements FluidHandler {
        private final IFluidHandler handler;

        private FluidHandlerView(final IFluidHandler handler) {
            this.handler = handler;
        }

        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return toCoreFluid(handler.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(final int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return handler.isFluidValid(tank, toNeoForgeFluid(stack));
        }

        @Override
        public int fill(final FluidStack stack, final FluidHandler.FluidAction action) {
            return handler.fill(toNeoForgeFluid(stack), toNeoForgeAction(action));
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidHandler.FluidAction action) {
            return toCoreFluid(handler.drain(toNeoForgeFluid(resource), toNeoForgeAction(action)));
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidHandler.FluidAction action) {
            return toCoreFluid(handler.drain(maxDrain, toNeoForgeAction(action)));
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final FluidHandlerView other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    private static net.neoforged.neoforge.fluids.FluidStack toNeoForgeFluid(
            final FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
        return new net.neoforged.neoforge.fluids.FluidStack(
                stack.getFluid(), stack.getAmount());
    }

    @Nullable
    private static FluidStack toCoreFluid(
            @Nullable final net.neoforged.neoforge.fluids.FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(stack.getFluid(), stack.getAmount());
    }

    private static IFluidHandler.FluidAction toNeoForgeAction(
            final FluidHandler.FluidAction action) {
        return action == FluidHandler.FluidAction.EXECUTE
                ? IFluidHandler.FluidAction.EXECUTE
                : IFluidHandler.FluidAction.SIMULATE;
    }

    private static FluidHandler.FluidAction toCoreAction(
            final IFluidHandler.FluidAction action) {
        return action == IFluidHandler.FluidAction.EXECUTE
                ? FluidHandler.FluidAction.EXECUTE
                : FluidHandler.FluidAction.SIMULATE;
    }
//?}
}
