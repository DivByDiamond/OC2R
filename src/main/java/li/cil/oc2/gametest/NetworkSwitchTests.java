/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.network.switches.NetworkSwitchBlockEntity;
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

/**
 * The switch block as it ships today. It exposes no network interface capability and has no item, so
 * connectors cannot reach it and frames cannot be forwarded through it; only its block entity is testable.
 */
@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkSwitchTests {
    private static final BlockPos SWITCH = new BlockPos(5, TestSupport.WORK_Y, 5);

    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void loneSwitchTicksAndReportsNoLinksOrHosts(final GameTestHelper helper) {
        helper.setBlock(SWITCH, Blocks.NETWORK_SWITCH.get().defaultBlockState());
        final var device = TestSupport.blockEntity(helper, SWITCH, NetworkSwitchBlockEntity.class);
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            for (final boolean linked : device.getLinkState()) {
                TestSupport.assertTrue(helper, "a lone switch has no links", !linked);
            }
            TestSupport.assertTrue(helper, "a lone switch knows no hosts", device.getHostTable().isEmpty());
        }).thenSucceed();
    }

    private NetworkSwitchTests() {
    }
}
