package li.cil.oc2.client.hooks;

import li.cil.oc2.client.RenderCompat;
import li.cil.oc2.client.gui.screen.monitor.BusInterfaceScreen;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BusInterfaceScreenHooks {
    private BusInterfaceScreenHooks() {}

    public static void openBusInterfaceScreen(final BusCableBlockEntity blockEntity, final Direction side) {
        RenderCompat.setScreen(new BusInterfaceScreen(blockEntity, side));
    }
}