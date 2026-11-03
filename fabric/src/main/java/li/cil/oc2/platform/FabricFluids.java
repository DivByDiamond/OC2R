package li.cil.oc2.platform;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Fabric droplets are converted exactly: 81 droplets per millibucket, with component variants intact. */
final class FabricFluids implements FluidHandler {
    private static final long DROPLETS = 81;
    private final Storage<FluidVariant> storage;

    private FabricFluids(final Storage<FluidVariant> storage) { this.storage = storage; }

    @Nullable
    static <T> T findBlock(final BlockCapability<T> capability, final Level level,
                          final BlockPos pos, @Nullable final Direction side) {
        if (capability.getType() != FluidHandler.class) return null;
        final Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        return storage == null ? null : capability.getType().cast(new FabricFluids(storage));
    }

    private List<StorageView<FluidVariant>> tanks() {
        final List<StorageView<FluidVariant>> result = new ArrayList<>();
        storage.forEach(result::add);
        return result;
    }

    private static net.minecraft.core.component.DataComponentPatch components(final FluidVariant variant) {
        //? if >=26.1 {
        /*return variant.getComponentsPatch();
        *///?} else {
        return variant.getComponents();
        //?}
    }

    private static int millibuckets(final long droplets) {
        return (int) Math.min(Integer.MAX_VALUE, droplets / DROPLETS);
    }

    @Override
    public int getTanks() { return tanks().size(); }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        final StorageView<FluidVariant> view = tanks().get(tank);
        return view.isResourceBlank() ? FluidStack.EMPTY : new FluidStack(view.getResource().getFluid(),
                millibuckets(view.getAmount()), components(view.getResource()));
    }

    @Override
    public int getTankCapacity(final int tank) { return millibuckets(tanks().get(tank).getCapacity()); }

    @Override
    public boolean isFluidValid(final int tank, final FluidStack stack) {
        final StorageView<FluidVariant> view = tanks().get(tank);
        if (stack.isEmpty() || !(view instanceof Storage<?>)) return false;
        @SuppressWarnings("unchecked")
        final Storage<FluidVariant> selected = (Storage<FluidVariant>) view;
        try (Transaction transaction = Transaction.openOuter()) {
            return selected.insert(FluidVariant.of(stack.getFluid(), stack.getComponentsPatch()),
                    DROPLETS, transaction) > 0;
        }
    }

    @Override
    public int fill(final FluidStack stack, final FluidAction action) {
        if (stack.isEmpty()) return 0;
        return move(FluidVariant.of(stack.getFluid(), stack.getComponentsPatch()), stack.getAmount(), action, true);
    }

    @Override
    public FluidStack drain(final FluidStack stack, final FluidAction action) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        final int moved = move(FluidVariant.of(stack.getFluid(), stack.getComponentsPatch()),
                stack.getAmount(), action, false);
        return stack.withAmount(moved);
    }

    @Override
    public FluidStack drain(final int amount, final FluidAction action) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (final StorageView<FluidVariant> view : tanks()) {
            if (!view.isResourceBlank()) {
                final FluidVariant variant = view.getResource();
                final int moved = move(variant, amount, action, false);
                if (moved > 0) return new FluidStack(variant.getFluid(), moved, components(variant));
            }
        }
        return FluidStack.EMPTY;
    }

    private int move(final FluidVariant variant, final int amount, final FluidAction action, final boolean insert) {
        final long limit = amount * DROPLETS;
        final long possible;
        try (Transaction preview = Transaction.openOuter()) {
            possible = insert ? storage.insert(variant, limit, preview) : storage.extract(variant, limit, preview);
        }
        final long whole = possible / DROPLETS * DROPLETS;
        if (whole == 0 || action == FluidAction.SIMULATE) return millibuckets(whole);
        try (Transaction transaction = Transaction.openOuter()) {
            final long moved = insert ? storage.insert(variant, whole, transaction)
                    : storage.extract(variant, whole, transaction);
            // Never destroy or manufacture a fractional millibucket when a storage changes its answer.
            if (moved % DROPLETS != 0) return 0;
            transaction.commit();
            return millibuckets(moved);
        }
    }
}
