package li.cil.oc2.fabric.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.fabric.PersistentDataHolder;
import li.cil.oc2.gametest.ComputerFixture;
import li.cil.oc2.gametest.TestSupport;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.gametest.v1.GameTest;
*///?} else {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
//?}
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Exercises the real block entity save/load entrypoints, including loading before a level is assigned. */
public final class FabricPersistenceTests
        //? if <26.1 {
        implements FabricGameTest
        //?}
{
    //? if >=26.1 {
    /*@GameTest(structure = "oc2r:empty", environment = "oc2r_gametest:oc2r_fabric_hooks", maxTicks = 100)
    *///?} else {
    @GameTest(template = EMPTY_STRUCTURE, batch = "oc2r_fabric_hooks")
    //?}
    public void detachedComputerRestoresCpuRegistersRamComponentsAndEnergy(final GameTestHelper helper) {
        final ComputerFixture fixture = ComputerFixture.place(helper);
        fixture.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_1.get()));
        fixture.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        final ComputerBlockEntity source = fixture.blockEntity();
        final long marker = 0x123456789ABCDEFL;
        source.virtualMachine.state.board.getCpu().getGeneralRegisters()[5] = marker;
        source.energy.receiveEnergy(1234, false);
        // Capture the VM through the production save path without starting a worker thread.
        source.virtualMachine.setRunState(VMRunState.LOADING_DEVICES);
        final CompoundTag saved = source.saveWithFullMetadata(helper.getLevel().registryAccess());
        source.virtualMachine.setRunState(VMRunState.STOPPED);

        final BlockEntity restored = BlockEntity.loadStatic(source.getBlockPos(), source.getBlockState(),
                saved, helper.getLevel().registryAccess());
        if (!(restored instanceof final ComputerBlockEntity target)) {
            throw TestSupport.failure(helper, "saved computer did not load as its registered block entity type");
        }
        try {
            TestSupport.assertTrue(helper, "loadStatic must exercise detached loading", target.getLevel() == null);
            TestSupport.assertEquals(helper, "CPU register must survive block save/load", marker,
                    target.virtualMachine.state.board.getCpu().getGeneralRegisters()[5]);
            final var cpu = target.deviceItems.getItemHandler(DeviceTypes.CPU).orElseThrow();
            final var memory = target.deviceItems.getItemHandler(DeviceTypes.MEMORY).orElseThrow();
            TestSupport.assertTrue(helper, "CPU component lost during detached load",
                    cpu.getStackInSlot(0).is(Items.CPU_TIER_1.get()));
            TestSupport.assertTrue(helper, "RAM component lost during detached load",
                    memory.getStackInSlot(0).is(Items.MEMORY_EXTRA_LARGE.get()));
            TestSupport.assertEquals(helper, "stored energy must survive block save/load",
                    source.energy.getEnergyStored(), target.energy.getEnergyStored());
        } finally {
            target.virtualMachine.dispose();
        }
        helper.succeed();
    }
    //? if >=26.1 {
    /*@GameTest(structure = "oc2r:empty", environment = "oc2r_gametest:oc2r_fabric_hooks", maxTicks = 100)
    *///?} else {
    @GameTest(template = EMPTY_STRUCTURE, batch = "oc2r_fabric_hooks")
    //?}
    public void persistentEntityDataSurvivesVanillaSaveLoad(final GameTestHelper helper) {
        final ArmorStand source = new ArmorStand(armorStandType(), helper.getLevel());
        final CompoundTag data = ((PersistentDataHolder) source).oc2r$getPersistentData();
        final CompoundTag nested = new CompoundTag();
        nested.putString("label", "persistent entity / робот");
        nested.putLongArray("addresses", new long[] {0, Long.MAX_VALUE, Long.MIN_VALUE});
        data.put("device", nested);
        //? if >=26.1 {
        /*final var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        source.saveWithoutId(output);
        final CompoundTag saved = output.buildResult();
        *///?} else {
        final CompoundTag saved = source.saveWithoutId(new CompoundTag());
        //?}
        final ArmorStand target = new ArmorStand(armorStandType(), helper.getLevel());
        //? if >=26.1 {
        /*target.load(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        *///?} else {
        target.load(saved);
        //?}
        final CompoundTag restored = ((PersistentDataHolder) target).oc2r$getPersistentData();
        TestSupport.assertTrue(helper, "persistent entity NBT differs after vanilla save/load", data.equals(restored));
        TestSupport.assertTrue(helper, "entity NBT must not share mutable state", data != restored);
        helper.succeed();
    }

    private static EntityType<ArmorStand> armorStandType() {
        //? if >=26.2 {
        /*return net.minecraft.world.entity.EntityTypes.ARMOR_STAND;
        *///?} else {
        return EntityType.ARMOR_STAND;
        //?}
    }

}
