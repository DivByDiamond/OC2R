package li.cil.oc2.client.gui.screen.tablet;

import li.cil.oc2.client.gui.screen.common.machine.AbstractMachineTerminalScreen;
import li.cil.oc2.common.container.tablet.TabletTerminalContainer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
//? if <26.1 {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}

/** Terminal and keyboard capture of a tablet; Right Ctrl (the shared binding) releases the keyboard. */
@OnlyIn(Dist.CLIENT)
public final class TabletTerminalScreen
        extends AbstractMachineTerminalScreen<TabletTerminalContainer> {
    public TabletTerminalScreen(
            final TabletTerminalContainer container,
            final Inventory inventory,
            final Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        // The shared screen only needs the box to exist; this screen has nothing to keep.
    }
}
