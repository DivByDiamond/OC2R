package li.cil.oc2.platform;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

/** Immutable, lossless fluid variant and amount shared by loader adapters. */
public final class FluidStack {
    /** An empty stack: no fluid and zero amount. */
    public static final FluidStack EMPTY = new FluidStack(null, 0);

    private final Fluid fluid;
    private final int amount;
    private final DataComponentPatch components;

    /** Creates a stack of {@code amount} millibuckets of {@code fluid}. */
    public FluidStack(final Fluid fluid, final int amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    /** Creates a fluid variant without dropping loader-provided component data. */
    public FluidStack(final Fluid fluid, final int amount, final DataComponentPatch components) {
        this.fluid = fluid;
        this.components = components;
        this.amount = amount;
    }

    /** Returns the immutable component patch identifying this variant. */
    public DataComponentPatch getComponentsPatch() { return components; }

    /** Copies this variant with another amount, retaining its component patch. */
    public FluidStack withAmount(final int count) { return new FluidStack(fluid, count, components); }

    /** Tests resource identity and components independently of the stored amount. */
    @SuppressWarnings("PMD.CompareObjectsWithEquals") // Fluids are canonical registry objects.
    public boolean sameResource(final FluidStack other) {
        return fluid == other.fluid && components.equals(other.components);
    }

    /** The fluid in this stack, or {@code null} when empty. */
    public Fluid getFluid() {
        return fluid;
    }

    /** The amount of fluid in millibuckets. */
    public int getAmount() {
        return amount;
    }

    /** Whether this stack holds no fluid. */
    public boolean isEmpty() {
        return fluid == null || amount <= 0;
    }
}
