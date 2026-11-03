package li.cil.oc2.platform;

/**
 * Loader-independent fluid tank contract, mirroring the shape of NeoForge's {@code IFluidHandler}.
 * Reads and transfers preserve the complete fluid variant, including its component patch.
 */
public interface FluidHandler {
    /** Whether a fluid operation actually changes the tank, or only reports what would happen. */
    enum FluidAction {
        /** Apply the operation. */
        EXECUTE,
        /** Compute the result without changing the tank. */
        SIMULATE
    }

    /** The number of tanks in this handler. */
    int getTanks();

    /** The fluid in {@code tank}; the returned stack must not be modified. */
    FluidStack getFluidInTank(int tank);

    /** The capacity of {@code tank} in millibuckets. */
    int getTankCapacity(int tank);

    /** Whether {@code stack} could ever be filled into {@code tank}, ignoring current contents. */
    boolean isFluidValid(int tank, FluidStack stack);

    /** Fills {@code stack} into the tanks, returning the amount accepted. */
    int fill(FluidStack stack, FluidAction action);

    /** Drains up to {@code maxDrain} millibuckets matching {@code stack}, returning what was taken. */
    FluidStack drain(FluidStack stack, FluidAction action);

    /** Drains up to {@code maxDrain} millibuckets from the tanks, returning what was taken. */
    FluidStack drain(int maxDrain, FluidAction action);
}
