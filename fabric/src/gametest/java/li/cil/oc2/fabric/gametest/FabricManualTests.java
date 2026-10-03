package li.cil.oc2.fabric.gametest;

import li.cil.manual.api.prefab.item.AbstractManualItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.Platform;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** The in-game manual item is the Markdown Manual library's on Fabric, and late registrations work. */
public final class FabricManualTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void manualItemUsesTheLibrary(final GameTestHelper helper) {
        if (!(Items.MANUAL.get() instanceof AbstractManualItem)) {
            helper.fail("the manual item is " + Items.MANUAL.get().getClass());
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
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
