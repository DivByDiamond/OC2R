/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.misc.PciCardCageBlock;
import li.cil.oc2.common.blockentity.misc.PciCardCageBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.PciCardCageDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A card cage on a running computer draws energy only while it is mounted and reports when it runs dry. */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PciCardCageTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE, timeoutTicks = 600)
    public static void mountedCardCageUsesEnergyAndNoticesWhenItIsGone(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        connectInterface(helper, CABLE_POS, Direction.WEST);
        // The device socket is the back of the cage; it faces the cable.
        helper.setBlock(DEVICE_POS, Blocks.PCI_CARD_CAGE.get().defaultBlockState()
                .setValue(PciCardCageBlock.FACING, Direction.EAST));
        connectInterface(helper, CABLE_POS, Direction.EAST);
        helper.setBlock(DEVICE_POS.above(), Blocks.CREATIVE_ENERGY.get().defaultBlockState());
        final PciCardCageBlockEntity cage = blockEntity(helper, DEVICE_POS, PciCardCageBlockEntity.class);
        computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_ONYXOS.get()));
        computer.install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_EXTRA_LARGE.get()));
        computer.install(DeviceTypes.CPU, new ItemStack(Items.CPU_TIER_INF.get()));

        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                assertTrue(helper, "the cage is on the bus", computer.devices().stream()
                    .anyMatch(PciCardCageDevice.class::isInstance));
                assertTrue(helper, "a cage on a stopped computer draws nothing", !cage.hasEnergy());
                computer.start();
            })
            .thenWaitUntil(() -> {
                computer.assertRunState(VMRunState.RUNNING, "guest with a card cage");
                assertTrue(helper, "the mounted cage is powered", cage.hasEnergy());
            })
            .thenExecute(() -> {
                computer.assertNoBootError();
                helper.setBlock(DEVICE_POS.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            })
            .thenWaitUntil(() -> assertTrue(helper, "the cage runs dry without a supply", !cage.hasEnergy()))
            .thenExecute(() -> helper.setBlock(DEVICE_POS.above(), Blocks.CREATIVE_ENERGY.get().defaultBlockState()))
            .thenWaitUntil(() -> assertTrue(helper, "the cage recovers when the supply returns", cage.hasEnergy()))
            .thenSucceed();
    }

    private PciCardCageTests() {
    }
}
