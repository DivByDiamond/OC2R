package li.cil.oc2.gametest;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.common.machine.MachineInventory;
import li.cil.oc2.common.machine.MachineResourcePersistence;
import li.cil.oc2.common.machine.MachineResources;
import li.cil.oc2.platform.FluidHandler;
import li.cil.oc2.platform.FluidStack;
import li.cil.oc2.platform.ItemStackHandler;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
//? if >=26.1 {
/*import li.cil.oc2.gametest.GameTestRegistrar.GameTest;
import li.cil.oc2.gametest.GameTestRegistrar.GameTestHolder;
import li.cil.oc2.gametest.GameTestRegistrar.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineTransferTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineEscrowsPartialInsertionAndDefersRemoval(final GameTestHelper helper) {
        final MachineInventory buffer = new MachineInventory(() -> {});
        final List<ItemStack> drops = new ArrayList<>();
        buffer.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        final ItemStackHandler target = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
                if (simulate) return ItemStack.EMPTY;
                final ItemStack remainder = super.insertItem(slot, stack.copyWithCount(7), false);
                TestSupport.assertTrue(helper, "test target accepted seven", remainder.isEmpty());
                buffer.close(drops::add);
                return stack.copyWithCount(stack.getCount() - 7);
            }
        };
        TestSupport.assertEquals(helper, "actual accepted", 7, buffer.transfer(buffer, 0, target, 0, 20));
        final int dropped = drops.stream().mapToInt(ItemStack::getCount).sum();
        TestSupport.assertEquals(helper, "removed during insertion conserves items", 20,
                dropped + target.getStackInSlot(0).getCount());
        TestSupport.assertTrue(helper, "removed buffer empty", buffer.isEmpty());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineRejectedRemainderSurvivesSaveLoad(final GameTestHelper helper) {
        final MachineInventory buffer = new MachineInventory(() -> {});
        buffer.setStackInSlot(0, new ItemStack(Items.DIAMOND, 20));
        final ItemStackHandler target = new ItemStackHandler(1) {
            @Override
            public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
                return simulate ? ItemStack.EMPTY : stack.copy();
            }
        };
        TestSupport.assertEquals(helper, "nothing accepted", 0, buffer.transfer(buffer, 0, target, 0, 20));
        final MachineInventory restored = new MachineInventory(() -> {});
        final var registries = helper.getLevel().registryAccess();
        restored.deserializeNBT(registries, buffer.serializeNBT(registries));
        TestSupport.assertEquals(helper, "escrow persisted", 20, restored.getStackInSlot(18).getCount());
        TestSupport.assertEquals(helper, "escrow recovered", 20, restored.recover());
        TestSupport.assertEquals(helper, "buffer recovered", 20, restored.getStackInSlot(0).getCount());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machineFluidComponentsAndCountsSurviveTransfersAndSave(final GameTestHelper helper) {
        final DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Special coolant")).build();
        final Tank source = new Tank(new FluidStack(Fluids.WATER, 3000, patch));
        final MachineResources buffer = new MachineResources(() -> {});
        TestSupport.assertEquals(helper, "pulled", 1000, buffer.moveFluid(source, 1000, true));
        TestSupport.assertTrue(helper, "variant preserved", buffer.fluid().getComponentsPatch().equals(patch));
        final CompoundTag tag = new CompoundTag();
        final var registries = helper.getLevel().registryAccess();
        MachineResourcePersistence.save(buffer, tag, registries);
        final MachineResources restored = new MachineResources(() -> {});
        MachineResourcePersistence.load(restored, tag, registries);
        TestSupport.assertTrue(helper, "variant persisted", restored.fluid().getComponentsPatch().equals(patch));
        final Tank sink = new Tank(FluidStack.EMPTY);
        sink.actualLimit = 120;
        TestSupport.assertEquals(helper, "actual fill", 120, restored.moveFluid(sink, 600, false));
        TestSupport.assertEquals(helper, "conserved", 3000,
                source.stack.getAmount() + restored.fluid().getAmount()
                        + restored.recovery().getAmount() + sink.stack.getAmount());
        TestSupport.assertEquals(helper, "recovery", 480, restored.recoverFluid());
        helper.succeed();
    }

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void machinePullsMatchingFluidBehindAnotherVariant(final GameTestHelper helper) {
        final MachineResources buffer = new MachineResources(() -> {});
        buffer.restore(new FluidStack(Fluids.WATER, 100), FluidStack.EMPTY, 0);
        final Tank source = new Tank(new FluidStack(Fluids.WATER, 2000));
        source.previewOtherFluid = true;
        TestSupport.assertEquals(helper, "typed extraction chooses matching variant", 500,
                buffer.moveFluid(source, 500, true));
        TestSupport.assertEquals(helper, "matching buffer", 600, buffer.fluid().getAmount());
        TestSupport.assertEquals(helper, "source remainder", 1500, source.stack.getAmount());
        helper.succeed();
    }

    /** Minimal handler deliberately accepting less on execute than on simulate. */
    private static final class Tank implements FluidHandler {
        private FluidStack stack;
        private int actualLimit = 8000;
        private boolean previewOtherFluid;
        private Tank(final FluidStack initial) { stack = initial; }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(final int tank) { return stack; }
        @Override public int getTankCapacity(final int tank) { return 8000; }
        @Override public boolean isFluidValid(final int tank, final FluidStack value) { return true; }
        @Override public int fill(final FluidStack value, final FluidAction action) {
            if (!stack.isEmpty() && !stack.sameResource(value)) return 0;
            final int accepted = Math.min(value.getAmount(), Math.min(8000 - stack.getAmount(),
                    action == FluidAction.EXECUTE ? actualLimit : 8000));
            if (action == FluidAction.EXECUTE) stack = value.withAmount(stack.getAmount() + accepted);
            return accepted;
        }
        @Override public FluidStack drain(final FluidStack value, final FluidAction action) {
            if (!stack.sameResource(value)) return FluidStack.EMPTY;
            return extract(value.getAmount(), action);
        }
        @Override public FluidStack drain(final int amount, final FluidAction action) {
            if (previewOtherFluid) return new FluidStack(Fluids.LAVA, amount);
            return extract(amount, action);
        }
        private FluidStack extract(final int amount, final FluidAction action) {
            final FluidStack taken = stack.withAmount(Math.min(amount, stack.getAmount()));
            if (action == FluidAction.EXECUTE) stack = stack.withAmount(stack.getAmount() - taken.getAmount());
            return taken;
        }
    }

    private MachineTransferTests() {}
}
