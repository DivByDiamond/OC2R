package li.cil.oc2.fabric.gametest;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.FluidHandler;
import li.cil.oc2.platform.FluidStack;
import li.cil.oc2.platform.Platform;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.gametest.v1.GameTest;
*///?} else {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
//?}
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/** Native Fabric storage lookups and exact droplet conversion exercised in a real server. */
public final class FabricMachineStorageTests
        //? if <26.1 {
        implements FabricGameTest
        //?}
{
    private static final NativeTank TANK = new NativeTank();
    static {
        FluidStorage.SIDED.registerForBlocks((level, pos, state, entity, side) -> TANK, Blocks.EMERALD_BLOCK);
    }

    //? if >=26.1 {
    /*@GameTest(structure = "oc2r:empty", environment = "oc2r_gametest:oc2r_fabric_storage", maxTicks = 100)
    *///?} else {
    @GameTest(template = EMPTY_STRUCTURE, batch = "oc2r_fabric_storage")
    //?}
    public void fluidVariantsSimulationAndFractionalDroplets(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.EMERALD_BLOCK);
        TANK.variant = FluidVariant.blank();
        TANK.amount = 0;
        final FluidHandler handler = Platform.capabilities().getBlockCapability(Capabilities.FluidHandler.BLOCK,
                helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        if (handler == null) throw new IllegalStateException("Foreign Fabric fluid handler not found");
        final DataComponentPatch components = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Coolant")).build();
        final FluidStack value = new FluidStack(Fluids.WATER, 1000, components);
        check(handler.fill(value, FluidHandler.FluidAction.SIMULATE) == 1000 && TANK.amount == 0,
                "Simulation changed native storage");
        check(handler.fill(value, FluidHandler.FluidAction.EXECUTE) == 1000 && TANK.amount == 81000,
                "Millibuckets did not convert exactly to droplets");
        check(handler.getFluidInTank(0).getComponentsPatch().equals(components), "Fluid components lost");
        check(handler.drain(value.withAmount(400), FluidHandler.FluidAction.EXECUTE).getAmount() == 400
                && TANK.amount == 48600, "Native extraction failed");
        TANK.amount = 40;
        check(handler.drain(1000, FluidHandler.FluidAction.EXECUTE).isEmpty() && TANK.amount == 40,
                "Fractional millibucket was destroyed");
        TANK.amount = 810000 - 40;
        check(handler.fill(value, FluidHandler.FluidAction.EXECUTE) == 0 && TANK.amount == 810000 - 40,
                "Fractional capacity manufactured a millibucket");
        helper.succeed();
    }

    private static void check(final boolean condition, final String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class NativeTank extends SingleFluidStorage {
        @Override
        protected long getCapacity(final FluidVariant variant) { return 810000; }
    }
}
