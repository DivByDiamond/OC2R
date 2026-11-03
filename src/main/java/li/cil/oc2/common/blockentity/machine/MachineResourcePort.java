package li.cil.oc2.common.blockentity.machine;

import java.util.Map;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.BlockCapability;
import li.cil.oc2.platform.FluidStack;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;

/** Resource transfers use the attached machine's sided APIs, with limits per RPC call. */
public interface MachineResourcePort {
    MachineInterfaceBlockEntity machine();

    @Callback
    default int getEnergyStored() {
        machine().checkAttached();
        return machine().resources.energy();
    }

    @Callback
    default Map<String, Object> getFluidStored() {
        machine().checkAttached();
        final FluidStack stack = machine().resources.fluid();
        final FluidStack pending = machine().resources.recovery();
        return Map.of("fluid", stack.isEmpty() ? "minecraft:empty"
                        : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),
                "amount", stack.getAmount(), "capacity", 8000,
                "recoveryAmount", pending.getAmount(), "recoveryFluid", pending.isEmpty() ? "minecraft:empty"
                        : BuiltInRegistries.FLUID.getKey(pending.getFluid()).toString());
    }

    @Callback
    default int getTargetEnergyStored() {
        return target(Capabilities.EnergyStorage.BLOCK).getEnergyStored();
    }

    @Callback
    default int getTargetTankCount() {
        return target(Capabilities.FluidHandler.BLOCK).getTanks();
    }

    @Callback
    default Map<String, Object> getTargetFluid(@Parameter("tank") final int tank) {
        final var handler = target(Capabilities.FluidHandler.BLOCK);
        if (tank < 0 || tank >= handler.getTanks()) throw new IllegalArgumentException("Invalid tank index");
        final FluidStack stack = handler.getFluidInTank(tank);
        return Map.of("fluid", stack.isEmpty() ? "minecraft:empty"
                        : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),
                "amount", stack.getAmount(), "capacity", handler.getTankCapacity(tank));
    }

    @Callback
    default int pullEnergy(@Parameter("amount") final int amount) {
        return machine().resources.moveEnergy(target(Capabilities.EnergyStorage.BLOCK), amount, true);
    }

    @Callback
    default int pushEnergy(@Parameter("amount") final int amount) {
        return machine().resources.moveEnergy(target(Capabilities.EnergyStorage.BLOCK), amount, false);
    }

    @Callback
    default int pullFluid(@Parameter("amount") final int amount) {
        return machine().resources.moveFluid(target(Capabilities.FluidHandler.BLOCK), amount, true);
    }

    @Callback
    default int pushFluid(@Parameter("amount") final int amount) {
        return machine().resources.moveFluid(target(Capabilities.FluidHandler.BLOCK), amount, false);
    }

    @Callback
    default int recoverFluid() {
        machine().checkAttached();
        return machine().resources.recoverFluid();
    }

    private <T> T target(final BlockCapability<T> capability) {
        final MachineInterfaceBlockEntity self = machine();
        self.targetState();
        final T handler = Platform.capabilities().getBlockCapability(capability,
                self.getLevel(), self.getBlockPos().relative(self.facing()), self.facing().getOpposite());
        if (handler == null) throw new IllegalStateException("Target exposes no " + capability.getId());
        return handler;
    }
}
