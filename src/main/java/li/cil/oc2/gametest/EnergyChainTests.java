/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.*;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Section 34 in-game acceptance for the cable energy network: a chain of cables must feed a
 * consumer at its far end, and the per-cable buffers must survive a save/reload cycle (the
 * buffers are documented as "saved with that block entity like any other stateful block",
 * see docs/CABLE-SYSTEM.md, "Energy").
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyChainTests {
    /** Four chained cables; the checklist asks for a chain of three or more. */
    private static final BlockPos MIDDLE_CABLE_POS = new BlockPos(4, WORK_Y, 2);
    /** The consumer at the far end, fed only through the cable chain. */
    private static final BlockPos CONSUMER_POS = new BlockPos(6, WORK_Y, 2);

    /**
     * The default {@code computerEnergyStorage} is 8000; "nearly full" leaves slack for tick
     * timing without waiting for the very last FE.
     */
    private static final long CONSUMER_NEARLY_FULL = 6000;

    /** Energy reaches the last cable and keeps flowing to the far-end consumer. */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 300)
    public static void energyChainFeedsTheLastConsumer(final GameTestHelper helper) {
        final ComputerFixture consumer = placeChain(helper);

        final long[] previous = new long[1];
        helper.startSequence()
            // Give the network a redistribution pass before sampling the trend.
            .thenExecuteAfter(40, () -> previous[0] = consumer.energy())
            .thenExecuteAfter(60, () -> {
                // The last cable is a pass-through: its redistributed share is pushed to the
                // consumer in the same pass, so its buffer is near zero most of the time. The
                // consumer being fed at all is the proof that energy reached the far end.
                assertTrue(helper, "energy must keep flowing to the far-end consumer",
                    consumer.energy() > previous[0]);
            })
            .thenWaitUntil(() -> assertTrue(helper, "the consumer must fill up through the chain",
                consumer.energy() >= CONSUMER_NEARLY_FULL))
            .thenSucceed();
    }

    /**
     * The middle cable's buffer holds energy, is written to its saved NBT and comes back on
     * the load a chunk reload performs; the reloaded network keeps feeding the consumer.
     */
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE,
        timeoutTicks = 400)
    public static void cableEnergySurvivesSaveAndReload(final GameTestHelper helper) {
        final ComputerFixture consumer = placeChain(helper);
        final BusCableBlockEntity middle = blockEntity(helper, MIDDLE_CABLE_POS, BusCableBlockEntity.class);

        final CompoundTag[] saved = new CompoundTag[1];
        final long[] storedBefore = new long[1];
        final long[] consumedBefore = new long[1];
        helper.startSequence()
            .thenWaitUntil(() -> assertTrue(helper, "the middle cable must hold some energy first",
                middle.energy.getEnergyStored() > 0))
            .thenExecute(() -> {
                storedBefore[0] = middle.energy.getEnergyStored();
                consumedBefore[0] = consumer.energy();
                saved[0] = middle.saveWithFullMetadata(helper.getLevel().registryAccess());
            })
            // Drive the lifecycle seam synchronously, the way a real chunk unload does;
            // neither call may turn the unload into destruction (see NetworkConnectorTests).
            .thenExecute(() -> {
                middle.onChunkUnloaded();
                middle.setRemoved();
            })
            // Recreate the block entity the way chunk loading does: build it from the saved
            // NBT, then hand it to the level; adding it to the chunk triggers onLoad/loadServer.
            .thenExecute(() -> {
                final BlockState state = helper.getBlockState(MIDDLE_CABLE_POS);
                final BusCableBlockEntity reloaded =
                    new BusCableBlockEntity(helper.absolutePos(MIDDLE_CABLE_POS), state);
                reloaded.setLevel(helper.getLevel());
                loadCable(reloaded, saved[0], helper);
                helper.getLevel().setBlockEntity(reloaded);
            })
            .thenWaitUntil(() -> {
                final BusCableBlockEntity reloaded =
                    blockEntity(helper, MIDDLE_CABLE_POS, BusCableBlockEntity.class);
                assertTrue(helper, "the middle cable must be the reloaded instance",
                    reloaded != middle && !reloaded.isRemoved());
                assertTrue(helper, "the reloaded cable must keep its stored energy: "
                    + storedBefore[0] + " before, " + reloaded.energy.getEnergyStored() + " after",
                    reloaded.energy.getEnergyStored() >= storedBefore[0]);
            })
            .thenWaitUntil(() -> assertTrue(helper, "the reloaded network must keep feeding the consumer",
                consumer.energy() > consumedBefore[0]))
            .thenSucceed();
    }

    private static ComputerFixture placeChain(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        placePower(helper, player);
        for (int x = 2; x <= 5; x++) {
            place(helper, player, new ItemStack(Items.BUS_CABLE.get()), new BlockPos(x, WORK_Y, 2));
        }
        return ComputerFixture.place(helper, player, CONSUMER_POS);
    }

    private static void loadCable(final BusCableBlockEntity cable, final CompoundTag tag,
                                 final GameTestHelper helper) {
        //? if >=26.1 {
        /*cable.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
            net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        *///?} else {
        cable.loadWithComponents(tag, helper.getLevel().registryAccess());
        //?}
    }

    private EnergyChainTests() {
    }
}
