package li.cil.oc2.platform;

import net.minecraft.world.level.material.Fluid;

/**
 * Loader-independent fluid amount, mirroring the fields of NeoForge's {@code FluidStack} that the
 * mod's code actually reads (fluid and amount). Components carried by a loader-specific stack are
 * dropped by the {@link CapabilityBridge} adapter: this type exists for cross-loader reading, not
 * for round-tripping foreign stacks.
 */
public final class FluidStack {
    /** An empty stack: no fluid and zero amount. */
    public static final FluidStack EMPTY = new FluidStack(null, 0);

    private final Fluid fluid;
    private final int amount;

    /** Creates a stack of {@code amount} millibuckets of {@code fluid}. */
    public FluidStack(final Fluid fluid, final int amount) {
        this.fluid = fluid;
        this.amount = amount;
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
