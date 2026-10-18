/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import static li.cil.oc2.gametest.TestSupport.assertNotNull;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
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

@GameTestHolder(API.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistrationTests {
    @GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void deviceTypesAreRegistered(final GameTestHelper helper) {
        assertNotNull(helper, DeviceTypes.CPU, "DeviceTypes.CPU");
        assertNotNull(helper, DeviceTypes.MEMORY, "DeviceTypes.MEMORY");
        assertNotNull(helper, DeviceTypes.HARD_DRIVE, "DeviceTypes.HARD_DRIVE");
        assertNotNull(helper, DeviceTypes.FLASH_MEMORY, "DeviceTypes.FLASH_MEMORY");
        assertNotNull(helper, DeviceTypes.CARD, "DeviceTypes.CARD");
        assertNotNull(helper, DeviceTypes.ROBOT_MODULE, "DeviceTypes.ROBOT_MODULE");
        assertNotNull(helper, DeviceTypes.FLOPPY, "DeviceTypes.FLOPPY");
        assertNotNull(helper, DeviceTypes.NETWORK_TUNNEL, "DeviceTypes.NETWORK_TUNNEL");
        helper.succeed();
    }

    //? if >=26.1 {
    /*@GameTest(template = TestSupport.TEMPLATE, templateNamespace = TestSupport.TEMPLATE_NAMESPACE)
    public static void serverPlayerReceivesModPayloads(final GameTestHelper helper) {
        final net.minecraft.server.level.ServerPlayer player = TestSupport.serverPlayer(helper);
        final li.cil.oc2.common.network.message.computer.ComputerBusStateMessage message =
                new li.cil.oc2.common.network.message.computer.ComputerBusStateMessage(
                        helper.absolutePos(TestSupport.COMPUTER_POS),
                        li.cil.oc2.common.bus.controller.BusState.READY, 0);
        TestSupport.assertTrue(helper, "computer payload channel must be negotiated",
                player.connection.hasChannel(message.type().id()));
        li.cil.oc2.platform.Platform.network().sendToPlayer(message, player);
        helper.succeed();
    }
    *///?}

    // --------------------------------------------------------------------- //

    private RegistrationTests() {
    }
}
