package li.cil.oc2.fabric.gametest;

import li.cil.oc2.manual.api.prefab.item.AbstractManualItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.Platform;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.gametest.v1.GameTest;
*///?} else {
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
//?}
import net.minecraft.gametest.framework.GameTestHelper;

/** The in-game manual item is the Markdown Manual library's on Fabric, and late registrations work. */
public final class FabricManualTests
        //? if <26.1 {
        implements FabricGameTest
        //?}
{
    //? if >=26.1 {
    /*@GameTest(structure = "oc2r:empty", environment = "oc2r_gametest:oc2r_fabric_manual", maxTicks = 100)
    *///?} else {
    @GameTest(template = EMPTY_STRUCTURE, batch = "oc2r_fabric_manual")
    //?}
    public void manualItemUsesTheLibrary(final GameTestHelper helper) {
        if (!(Items.MANUAL.get() instanceof AbstractManualItem)) {
            helper.fail("the manual item is " + Items.MANUAL.get().getClass());
        }
        helper.succeed();
    }

    //? if >=26.1 {
    /*@GameTest(structure = "oc2r:empty", environment = "oc2r_gametest:oc2r_fabric_manual", maxTicks = 100)
    *///?} else {
    @GameTest(template = EMPTY_STRUCTURE, batch = "oc2r_fabric_manual")
    //?}
    public void vanillaRegistriesRejectLateRegistration(final GameTestHelper helper) {
        try {
            Platform.registries().register("minecraft:item", "oc2r_gametest", "late", () -> net.minecraft.world.item.Items.STICK);
        } catch (final IllegalStateException expected) {
            helper.succeed();
            return;
        }
        helper.fail("registering into a frozen vanilla registry after bind must be refused, not applied");
    }
}
